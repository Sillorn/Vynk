import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { SettingsPage } from './settings-page';

interface Internals {
  form: { setValue(v: Record<string, string>): void };
  pwError(): string | null;
  pwSuccess(): boolean;
  delError(): string | null;
  changePassword(): void;
  deleteAccount(): void;
}

describe('SettingsPage', () => {
  const user = signal<{ username: string } | null>({ username: 'ada' });
  let http: HttpTestingController;
  let page: Internals;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    user.set({ username: 'ada' });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { user } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(SettingsPage);
    fixture.detectChanges();
    page = fixture.componentInstance as unknown as Internals;
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  it('rejects short new passwords client-side', () => {
    page.form.setValue({ currentPassword: 'old', newPassword: 'short' });
    page.changePassword();
    http.expectNone('/api/v1/auth/password');
  });

  it('changes the password', () => {
    page.form.setValue({ currentPassword: 'old password', newPassword: 'a long new password' });
    page.changePassword();
    const req = http.expectOne('/api/v1/auth/password');
    expect(req.request.body).toEqual({
      currentPassword: 'old password',
      newPassword: 'a long new password',
    });
    req.flush(null);
    expect(page.pwSuccess()).toBe(true);
  });

  it('shows the server detail when the change fails', () => {
    page.form.setValue({ currentPassword: 'old password', newPassword: 'a long new password' });
    page.changePassword();
    http
      .expectOne('/api/v1/auth/password')
      .flush({ detail: 'Wrong password' }, { status: 400, statusText: 'Bad Request' });
    expect(page.pwError()).toBe('Wrong password');
  });

  it('does nothing when account deletion is not confirmed', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    page.deleteAccount();
    http.expectNone('/api/v1/auth/account');
  });

  it('deletes the account, clears the session and goes to login', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    page.deleteAccount();
    http.expectOne('/api/v1/auth/account').flush(null);
    expect(user()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });

  it('reports a failed deletion', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    page.deleteAccount();
    http
      .expectOne('/api/v1/auth/account')
      .flush({ detail: 'Nope' }, { status: 500, statusText: 'Error' });
    expect(page.delError()).toBe('Nope');
  });
});
