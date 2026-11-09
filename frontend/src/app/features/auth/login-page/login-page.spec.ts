import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { LoginPage } from './login-page';

describe('LoginPage redirect after sign-in', () => {
  async function submit(returnUrl: string | null): Promise<string> {
    const login = vi.fn().mockReturnValue(of({ username: 'ada' }));
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => returnUrl } } },
        },
      ],
    }).compileComponents();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginPage);
    const page = fixture.componentInstance as unknown as {
      form: { setValue(v: { username: string; password: string }): void };
      onSubmit(): void;
    };
    page.form.setValue({ username: 'ada', password: 'secret' });
    page.onSubmit();
    return navigate.mock.calls[0][0] as string;
  }

  it('returns to the requested page', async () => {
    expect(await submit('/messages')).toBe('/messages');
  });

  it('defaults to the feed', async () => {
    expect(await submit(null)).toBe('/feed');
  });

  it.each(['https://evil.example', '//evil.example', 'javascript:alert(1)'])(
    'ignores unsafe returnUrl %s',
    async (url) => {
      expect(await submit(url)).toBe('/feed');
    },
  );
});
