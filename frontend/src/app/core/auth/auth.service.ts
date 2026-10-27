import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { catchError, of, switchMap, tap } from 'rxjs';

export interface SessionUser {
  readonly username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = '/api/v1/auth';

  readonly user = signal<SessionUser | null>(null);
  readonly loading = signal(true);

  constructor() {
    this.restoreSession();
  }

  restoreSession() {
    this.http
      .get<SessionUser>(`${this.apiBase}/me`, { withCredentials: true })
      .pipe(
        catchError(() => of(null)),
        tap((user) => {
          this.user.set(user);
          this.loading.set(false);
        }),
      )
      .subscribe();
  }

  login(username: string, password: string) {
    return this.http
      .get<void>(`${this.apiBase}/csrf`, { withCredentials: true })
      .pipe(
        switchMap(() =>
          this.http.post<SessionUser>(
            `${this.apiBase}/login`,
            { username: username.toLowerCase(), password },
            { withCredentials: true },
          ),
        ),
        tap((user) => this.user.set(user)),
      );
  }

  register(payload: {
    username: string;
    password: string;
    firstName: string;
    lastName: string;
    headline: string;
    summary: string;
  }) {
    return this.http
      .get<void>(`${this.apiBase}/csrf`, { withCredentials: true })
      .pipe(
        switchMap(() =>
          this.http.post<SessionUser>(
            `${this.apiBase}/register`,
            {
              ...payload,
              username: payload.username.toLowerCase(),
            },
            { withCredentials: true },
          ),
        ),
        tap((user) => this.user.set(user)),
      );
  }

  logout() {
    return this.http
      .post(`${this.apiBase}/logout`, {}, { withCredentials: true })
      .pipe(tap(() => this.user.set(null)));
  }
}
