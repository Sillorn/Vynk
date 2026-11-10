import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ConnectionService } from '../../../core/social/connection.service';
import { UnreadService } from '../../../core/social/unread.service';
import { NetworkPage } from './network-page';

interface Internals {
  username: string;
  tab: { set(v: string): void };
  contacts(): readonly string[];
  contactsHasNext(): boolean;
  loadMore(list: string): void;
  busy(): boolean;
  error(): string | null;
  send(): void;
  accept(r: unknown): void;
}

describe('NetworkPage', () => {
  const service = {
    contacts: vi.fn(),
    incoming: vi.fn(),
    outgoing: vi.fn(),
    send: vi.fn(),
    accept: vi.fn(),
    decline: vi.fn(),
    cancel: vi.fn(),
  };
  const unread = { refreshInvitations: vi.fn() };
  const request = { id: 7, username: 'bob', createdAt: '2026-01-01T00:00:00Z' };
  const pageOf = <T>(items: T[], page = 0, hasNext = false) => ({
    items,
    page,
    size: 20,
    totalElements: items.length,
    totalPages: hasNext ? page + 2 : page + 1,
    hasNext,
  });

  function create(signedIn = true) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user: signal(signedIn ? { username: 'ada' } : null) } },
        { provide: ConnectionService, useValue: service },
        { provide: UnreadService, useValue: unread },
      ],
    });
    const fixture = TestBed.createComponent(NetworkPage);
    fixture.detectChanges();
    return { fixture, page: fixture.componentInstance as unknown as Internals };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    service.contacts.mockReturnValue(of(pageOf(['bob'])));
    service.incoming.mockReturnValue(of(pageOf([request])));
    service.outgoing.mockReturnValue(of(pageOf([])));
  });

  it('appends the next page when loading more contacts', () => {
    service.contacts.mockReturnValue(of(pageOf(['bob'], 0, true)));
    const { page } = create();
    expect(page.contactsHasNext()).toBe(true);
    service.contacts.mockReturnValue(of(pageOf(['carol'], 1, false)));
    page.loadMore('contacts');
    expect(service.contacts).toHaveBeenLastCalledWith(1);
    expect(page.contacts()).toEqual(['bob', 'carol']);
    expect(page.contactsHasNext()).toBe(false);
    expect(page.busy()).toBe(false);
  });

  it('does not load anything for anonymous visitors', () => {
    create(false);
    expect(service.contacts).not.toHaveBeenCalled();
  });

  it('links contacts and invitations with routerLink (no full reload)', () => {
    const { fixture, page } = create();
    page.tab.set('invitations');
    fixture.detectChanges();
    const link = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>(
      'a[href="/profile/bob"]',
    );
    expect(link).toBeTruthy();
  });

  it('accepting refreshes the lists and the invitation badge', () => {
    service.accept.mockReturnValue(of(undefined));
    const { page } = create();
    service.incoming.mockClear();
    page.accept(request);
    expect(service.accept).toHaveBeenCalledWith(7);
    expect(service.incoming).toHaveBeenCalledTimes(1);
    expect(unread.refreshInvitations).toHaveBeenCalled();
    expect(page.busy()).toBe(false);
  });

  it('sends a trimmed invitation and clears the field', () => {
    service.send.mockReturnValue(of(undefined));
    const { page } = create();
    page.username = '  carol ';
    page.send();
    expect(service.send).toHaveBeenCalledWith('carol');
    expect(page.username).toBe('');
  });

  it('shows the server detail and unblocks on failure', () => {
    service.send.mockReturnValue(
      throwError(() => ({ status: 409, error: { detail: 'Already pending' } })),
    );
    const { page } = create();
    page.username = 'carol';
    page.send();
    expect(page.error()).toBe('Already pending');
    expect(page.busy()).toBe(false);
  });
});
