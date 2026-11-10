import { TestBed } from '@angular/core/testing';
import { Subject, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ProfileService } from '../../../core/profiles/profile.service';
import { ConnectionService } from '../../../core/social/connection.service';
import { EventStreamService } from '../../../core/social/event-stream.service';
import { MessagingService } from '../../../core/social/messaging.service';
import { UnreadService } from '../../../core/social/unread.service';
import { signal } from '@angular/core';
import { MessagesPage } from './messages-page';

const item = (id: number, name: string) => ({
  conversationId: id,
  participantId: id,
  participantUsername: name,
  latestMessage: null,
  unreadCount: 0,
});
const inboxPage = (items: unknown[], page = 0, hasNext = false) => ({
  items,
  page,
  size: 20,
  totalElements: items.length,
  totalPages: hasNext ? page + 2 : page + 1,
  hasNext,
});
const message = (id: number) => ({
  id,
  conversationId: 1,
  senderUsername: 'ada',
  body: 'hi',
  createdAt: '2026-01-01T00:00:00Z',
});

interface Internals {
  draft: string;
  inbox(): readonly { conversationId: number; participantUsername: string }[];
  selected(): { conversationId: number } | null;
  messages(): unknown[];
  open(i: unknown): void;
  send(): void;
  hideSelected(): void;
  startConversation(u: string): void;
  loadMoreInbox(): void;
}

