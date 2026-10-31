import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  catchError,
  distinctUntilChanged,
  map,
  mergeWith,
  of,
  Subject,
  switchMap,
  timeout,
} from 'rxjs';
import { PublicProfile } from '../../../core/profiles/profile.models';
import { ProfileService } from '../../../core/profiles/profile.service';
import { InitialsAvatar } from '../../../shared/initials-avatar/initials-avatar';
import { AuthService } from '../../../core/auth/auth.service';
import { ConnectionService } from '../../../core/social/connection.service';

const MONTH_YEAR = new Intl.DateTimeFormat('en', {
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

interface LoadResult {
  readonly profile: PublicProfile | null;
  readonly error: string | null;
}

/**
 * Read-only public profile page. Only the privacy-safe public DTO is rendered; private account data
 * never reaches this screen. Visitors are anonymous by default.
 */
@Component({
  selector: 'app-public-profile-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InitialsAvatar, MatButtonModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './public-profile-page.html',
  styleUrl: './public-profile-page.scss',
})
export class PublicProfilePage {
  private readonly route = inject(ActivatedRoute);
  private readonly profiles = inject(ProfileService);
  private readonly connections = inject(ConnectionService);
  protected readonly auth = inject(AuthService);
  private readonly title = inject(Title);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reloadRequest$ = new Subject<string>();

  protected readonly profile = signal<PublicProfile | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly connected = signal(false);
  protected readonly connectionBusy = signal(false);
  protected readonly invitationSent = signal(false);

  protected readonly hasSidebar = computed(() => {
    const profile = this.profile();
    return (
      !!profile &&
      ((profile.skills?.length ?? 0) > 0 ||
        (profile.languages?.length ?? 0) > 0 ||
        (profile.links?.length ?? 0) > 0)
    );
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('username') ?? ''),
        mergeWith(this.reloadRequest$),
        distinctUntilChanged(),
        switchMap((username) => {
          this.loading.set(true);
          this.error.set(null);
          return this.profiles.getPublicProfile(username).pipe(
            timeout({ first: 15_000 }),
            map((profile): LoadResult => ({ profile, error: null })),
            catchError((error: unknown) =>
              of<LoadResult>({
                profile: null,
                error:
                  error instanceof HttpErrorResponse
                    ? describeError(error)
                    : 'The profile request timed out. Check the server connection and try again.',
              }),
            ),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        this.profile.set(result.profile);
        this.connected.set(false);
        this.invitationSent.set(false);
        if (
          result.profile &&
          this.auth.user() &&
          this.auth.user()?.username.toLowerCase() !== result.profile.username.toLowerCase()
        ) {
          this.connections.isConnected(result.profile.username).subscribe({
            next: (connected) => {
              this.connected.set(connected);
              if (!connected) {
                this.connections.outgoing().subscribe({
                  next: (requests) =>
                    this.invitationSent.set(
                      requests.some(
                        (request) =>
                          request.username.toLowerCase() === result.profile?.username.toLowerCase(),
                      ),
                    ),
                });
              }
            },
          });
        }
        this.error.set(result.error);
        this.loading.set(false);
        this.title.setTitle(
          result.profile ? `${fullName(result.profile)} · Vynk` : 'Profile unavailable · Vynk',
        );
      });
  }

  protected connect(username: string): void {
    if (this.connectionBusy() || this.connected()) return;
    this.connectionBusy.set(true);
    this.connections.send(username).subscribe({
      next: () => {
        this.invitationSent.set(true);
        this.connectionBusy.set(false);
      },
      error: () => {
        this.connectionBusy.set(false);
      },
    });
  }

  protected reload(): void {
    const username = this.route.snapshot.paramMap.get('username') ?? '';
    this.reloadRequest$.next(username);
  }

  protected fullName(profile: PublicProfile): string {
    return fullName(profile);
  }

  protected cvUrl(profile: PublicProfile): string {
    return `/api/v1/profiles/${encodeURIComponent(profile.username)}/cv`;
  }

  protected period(start?: string | null, end?: string | null, current = false): string {
    return period(start, end, current);
  }

  protected certificationPeriod(issueDate?: string | null, expirationDate?: string | null): string {
    const parts: string[] = [];
    const issued = formatMonth(issueDate);
    const expires = formatMonth(expirationDate);
    if (issued) {
      parts.push(`Issued ${issued}`);
    }
    if (expires) {
      parts.push(`Expires ${expires}`);
    }
    return parts.join(' · ');
  }
}

function fullName(profile: PublicProfile): string {
  return `${profile.firstName} ${profile.lastName}`.trim() || profile.username;
}

function period(start?: string | null, end?: string | null, current = false): string {
  const from = formatMonth(start);
  const to = current ? 'Present' : formatMonth(end);
  if (from && to) {
    return `${from} – ${to}`;
  }
  return from || to;
}

function formatMonth(value?: string | null): string {
  if (!value) {
    return '';
  }
  const match = /^(\d{4})-(\d{2})/.exec(value);
  if (!match) {
    return value;
  }
  const year = Number(match[1]);
  const month = Number(match[2]) - 1;
  if (month < 0 || month > 11) {
    return value;
  }
  return MONTH_YEAR.format(new Date(Date.UTC(year, month, 1)));
}

function describeError(error: HttpErrorResponse): string {
  if (error.status === 404) {
    return 'No profile matches that username. Check the spelling or browse the people directory.';
  }
  if (error.status === 0) {
    return 'The server could not be reached. Check your connection and try again.';
  }
  const detail = error.error?.detail;
  if (typeof detail === 'string' && detail.trim().length > 0) {
    return detail;
  }
  return 'This profile could not be loaded right now. Please try again.';
}
