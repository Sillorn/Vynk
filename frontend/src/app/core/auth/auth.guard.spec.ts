import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  const user = { value: null as { username: string } | null };
  let loading: ReturnType<typeof import('@angular/core').signal<boolean>>;
  let userSignal: ReturnType<typeof import('@angular/core').signal<{ username: string } | null>>;

  beforeEach(async () => {
    const { signal } = await import('@angular/core');
    loading = signal(false);
    userSignal = signal(user.value);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { loading, user: userSignal } },
      ],
    });
  });

  const run = (url: string) =>
    firstValueFrom(
      TestBed.runInInjectionContext(() => authGuard({} as never, { url } as never)) as Observable<
        boolean | UrlTree
      >,
    );

  it('allows authenticated users', async () => {
    userSignal.set({ username: 'ada' });
    expect(await run('/network')).toBe(true);
  });

  it('redirects anonymous users to login with a returnUrl', async () => {
    const result = (await run('/messages?x=1')) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe(
      '/login?returnUrl=%2Fmessages%3Fx%3D1',
    );
  });

  it('waits for the session restore before deciding', async () => {
    loading.set(true);
    let settled = false;
    const pending = run('/network').then((value) => {
      settled = true;
      return value;
    });
    await Promise.resolve();
    expect(settled).toBe(false);
    userSignal.set({ username: 'ada' });
    loading.set(false);
    expect(await pending).toBe(true);
  });
});