describe('MessagesPage', () => {
  const messaging = {
    inbox: vi.fn(),
    history: vi.fn(),
    send: vi.fn(),
    hide: vi.fn(),
    markRead: vi.fn(),
  };

  function create(rows: unknown[] = [item(1, 'bob'), item(2, 'cy')]) {
    messaging.inbox.mockReturnValue(of(inboxPage(rows)));
    messaging.markRead.mockReturnValue(of(undefined));
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { user: signal({ username: 'ada' }) } },
        { provide: MessagingService, useValue: messaging },
        { provide: ConnectionService, useValue: { contacts: () => of([]) } },
        { provide: ProfileService, useValue: { getPublicProfile: () => of({}) } },
        { provide: EventStreamService, useValue: { connect: () => () => {} } },
        { provide: UnreadService, useValue: { refreshMessages: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(MessagesPage);
    fixture.detectChanges();
    return { fixture, page: fixture.componentInstance as unknown as Internals };
  }

  afterEach(() => vi.resetAllMocks());

  it('reuses the client request ID when retrying the same message', () => {
    const { page } = create();
    messaging.history.mockReturnValue(of({ items: [], hasNext: false }));
    messaging.send.mockReturnValueOnce(new Subject()).mockReturnValueOnce(new Subject());
    page.open(item(1, 'bob'));
    page.draft = 'hello';
    page.send();
    // A failed send leaves `sending` false; emulate by erroring the first request.
    const first = messaging.send.mock.results[0].value as Subject<unknown>;
    first.error({ status: 0 });
    page.send();
    const [, , id1] = messaging.send.mock.calls[0];
    const [, , id2] = messaging.send.mock.calls[1];
    expect(id1).toBe(id2);
  });

  it('generates a new ID once the message changes or succeeds', () => {
    const { page } = create();
    messaging.history.mockReturnValue(of({ items: [], hasNext: false }));
    messaging.send.mockReturnValue(of(message(1)));
    page.open(item(1, 'bob'));
    page.draft = 'one';
    page.send();
    page.draft = 'one';
    page.send();
    expect(messaging.send.mock.calls[0][2]).not.toBe(messaging.send.mock.calls[1][2]);
  });

  it('hides the selected conversation', () => {
    const { page, fixture } = create();
    messaging.history.mockReturnValue(of({ items: [], hasNext: false }));
    messaging.hide.mockReturnValue(of(undefined));
    const confirmSpy = vi.spyOn(globalThis, 'confirm').mockReturnValue(true);
    page.open(item(1, 'bob'));
    fixture.detectChanges();
    const button = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ).find((b) => b.textContent?.includes('Hide conversation'));
    expect(button).toBeTruthy();
    button!.click();
    expect(confirmSpy).toHaveBeenCalledWith(expect.stringContaining('until you send a message'));
    expect(messaging.hide).toHaveBeenCalledWith(1);
    expect(page.selected()).toBeNull();
    confirmSpy.mockRestore();
  });

  it('does not hide the conversation when the confirmation is cancelled', () => {
    const { page, fixture } = create();
    messaging.history.mockReturnValue(of({ items: [], hasNext: false }));
    messaging.hide.mockClear();
    const confirmSpy = vi.spyOn(globalThis, 'confirm').mockReturnValue(false);
    page.open(item(1, 'bob'));
    fixture.detectChanges();
    const button = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ).find((b) => b.textContent?.includes('Hide conversation'));
    button!.click();
    expect(messaging.hide).not.toHaveBeenCalled();
    expect(page.selected()).not.toBeNull();
    confirmSpy.mockRestore();
  });

  it('ignores a stale history response after switching conversations', () => {
    const { page } = create();
    const slow = new Subject<unknown>();
    messaging.history
      .mockReturnValueOnce(slow)
      .mockReturnValueOnce(of({ items: [message(20)], hasNext: false }));
    page.open(item(1, 'bob'));
    page.open(item(2, 'cy'));
    slow.next({ items: [message(10)], hasNext: false });
    expect(page.messages()).toEqual([message(20)]);
  });
  it('renders unread badges and a hidden conversation disappears from the inbox', () => {
    const { fixture, page } = create([{ ...item(1, 'bob'), unreadCount: 3 }, item(2, 'cy')]);
    fixture.detectChanges();
    const badge = (fixture.nativeElement as HTMLElement).querySelector('.badge');
    expect(badge?.textContent?.trim()).toBe('3');
    expect(badge?.getAttribute('aria-label')).toBe('3 unread messages');

    messaging.history.mockReturnValue(of({ items: [], hasNext: false }));
    messaging.hide.mockReturnValue(of(undefined));
    messaging.markRead.mockReturnValue(of(undefined));
    const confirmSpy = vi.spyOn(globalThis, 'confirm').mockReturnValue(true);
    page.open(item(1, 'bob'));
    fixture.detectChanges();
    const hide = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find(
      (b) => b.textContent?.includes('Hide conversation'),
    );
    hide!.click();
    fixture.detectChanges();
    const names = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.inbox-item .username'),
    ).map((el) => el.textContent?.trim());
    expect(names).toEqual(['@cy']);
    confirmSpy.mockRestore();
  });

  it('loads the next inbox page on demand and keeps the loaded rows', () => {
    messaging.inbox
      .mockReturnValueOnce(of(inboxPage([item(1, 'bob')], 0, true)))
      .mockReturnValueOnce(of(inboxPage([item(1, 'bob'), item(2, 'cy')], 1, false)));
    const { fixture, page } = create();
    expect(messaging.inbox).toHaveBeenLastCalledWith(0);
    const more = () =>
      Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Load more conversations'),
      );
    expect(more()).toBeTruthy();

    more()!.click();
    fixture.detectChanges();

    expect(messaging.inbox).toHaveBeenLastCalledWith(1);
    // The duplicate row that offset paging can return is not rendered twice.
    expect(page.inbox()).toHaveLength(2);
    expect(more()).toBeUndefined();
  });

  it('keeps the first page when a load-more response arrives after a reload', () => {
    const slow = new Subject<unknown>();
    messaging.inbox
      .mockReturnValueOnce(of(inboxPage([item(1, 'bob')], 0, true)))
      .mockReturnValueOnce(slow)
      .mockReturnValueOnce(of(inboxPage([item(3, 'dee')], 0, false)));
    const { page } = create();
    page.loadMoreInbox();
    (page as unknown as { reload(): void }).reload();
    slow.next(inboxPage([item(2, 'cy')], 1, false));
    expect(page.inbox().map((row) => row.participantUsername)).toEqual(['dee']);
  });

  it('finds a contact whose conversation is on a later page instead of opening a draft', () => {
    messaging.inbox
      .mockReturnValueOnce(of(inboxPage([item(1, 'bob')], 0, true)))
      .mockReturnValueOnce(of(inboxPage([item(7, 'zed')], 1, false)));
    messaging.history.mockReturnValue(of({ items: [], hasNext: false }));
    const { page } = create();
    page.startConversation('zed');
    expect(page.selected()?.conversationId).toBe(7);
    expect(messaging.inbox).toHaveBeenLastCalledWith(1);
  });

  it('opens a draft for a new contact once every page is loaded', () => {
    messaging.inbox.mockReturnValueOnce(of(inboxPage([item(1, 'bob')], 0, false)));
    const { page } = create();
    page.startConversation('newperson');
    expect(page.selected()).toEqual(expect.objectContaining({ conversationId: 0 }));
    expect(messaging.inbox).toHaveBeenCalledTimes(1);
  });
});
