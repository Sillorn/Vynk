import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { RegisterPage } from './register-page';

interface Internals {
  form: {
    setValue(v: Record<string, string>): void;
    controls: { confirmPassword: { hasError(e: string): boolean } };
  };
  error(): string | null;
  submitting(): boolean;
  onSubmit(): void;
}

describe('RegisterPage', () => {
  const auth = { register: vi.fn() };
  const valid = {
    username: 'ada',
    password: 'correct horse battery',
    confirmPassword: 'correct horse battery',
    firstName: 'Ada',
    lastName: 'Lovelace',
    headline: 'Engineer',
    summary: 'Hello',
  };

  function create() {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    return { page: fixture.componentInstance as unknown as Internals, navigate };
  }

  beforeEach(() => vi.resetAllMocks());

  it('does not submit an invalid form', () => {
    const { page } = create();
    page.onSubmit();
    expect(auth.register).not.toHaveBeenCalled();
  });

  it('flags mismatching passwords without calling the API', () => {
    const { page } = create();
    page.form.setValue({ ...valid, confirmPassword: 'different password!' });
    page.onSubmit();
    expect(page.form.controls.confirmPassword.hasError('mismatch')).toBe(true);
    expect(auth.register).not.toHaveBeenCalled();
  });

  it('registers without the confirmation field and goes to the feed', () => {
    auth.register.mockReturnValue(of({ username: 'ada' }));
    const { page, navigate } = create();
    page.form.setValue(valid);
    page.onSubmit();
    const sent = auth.register.mock.calls[0][0];
    expect(sent.confirmPassword).toBeUndefined();
    expect(sent.username).toBe('ada');
    expect(navigate).toHaveBeenCalledWith('/feed');
  });

  it('shows the server detail on failure', () => {
    auth.register.mockReturnValue(throwError(() => ({ error: { detail: 'Username taken' } })));
    const { page } = create();
    page.form.setValue(valid);
    page.onSubmit();
    expect(page.error()).toBe('Username taken');
    expect(page.submitting()).toBe(false);
  });
});
