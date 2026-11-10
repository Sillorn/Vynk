import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, WritableSignal, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { describeHttpError } from '../../../shared/http-error';
import { AuthService } from '../../../core/auth/auth.service';
import { ConnectionRequest, ConnectionService } from '../../../core/social/connection.service';
import { UnreadService } from '../../../core/social/unread.service';
import { PageResponse } from '../../../core/social/post.service';
import { Observable } from 'rxjs';

type PagedList = 'contacts' | 'incoming' | 'outgoing';

@Component({
  selector: 'app-network-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, MatButtonModule, RouterLink],
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
        <div class="tabs" role="tablist" aria-label="My network">
          <button
            mat-button
            role="tab"
            [attr.aria-selected]="tab() === 'contacts'"
            (click)="tab.set('contacts')"
          >
            Contacts
          </button>
          <button
            mat-button
            role="tab"
            [attr.aria-selected]="tab() === 'invitations'"
            (click)="tab.set('invitations')"
          >
            Invitations
          </button>
        </div>
        @if (tab() === 'contacts') {
          <section class="surface-card list">
            <h2>Contacts</h2>
            @if (contacts().length) {
              @for (username of contacts(); track username) {
                <a class="contact-card" [routerLink]="['/profile', username]">
                  <span class="contact-avatar" aria-hidden="true">{{ initials(username) }}</span>
                  <span class="contact-details">
                    <strong>{{ username }}</strong>
                    <span>&#64;{{ username }}</span>
                  </span>
                  <span class="contact-card__action" aria-hidden="true"
                    >View profile <span>→</span></span
                  >
                </a>
              }
            } @else {
              <p class="muted">
                You don't have any contact yet, <a routerLink="/people">connect with people now!</a>
              </p>
            }
            @if (contactsHasNext()) {
              <button mat-button type="button" (click)="loadMore('contacts')" [disabled]="busy()">
                Load more contacts
              </button>
            }
          </section>
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
                    <a [routerLink]="['/profile', request.username]">{{ request.username }}</a
                    ><time>{{ request.createdAt | date: 'mediumDate' }}</time>
                  </div>
                  <button mat-flat-button (click)="accept(request)">Accept</button
                  ><button mat-button (click)="decline(request)">Decline</button>
                </article>
              }
            } @else {
              <p class="muted">No pending invitations.</p>
            }
            @if (incomingHasNext()) {
              <button mat-button type="button" (click)="loadMore('incoming')" [disabled]="busy()">
                Load more invitations
              </button>
            }
          </section>
          <section class="surface-card list">
            <h2>Sent invitations</h2>
            @if (outgoing().length) {
              @for (request of outgoing(); track request.id) {
                <article>
                  <div>
                    <a [routerLink]="['/profile', request.username]">{{ request.username }}</a
                    ><time>{{ request.createdAt | date: 'mediumDate' }}</time>
                  </div>
                  <button mat-button (click)="cancel(request)">Cancel</button>
                </article>
              }
            } @else {
              <p class="muted">No outgoing invitations.</p>
            }
            @if (outgoingHasNext()) {
              <button mat-button type="button" (click)="loadMore('outgoing')" [disabled]="busy()">
                Load more invitations
              </button>
            }
          </section>
        }
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
      .contact-card {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1rem;
        border: 1px solid #d8dee6;
        border-radius: 0.75rem;
        color: inherit;
        text-decoration: none;
        transition:
          border-color 120ms ease,
          background-color 120ms ease,
          transform 120ms ease;
      }
      .contact-card + .contact-card {
        margin-top: 0.75rem;
      }
      .contact-card:hover {
        border-color: #8ea8c3;
        background-color: #f7f9fc;
        transform: translateY(-1px);
      }
      .contact-avatar {
        display: grid;
        place-items: center;
        flex: 0 0 3rem;
        width: 3rem;
        height: 3rem;
        border-radius: 50%;
        background: #e5eaf2;
        color: #263c58;
        font-weight: 700;
      }
      .contact-details {
        display: grid;
        gap: 0.2rem;
        min-width: 0;
      }
      .contact-details strong {
        overflow-wrap: anywhere;
      }
      .contact-details span,
      .contact-card__action {
        color: #5e6875;
        font-size: 0.9rem;
      }
      .contact-card__action {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        margin-left: auto;
        white-space: nowrap;
      }
      .contact-card__action span {
        font-size: 1.15rem;
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
        color: var(--mat-sys-error);
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
  private readonly unread = inject(UnreadService);
  protected readonly incoming = signal<readonly ConnectionRequest[]>([]);
  protected readonly contacts = signal<readonly string[]>([]);
  protected readonly contactsHasNext = signal(false);
  protected readonly incomingHasNext = signal(false);
  protected readonly outgoingHasNext = signal(false);
  protected readonly tab = signal<'contacts' | 'invitations'>('contacts');
  protected readonly outgoing = signal<readonly ConnectionRequest[]>([]);
  private readonly pages: Record<PagedList, number> = { contacts: 0, incoming: 0, outgoing: 0 };
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
      if (++completed === 3) this.busy.set(false);
    };
    this.busy.set(true);
    this.applyPage(
      'contacts',
      this.service.contacts(0),
      this.contacts,
      this.contactsHasNext,
      0,
      done,
    );
    this.applyPage(
      'incoming',
      this.service.incoming(0),
      this.incoming,
      this.incomingHasNext,
      0,
      done,
    );
    this.applyPage(
      'outgoing',
      this.service.outgoing(0),
      this.outgoing,
      this.outgoingHasNext,
      0,
      done,
    );
  }

  protected loadMore(list: PagedList): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    const page = this.pages[list] + 1;
    const done = () => this.busy.set(false);
    switch (list) {
      case 'contacts':
        this.applyPage(
          list,
          this.service.contacts(page),
          this.contacts,
          this.contactsHasNext,
          page,
          done,
        );
        break;
      case 'incoming':
        this.applyPage(
          list,
          this.service.incoming(page),
          this.incoming,
          this.incomingHasNext,
          page,
          done,
        );
        break;
      case 'outgoing':
        this.applyPage(
          list,
          this.service.outgoing(page),
          this.outgoing,
          this.outgoingHasNext,
          page,
          done,
        );
        break;
    }
  }
  protected initials(username: string): string {
    return username.trim().slice(0, 2).toUpperCase();
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
        this.unread.refreshInvitations();
      },
      error: (err) => {
        this.fail(err);
        this.busy.set(false);
      },
    });
  }
  /** Page 0 replaces the list; later pages append. */
  private applyPage<T>(
    list: PagedList,
    request: Observable<PageResponse<T>>,
    target: WritableSignal<readonly T[]>,
    hasNext: WritableSignal<boolean>,
    page: number,
    done: () => void,
  ): void {
    request.subscribe({
      next: (response) => {
        target.set(page > 0 ? [...target(), ...response.items] : response.items);
        hasNext.set(response.hasNext);
        this.pages[list] = response.page;
        done();
      },
      error: (err) => {
        this.fail(err);
        done();
      },
    });
  }

  private fail(err: HttpErrorResponse): void {
    this.error.set(describeHttpError(err, 'Unable to update your network.'));
  }
}
