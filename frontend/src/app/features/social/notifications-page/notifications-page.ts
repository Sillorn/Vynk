import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/auth/auth.service';
import { EventStreamService } from '../../../core/social/event-stream.service';
import { NotificationItem, NotificationService } from '../../../core/social/notification.service';

@Component({
  selector: 'app-notifications-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule],
  template: `
    <section class="page notifications-page">
      <header class="page-intro">
        <p class="eyebrow">Your activity</p>
        <h1>Notifications</h1>
        <p class="lede">Connection activity and other updates.</p>
      </header>
      @if (!auth.user()) {
        <p class="surface-card" role="status">Sign in to view your notifications.</p>
      } @else {
        @if (error()) {
          <p class="error" role="alert">
            {{ error() }} <button mat-button type="button" (click)="reload()">Try again</button>
          </p>
        }
        <section
          class="surface-card notification-list"
          aria-label="Notifications"
          aria-live="polite"
        >
          @if (loading()) {
            <p role="status">Loading notifications…</p>
          } @else if (!items().length) {
            <p class="muted">You’re all caught up. New activity will appear here.</p>
          } @else {
            @for (item of items(); track item.id) {
              <article class="notification" [class.unread]="!item.readAt">
                <div>
                  <p>{{ description(item) }}</p>
                  <time [attr.datetime]="item.createdAt">{{
                    item.createdAt | date: 'medium'
                  }}</time>
                </div>
                @if (!item.readAt) {
                  <button
                    mat-button
                    type="button"
                    (click)="markRead(item)"
                    [disabled]="pending().has(item.id)"
                  >
                    Mark as read
                  </button>
                } @else {
                  <span class="read-label">Read</span>
                }
              </article>
            }
            @if (hasNext()) {
              <button mat-button type="button" (click)="loadMore()" [disabled]="loadingMore()">
                {{ loadingMore() ? 'Loading…' : 'Load more' }}
              </button>
            }
          }
        </section>
      }
    </section>
  `,
  styles: [
    `
      .notifications-page {
        max-width: 850px;
        margin: 0 auto;
      }
      .notification-list {
        padding: 0.5rem 1rem;
      }
      .notification {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        padding: 1rem 0.25rem;
        border-bottom: 1px solid #d8dee6;
      }
      .notification:last-of-type {
        border-bottom: 0;
      }
      .notification p {
        margin: 0 0 0.3rem;
      }
      .notification time,
      .read-label,
      .muted {
        color: #596675;
        font-size: 0.9rem;
      }
      .notification.unread {
        border-left: 3px solid #244b74;
        padding-left: 0.75rem;
        background: #f5f8fb;
      }
      .error {
        color: #a52222;
      }
      @media (max-width: 560px) {
        .notification {
          align-items: flex-start;
          flex-direction: column;
        }
      }
    `,
  ],
})
export class NotificationsPage {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(NotificationService);
  private readonly events = inject(EventStreamService);
  protected readonly items = signal<readonly NotificationItem[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadingMore = signal(false);
  protected readonly hasNext = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly pending = signal<ReadonlySet<number>>(new Set());
  private page = 0;

  constructor() {
    effect((onCleanup) => {
      if (this.auth.user()) {
        this.reload();
        onCleanup(this.events.connect(() => this.reload()));
      } else {
        this.items.set([]);
      }
    });
  }

  protected description(item: NotificationItem): string {
    const actor = item.actorUsername ? `${item.actorUsername} ` : '';
    const labels: Record<string, string> = {
      CONNECTION_REQUEST: 'sent you a connection request.',
      CONNECTION_ACCEPTED: 'accepted your connection request.',
      MESSAGE: 'sent you a message.',
    };
    return actor + (labels[item.type] ?? 'there is a new update for you.');
  }

  protected reload(): void {
    this.page = 0;
    this.error.set(null);
    this.loading.set(true);
    this.service.list().subscribe({
      next: (response) => {
        this.items.set(response.items);
        this.hasNext.set(response.hasNext);
        this.loading.set(false);
      },
      error: (err) => {
        this.fail(err);
        this.loading.set(false);
      },
    });
  }

  protected loadMore(): void {
    if (this.loadingMore() || !this.hasNext()) return;
    const next = this.page + 1;
    this.loadingMore.set(true);
    this.service.list(next).subscribe({
      next: (response) => {
        this.page = next;
        this.items.update((items) => [...items, ...response.items]);
        this.hasNext.set(response.hasNext);
        this.loadingMore.set(false);
      },
      error: (err) => {
        this.fail(err);
        this.loadingMore.set(false);
      },
    });
  }

  protected markRead(item: NotificationItem): void {
    if (this.pending().has(item.id)) return;
    this.pending.update((ids) => new Set(ids).add(item.id));
    this.service.markRead(item.id).subscribe({
      next: () =>
        this.items.update((items) =>
          items.map((current) =>
            current.id === item.id ? { ...current, readAt: new Date().toISOString() } : current,
          ),
        ),
      error: (err) => this.fail(err),
      complete: () =>
        this.pending.update((ids) => {
          const next = new Set(ids);
          next.delete(item.id);
          return next;
        }),
    });
  }

  private fail(err: HttpErrorResponse): void {
    this.error.set(
      err.status === 0
        ? 'Unable to reach the service. Try again.'
        : (err.error?.detail ?? 'Unable to load notifications.'),
    );
  }
}
