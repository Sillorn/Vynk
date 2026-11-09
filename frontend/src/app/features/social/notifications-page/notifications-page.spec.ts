import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { EventStreamService } from '../../../core/social/event-stream.service';
import { NotificationService } from '../../../core/social/notification.service';
import { UnreadService } from '../../../core/social/unread.service';
import { NotificationsPage } from './notifications-page';

const n = (id: number, readAt: string | null = null) => ({
  id,
  type: 'CONNECTION_ACCEPTED',
  actorUsername: 'bob',
  createdAt: '2026-01-01T00:00:00Z',
  readAt,
});

describe('NotificationsPage', () => {
  const service = { list: vi.fn(), markRead: vi.fn() };
  const unread = { refreshNotifications: vi.fn() };
  const stream = { connect: vi.fn() };
  const user = signal<{ username: string } | null>({ username: 'ada' });

  function create() {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { user } },
        { provide: NotificationService, useValue: service },
        { provide: UnreadService, useValue: unread },
        { provide: EventStreamService, useValue: stream },
      ],
    });
    const fixture = TestBed.createComponent(NotificationsPage);
    fixture.detectChanges();
    return fixture;
  }
  const text = (f: ReturnType<typeof create>) => (f.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(() => {
    vi.resetAllMocks();
    user.set({ username: 'ada' });
    stream.connect.mockReturnValue(() => {});
  });

  it('asks anonymous visitors to sign in', () => {
    user.set(null);
    expect(text(create())).toContain('Sign in to view your notifications');
    expect(service.list).not.toHaveBeenCalled();
  });

  it('lists notifications and marks one as read', () => {
    service.list.mockReturnValue(of({ items: [n(1)], hasNext: false }));
    service.markRead.mockReturnValue(of(undefined));
    const fixture = create();
    expect(text(fixture)).toContain('bob accepted your connection request.');
    const button = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ).find((b) => b.textContent?.includes('Mark as read'));
    button!.click();
    fixture.detectChanges();
    expect(service.markRead).toHaveBeenCalledWith(1);
    expect(unread.refreshNotifications).toHaveBeenCalled();
    expect(text(fixture)).toContain('Read');
  });

  it('re-enables the button when marking as read fails', () => {
    service.list.mockReturnValue(of({ items: [n(1)], hasNext: false }));
    service.markRead.mockReturnValue(throwError(() => ({ status: 500 })));
    const fixture = create();
    const page = fixture.componentInstance as unknown as {
      pending(): Set<number>;
      markRead(i: unknown): void;
    };
    page.markRead(n(1));
    expect(page.pending().size).toBe(0);
  });

  it('reloads from REST on stream events and reconnects', () => {
    service.list.mockReturnValue(of({ items: [], hasNext: false }));
    create();
    const [onEvent, onReconnect] = stream.connect.mock.calls[0];
    service.list.mockClear();
    onEvent('notification');
    onReconnect();
    expect(service.list).toHaveBeenCalledTimes(2);
  });

  it('loads further pages', () => {
    service.list
      .mockReturnValueOnce(of({ items: [n(1)], hasNext: true }))
      .mockReturnValueOnce(of({ items: [n(2)], hasNext: false }));
    const fixture = create();
    const page = fixture.componentInstance as unknown as {
      loadMore(): void;
      items(): unknown[];
      hasNext(): boolean;
    };
    page.loadMore();
    expect(service.list).toHaveBeenLastCalledWith(1);
    expect(page.items()).toHaveLength(2);
    expect(page.hasNext()).toBe(false);
  });

  it('shows an error with the server detail', () => {
    const pending = new Subject<never>();
    service.list.mockReturnValue(pending);
    const fixture = create();
    pending.error({ status: 500, error: { detail: 'Boom' } });
    fixture.detectChanges();
    expect(text(fixture)).toContain('Boom');
  });
});
