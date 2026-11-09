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
    service.contacts.mockReturnValue(of(['bob']));
    service.incoming.mockReturnValue(of([request]));
    service.outgoing.mockReturnValue(of([]));
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
