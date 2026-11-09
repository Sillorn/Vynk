import { BreakpointObserver } from '@angular/cdk/layout';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  Injector,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import type { MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from './core/auth/auth.service';
import { PwaUpdateService } from './core/pwa-update.service';
import { EventStreamService } from './core/social/event-stream.service';
import { UnreadService } from './core/social/unread.service';

interface NavItem {
  readonly label: string;
  readonly path: string;
  readonly icon: string;
}

const MOBILE_BREAKPOINT = '(max-width: 899px)';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  imports: [
    MatButtonModule,
    MatSidenavModule,
    MatToolbarModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
  ],
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly events = inject(EventStreamService);
  private readonly unread = inject(UnreadService);
  private readonly pwa = inject(PwaUpdateService);
  protected readonly auth = inject(AuthService);

  protected readonly isHandset = toSignal(
    this.breakpointObserver.observe(MOBILE_BREAKPOINT).pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly menuOpen = signal(false);
  protected readonly isOffline = signal(typeof navigator !== 'undefined' && !navigator.onLine);

  protected readonly navItems: readonly NavItem[] = [
    {
      label: 'Feed',
      path: '/feed',
      icon: 'M4 4h16v2H4V4zm0 5h16v2H4V9zm0 5h10v2H4v-2zm0 5h10v2H4v-2z',
    },
    {
      label: 'People',
      path: '/people',
      icon: 'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z',
    },
    {
      label: 'My network',
      path: '/network',
      icon: 'M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z',
    },
    {
      label: 'Messages',
      path: '/messages',
      icon: 'M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z',
    },
    {
      label: 'Notifications',
      path: '/notifications',
      icon: 'M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5S10.5 3.17 10.5 4v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z',
    },
  ];

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('offline', this.handleOffline);
      window.addEventListener('online', this.handleOnline);
      this.destroyRef.onDestroy(() => {
        window.removeEventListener('offline', this.handleOffline);
        window.removeEventListener('online', this.handleOnline);
      });
      if (this.isOffline()) this.showOfflineMessage();
    }

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.menuOpen.set(false);
        if (this.auth.user() && !this.isOffline()) this.unread.refreshAll();
      });

    // One shared SSE stream drives the navigation badges; events are hints, REST is the truth.
    effect((onCleanup) => {
      if (!this.auth.user()) {
        this.unread.clear();
        return;
      }
      this.unread.refreshAll();
      onCleanup(
        this.events.connect(
          (name) =>
            name === 'message' ? this.unread.refreshMessages() : this.unread.refreshNotifications(),
          () => this.unread.refreshAll(),
        ),
      );
    });

    this.pwa.init();
    effect(() => {
      if (!this.pwa.updateAvailable()) return;
      void this.notify('A new version of Vynk is available.', 'Reload', 0).then((ref) =>
        ref.onAction().subscribe(() => this.pwa.applyUpdate()),
      );
    });
  }

  protected hasIndicator(path: string): boolean {
    switch (path) {
      case '/network':
        return this.unread.hasIncomingInvitations();
      case '/messages':
        return this.unread.hasUnreadMessages();
      case '/notifications':
        return this.unread.hasUnreadNotifications();
      default:
        return false;
    }
  }

  protected indicatorLabel(path: string): string {
    return path === '/messages'
      ? 'Unread messages'
      : path === '/notifications'
        ? 'Unread notifications'
        : 'Pending invitations';
  }

  private readonly handleOffline = (): void => {
    this.isOffline.set(true);
    this.showOfflineMessage();
  };

  private readonly handleOnline = (): void => {
    this.isOffline.set(false);
    void this.notify('You are back online.', 'Dismiss', 4000);
  };

  private showOfflineMessage(): void {
    void this.notify(
      'You are offline. Changes and account actions need an internet connection.',
      'Dismiss',
      7000,
    );
  }

  /** Snackbar code is loaded on first use to keep it out of the initial bundle. */
  private async notify(
    message: string,
    action: string,
    duration: number,
  ): Promise<MatSnackBarRef<TextOnlySnackBar>> {
    const { MatSnackBar } = await import('@angular/material/snack-bar');
    return this.injector.get(MatSnackBar).open(message, action, { duration });
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.closeMenu();
        this.router.navigateByUrl('/login');
      },
      error: () => {
        this.closeMenu();
        this.router.navigateByUrl('/login');
      },
    });
  }
}
