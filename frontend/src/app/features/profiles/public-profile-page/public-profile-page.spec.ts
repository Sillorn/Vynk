import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ProfileService } from '../../../core/profiles/profile.service';
import { ConnectionService } from '../../../core/social/connection.service';
import { PublicProfilePage } from './public-profile-page';

interface Internals {
  loading(): boolean;
  error(): string | null;
  connected(): boolean;
  invitationSent(): boolean;
  connectionError(): string | null;
  connect(u: string): void;
}

describe('PublicProfilePage', () => {
  const profiles = { getPublicProfile: vi.fn() };
  const connections = { isConnected: vi.fn(), outgoing: vi.fn(), send: vi.fn() };
  const profile = { username: 'bob', firstName: 'Bob', lastName: 'Builder' };

  function create(viewer: string | null = 'ada') {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user: signal(viewer ? { username: viewer } : null) } },
        { provide: ProfileService, useValue: profiles },
        { provide: ConnectionService, useValue: connections },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: new BehaviorSubject({ get: () => 'bob' }),
            snapshot: { paramMap: { get: () => 'bob' } },
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(PublicProfilePage);
    fixture.detectChanges();
    return { fixture, page: fixture.componentInstance as unknown as Internals };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    profiles.getPublicProfile.mockReturnValue(of(profile));
    connections.isConnected.mockReturnValue(of(false));
    connections.outgoing.mockReturnValue(of([]));
  });

  it('loads the profile and checks the connection state for signed-in viewers', () => {
    const { page } = create();
    expect(page.loading()).toBe(false);
    expect(connections.isConnected).toHaveBeenCalledWith('bob');
    expect(page.connected()).toBe(false);
  });

  it('skips connection checks for anonymous visitors and for the owner', () => {
    create(null);
    expect(connections.isConnected).not.toHaveBeenCalled();
    TestBed.resetTestingModule();
    create('BOB');
    expect(connections.isConnected).not.toHaveBeenCalled();
  });

  it('marks a pending outgoing invitation', () => {
    connections.outgoing.mockReturnValue(of([{ username: 'Bob' }]));
    const { page } = create();
    expect(page.invitationSent()).toBe(true);
  });

  it('falls back to a generic message for non-HTTP failures', () => {
    profiles.getPublicProfile.mockReturnValue(throwError(() => new Error('x')));
    const { page } = create();
    expect(page.error()).toContain('timed out');
  });

  it('surfaces a failed connection-status lookup', () => {
    connections.isConnected.mockReturnValue(throwError(() => ({ status: 500 })));
    const { page } = create();
    expect(page.connectionError()).toBe('Your connection status could not be loaded.');
  });

  it('sends an invitation', () => {
    connections.send.mockReturnValue(of(undefined));
    const { page } = create();
    page.connect('bob');
    expect(page.invitationSent()).toBe(true);
  });

  it('reports a failed invitation with the server detail', () => {
    connections.send.mockReturnValue(
      throwError(() => ({ status: 409, error: { detail: 'Already connected' } })),
    );
    const { page } = create();
    page.connect('bob');
    expect(page.connectionError()).toBe('Already connected');
  });
});
