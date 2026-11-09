import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ConnectionService } from './connection.service';
import { MessagingService } from './messaging.service';
import { NotificationService } from './notification.service';
import { UnreadService } from './unread.service';

describe('UnreadService', () => {
  const connections = { incoming: vi.fn() };
  const messaging = { inbox: vi.fn() };
  const notifications = { list: vi.fn() };
  let service: UnreadService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ConnectionService, useValue: connections },
        { provide: MessagingService, useValue: messaging },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    service = TestBed.inject(UnreadService);
  });

  it('flags pending invitations', () => {
    connections.incoming.mockReturnValue(of([{ id: 1 }]));
    service.refreshInvitations();
    expect(service.hasIncomingInvitations()).toBe(true);
    connections.incoming.mockReturnValue(of([]));
    service.refreshInvitations();
    expect(service.hasIncomingInvitations()).toBe(false);
  });

  it('flags unread messages', () => {
    messaging.inbox.mockReturnValue(of([{ unreadCount: 0 }, { unreadCount: 2 }]));
    service.refreshMessages();
    expect(service.hasUnreadMessages()).toBe(true);
    messaging.inbox.mockReturnValue(of([{ unreadCount: 0 }]));
    service.refreshMessages();
    expect(service.hasUnreadMessages()).toBe(false);
  });

  it('flags unread notifications, including when more pages exist', () => {
    notifications.list.mockReturnValue(of({ items: [{ readAt: null }], hasNext: false }));
    service.refreshNotifications();
    expect(service.hasUnreadNotifications()).toBe(true);
    notifications.list.mockReturnValue(of({ items: [{ readAt: 'x' }], hasNext: false }));
    service.refreshNotifications();
    expect(service.hasUnreadNotifications()).toBe(false);
    notifications.list.mockReturnValue(of({ items: [{ readAt: 'x' }], hasNext: true }));
    service.refreshNotifications();
    expect(service.hasUnreadNotifications()).toBe(true);
  });

  it('clears indicators on error and via clear()', () => {
    messaging.inbox.mockReturnValue(of([{ unreadCount: 1 }]));
    service.refreshMessages();
    messaging.inbox.mockReturnValue(throwError(() => new Error('x')));
    service.refreshMessages();
    expect(service.hasUnreadMessages()).toBe(false);

    connections.incoming.mockReturnValue(of([{ id: 1 }]));
    service.refreshInvitations();
    service.clear();
    expect(service.hasIncomingInvitations()).toBe(false);
  });

  it('refreshAll refreshes every indicator', () => {
    connections.incoming.mockReturnValue(of([{ id: 1 }]));
    messaging.inbox.mockReturnValue(of([{ unreadCount: 1 }]));
    notifications.list.mockReturnValue(of({ items: [{ readAt: null }], hasNext: false }));
    service.refreshAll();
    expect(service.hasIncomingInvitations()).toBe(true);
    expect(service.hasUnreadMessages()).toBe(true);
    expect(service.hasUnreadNotifications()).toBe(true);
  });
});
