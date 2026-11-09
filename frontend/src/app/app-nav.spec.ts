import { BreakpointObserver } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';
import { PwaUpdateService } from './core/pwa-update.service';
import { EventStreamService } from './core/social/event-stream.service';
import { UnreadService } from './core/social/unread.service';

describe('App navigation', () => {
  const user = signal<{ username: string } | null>(null);
  const unread = {
    hasIncomingInvitations: signal(false),
    hasUnreadMessages: signal(false),
    hasUnreadNotifications: signal(false),
    refreshAll: vi.fn(),
    refreshMessages: vi.fn(),
    refreshNotifications: vi.fn(),
    refreshInvitations: vi.fn(),
    clear: vi.fn(),
  };
  const stream = { connect: vi.fn() };

  function create(handset: boolean) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user, loading: signal(false), logout: vi.fn() } },
        { provide: UnreadService, useValue: unread },
        { provide: EventStreamService, useValue: stream },
        { provide: PwaUpdateService, useValue: { init: vi.fn(), updateAvailable: signal(false) } },
        {
          provide: BreakpointObserver,
          useValue: { observe: () => new BehaviorSubject({ matches: handset, breakpoints: {} }) },
        },
      ],
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    vi.clearAllMocks();
    user.set({ username: 'ada' });
    unread.hasIncomingInvitations.set(false);
    unread.hasUnreadMessages.set(false);
    unread.hasUnreadNotifications.set(false);
    stream.connect.mockReturnValue(() => {});
  });

  it('shows a Settings link in the desktop toolbar for signed-in users', () => {
    const el = create(false);
    const links = Array.from(el.querySelectorAll<HTMLAnchorElement>('.toolbar-actions a'));
    expect(links.some((a) => a.getAttribute('href') === '/settings')).toBe(true);
  });

  it('shows unread dots for each nav item on desktop', () => {
    unread.hasIncomingInvitations.set(true);
    unread.hasUnreadMessages.set(true);
    unread.hasUnreadNotifications.set(true);
    const el = create(false);
    const labels = Array.from(el.querySelectorAll('nav .invitation-indicator')).map((i) =>
      i.getAttribute('aria-label'),
    );
    expect(labels.sort()).toEqual([
      'Pending invitations',
      'Unread messages',
      'Unread notifications',
    ]);
  });

  it('renders the mobile drawer list with links and indicators', () => {
    unread.hasUnreadNotifications.set(true);
    const el = create(true);
    const items = Array.from(el.querySelectorAll<HTMLAnchorElement>('.mobile-nav__item'));
    expect(items.map((a) => a.getAttribute('href'))).toContain('/notifications');
    const notifications = items.find((a) => a.getAttribute('href') === '/notifications');
    expect(notifications?.querySelector('.invitation-indicator')).toBeTruthy();
    const messages = items.find((a) => a.getAttribute('href') === '/messages');
    expect(messages?.querySelector('.invitation-indicator')).toBeNull();
  });

  it('subscribes to the event stream only while signed in and refreshes on events', () => {
    create(false);
    expect(stream.connect).toHaveBeenCalledTimes(1);
    const [onEvent, onReconnect] = stream.connect.mock.calls[0];
    onEvent('message');
    expect(unread.refreshMessages).toHaveBeenCalled();
    onEvent('notification');
    expect(unread.refreshNotifications).toHaveBeenCalled();
    unread.refreshAll.mockClear();
    onReconnect();
    expect(unread.refreshAll).toHaveBeenCalled();
  });

  it('does not connect for anonymous users', () => {
    user.set(null);
    create(false);
    expect(stream.connect).not.toHaveBeenCalled();
    expect(unread.clear).toHaveBeenCalled();
  });
});
