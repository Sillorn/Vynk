import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/auth/auth.service';
import { ConnectionRequest, ConnectionService } from '../../../core/social/connection.service';

@Component({
  selector: 'app-network-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, MatButtonModule],
  template: `
    <section class="page network-page">
      <header class="page-intro">
        <p class="eyebrow">Your community</p>
        <h1>Your network</h1>
        <p class="lede">
          Manage invitations and stay connected with people in your professional network.
        </p>
      </header>
      @if (!auth.user()) {
        <p class="surface-card" role="status">Sign in to manage your network.</p>
      } @else {
        <form class="surface-card invite" (ngSubmit)="send()">
          <label for="invite-user">Invite by username</label>
          <div>
            <input
              id="invite-user"
              name="username"
              [(ngModel)]="username"
              required
              maxlength="30"
              autocomplete="off"
            /><button mat-flat-button type="submit" [disabled]="busy() || !username.trim()">
              Send invitation
            </button>
          </div>
        </form>
        @if (message()) {
          <p role="status">{{ message() }}</p>
        }
        @if (error()) {
          <p class="error" role="alert">
            {{ error() }} <button mat-button (click)="load()">Try again</button>
          </p>
        }
        <section class="surface-card list">
          <h2>Received invitations</h2>
          @if (incoming().length) {
            @for (request of incoming(); track request.id) {
              <article>
                <div>
                  <a [href]="'/profile/' + request.username">{{ request.username }}</a
                  ><time>{{ request.createdAt | date: 'mediumDate' }}</time>
                </div>
                <button mat-flat-button (click)="accept(request)">Accept</button
                ><button mat-button (click)="decline(request)">Decline</button>
              </article>
            }
          } @else {
            <p class="muted">No pending invitations.</p>
          }
        </section>
        <section class="surface-card list">
          <h2>Sent invitations</h2>
          @if (outgoing().length) {
            @for (request of outgoing(); track request.id) {
              <article>
                <div>
                  <a [href]="'/profile/' + request.username">{{ request.username }}</a
                  ><time>{{ request.createdAt | date: 'mediumDate' }}</time>
                </div>
                <button mat-button (click)="cancel(request)">Cancel</button>
              </article>
            }
          } @else {
            <p class="muted">No outgoing invitations.</p>
          }
        </section>
      }
    </section>
  `,
  styles: [
    `
      .network-page {
        max-width: 760px;
        margin: 0 auto;
      }
      .invite,
      .list {
        padding: 1.25rem;
        margin: 1rem 0;
      }
      .invite {
        display: grid;
        gap: 0.6rem;
      }
      .invite div {
        display: flex;
        gap: 0.75rem;
      }
      input {
        flex: 1;
        min-width: 0;
        padding: 0.7rem;
        font: inherit;
        border: 1px solid #8b98a8;
        border-radius: 0.4rem;
      }
      .list h2 {
        margin-top: 0;
      }
      .list article {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.8rem 0;
        border-top: 1px solid #d8dee6;
      }
      .list article div {
        flex: 1;
        display: grid;
        gap: 0.25rem;
      }
      time,
      .muted {
        color: #5e6875;
        font-size: 0.9rem;
      }
      .error {
        color: #a52222;
      }
      @media (max-width: 540px) {
        .invite div {
          flex-direction: column;
        }
        .list article {
          flex-wrap: wrap;
        }
      }
    `,
  ],
})
export class NetworkPage {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(ConnectionService);
  protected readonly incoming = signal<readonly ConnectionRequest[]>([]);
  protected readonly outgoing = signal<readonly ConnectionRequest[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected username = '';
  constructor() {
    this.load();
  }
  protected load(): void {
    if (!this.auth.user()) return;
    this.error.set(null);
    let completed = 0;
    const done = () => {
      if (++completed === 2) this.busy.set(false);
    };
    this.busy.set(true);
    this.service.incoming().subscribe({
      next: (value) => {
        this.incoming.set(value);
        done();
      },
      error: (err) => {
        this.fail(err);
        done();
      },
    });
    this.service.outgoing().subscribe({
      next: (value) => {
        this.outgoing.set(value);
        done();
      },
      error: (err) => {
        this.fail(err);
        done();
      },
    });
  }
  protected send(): void {
    this.action(
      () => this.service.send(this.username.trim()),
      () => {
        this.username = '';
      },
    );
  }
  protected accept(r: ConnectionRequest): void {
    this.action(() => this.service.accept(r.id));
  }
  protected decline(r: ConnectionRequest): void {
    this.action(() => this.service.decline(r.id));
  }
  protected cancel(r: ConnectionRequest): void {
    this.action(() => this.service.cancel(r.id));
  }
  private action(request: () => import('rxjs').Observable<void>, after?: () => void): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);
    request().subscribe({
      next: () => {
        after?.();
        this.message.set('Invitation updated.');
        this.load();
      },
      error: (err) => {
        this.fail(err);
        this.busy.set(false);
      },
    });
  }
  private fail(err: HttpErrorResponse): void {
    this.error.set(
      err.status === 0
        ? 'Unable to reach the service. Try again.'
        : (err.error?.detail ?? 'Unable to update your network.'),
    );
  }
}
