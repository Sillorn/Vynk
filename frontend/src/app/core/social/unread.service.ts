import { Injectable, inject, signal } from '@angular/core';
import { ConnectionService } from './connection.service';
import { MessagingService } from './messaging.service';
import { NotificationService } from './notification.service';

/** App-wide indicators for navigation badges. Refreshed from REST; SSE events are only hints. */
@Injectable({ providedIn: 'root' })
export class UnreadService {
  private readonly connections = inject(ConnectionService);
  private readonly messaging = inject(MessagingService);
  private readonly notifications = inject(NotificationService);

  readonly hasIncomingInvitations = signal(false);
  readonly hasUnreadMessages = signal(false);
  readonly hasUnreadNotifications = signal(false);

  refreshAll(): void {
    this.refreshInvitations();
    this.refreshMessages();
    this.refreshNotifications();
  }

  refreshInvitations(): void {
    this.connections.incoming().subscribe({
      next: (requests) => this.hasIncomingInvitations.set(requests.length > 0),
      error: () => this.hasIncomingInvitations.set(false),
    });
  }

  refreshMessages(): void {
    this.messaging.inbox().subscribe({
      next: (items) => this.hasUnreadMessages.set(items.some((item) => item.unreadCount > 0)),
      error: () => this.hasUnreadMessages.set(false),
    });
  }

  refreshNotifications(): void {
    this.notifications.list(0, 30).subscribe({
      next: (page) =>
        this.hasUnreadNotifications.set(
          page.items.some((item) => item.readAt === null) || page.hasNext,
        ),
      error: () => this.hasUnreadNotifications.set(false),
    });
  }

  clear(): void {
    this.hasIncomingInvitations.set(false);
    this.hasUnreadMessages.set(false);
    this.hasUnreadNotifications.set(false);
  }
}
