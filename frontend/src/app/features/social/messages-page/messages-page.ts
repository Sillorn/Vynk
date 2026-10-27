import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/auth/auth.service';
import { EventStreamService } from '../../../core/social/event-stream.service';
import {
  ConversationMessage,
  InboxItem,
  MessagingService,
} from '../../../core/social/messaging.service';

@Component({
  selector: 'app-messages-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, MatButtonModule],
  template: `
    <section class="page messages-page">
      <header class="page-intro">
        <p class="eyebrow">Private conversations</p>
        <h1>Messages</h1>
        <p class="lede">Keep in touch with your accepted connections.</p>
      </header>
      @if (!auth.user()) {
        <p class="surface-card" role="status">Sign in to view your messages.</p>
      } @else {
        @if (error()) {
          <p class="error" role="alert">
            {{ error() }} <button mat-button type="button" (click)="reload()">Try again</button>
          </p>
        }
        <div class="messaging-layout">
          <aside class="surface-card inbox" aria-label="Conversations">
            <h2>Inbox</h2>
            @if (loadingInbox()) {
              <p role="status">Loading conversations…</p>
            } @else if (!inbox().length) {
              <p class="muted">
                No conversations yet. Start by connecting with someone in your network.
              </p>
            } @else {
              @for (item of inbox(); track item.conversationId) {
                <button
                  class="inbox-item"
                  type="button"
                  [class.selected]="selected()?.conversationId === item.conversationId"
                  [attr.aria-pressed]="selected()?.conversationId === item.conversationId"
                  (click)="open(item)"
                >
                  <span class="inbox-item__name"
                    >{{ item.participantUsername }}
                    @if (item.unreadCount) {
                      <span
                        class="badge"
                        [attr.aria-label]="item.unreadCount + ' unread messages'"
                        >{{ item.unreadCount }}</span
                      >
                    }
                  </span>
                  <span class="inbox-item__preview">{{
                    item.latestMessage?.body ?? 'No messages yet'
                  }}</span>
                  @if (item.latestMessage) {
                    <time>{{ item.latestMessage.createdAt | date: 'short' }}</time>
                  }
                </button>
              }
            }
          </aside>
          <section class="surface-card conversation" aria-label="Conversation">
            @if (!selected()) {
              <p class="muted">Choose a conversation to view messages.</p>
            } @else {
              <header class="conversation__header">
                <h2>{{ selected()?.participantUsername }}</h2>
                <button mat-button type="button" (click)="hideSelected()">Hide conversation</button>
              </header>
              @if (loadingMessages()) {
                <p role="status">Loading messages…</p>
              }
              @if (historyError()) {
                <div class="error" role="alert">
                  <p>{{ historyError() }}</p>
                  <button mat-button type="button" (click)="retryHistory()">Try again</button>
                </div>
              } @else if (!loadingMessages() && !messages().length) {
                <p class="muted">
                  No messages yet. Send the first message to start this conversation.
                </p>
              }
              @if (hasOlder()) {
                <button
                  mat-button
                  type="button"
                  (click)="loadOlder()"
                  [disabled]="loadingMessages()"
                >
                  Load older messages
                </button>
              }
              <ol class="message-list" aria-label="Messages" role="log" aria-live="polite">
                @for (message of messages(); track message.id) {
                  <li [class.own]="message.senderUsername === auth.user()?.username">
                    <span>{{
                      message.senderUsername === auth.user()?.username
                        ? 'You'
                        : message.senderUsername
                    }}</span>
                    <p>{{ message.body }}</p>
                    <time>{{ message.createdAt | date: 'short' }}</time>
                  </li>
                }
              </ol>
              <form class="composer" (ngSubmit)="send()">
                <label class="visually-hidden" for="message-body">Write a message</label
                ><textarea
                  id="message-body"
                  name="body"
                  [(ngModel)]="draft"
                  rows="2"
                  maxlength="5000"
                  required
                ></textarea
                ><button mat-flat-button type="submit" [disabled]="sending() || !draft.trim()">
                  Send
                </button>
              </form>
            }
          </section>
        </div>
      }
    </section>
  `,
  styles: [
    `
      .messages-page {
        max-width: 1100px;
        margin: 0 auto;
      }
      .messaging-layout {
        display: grid;
        grid-template-columns: minmax(15rem, 0.8fr) minmax(0, 1.6fr);
        gap: 1rem;
        align-items: start;
      }
      .inbox,
      .conversation {
        padding: 1rem;
        min-width: 0;
      }
      .inbox h2,
      .conversation h2 {
        margin: 0.25rem 0 1rem;
      }
      .inbox-item {
        display: grid;
        width: 100%;
        gap: 0.35rem;
        padding: 0.8rem;
        text-align: left;
        font: inherit;
        color: inherit;
        background: transparent;
        border: 0;
        border-top: 1px solid #d8dee6;
        cursor: pointer;
      }
      .inbox-item:hover,
      .inbox-item.selected {
        background: #eef3f8;
      }
      .inbox-item__name {
        font-weight: 650;
        display: flex;
        justify-content: space-between;
      }
      .inbox-item__preview {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        color: #596675;
      }
      .inbox-item time,
      .message-list time {
        font-size: 0.8rem;
        color: #596675;
      }
      .badge {
        display: inline-grid;
        place-items: center;
        min-width: 1.4rem;
        height: 1.4rem;
        border-radius: 99px;
        background: #244b74;
        color: white;
        font-size: 0.75rem;
      }
      .conversation {
        display: flex;
        flex-direction: column;
        min-height: 28rem;
      }
      .conversation__header {
        border-bottom: 1px solid #d8dee6;
      }
      .message-list {
        list-style: none;
        display: flex;
        flex: 1;
        flex-direction: column-reverse;
        gap: 0.75rem;
        padding: 1rem 0;
        margin: 0;
        max-height: 55vh;
        overflow: auto;
      }
      .message-list li {
        max-width: 82%;
        align-self: flex-start;
        border-radius: 0.8rem;
        padding: 0.65rem 0.85rem;
        background: #eef1f5;
      }
      .message-list li.own {
        align-self: flex-end;
        background: #e2edf8;
      }
      .message-list p {
        margin: 0.25rem 0;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
      }
      .message-list li > span {
        font-size: 0.8rem;
        font-weight: 650;
      }
      .message-list time {
        display: block;
        text-align: right;
      }
      .composer {
        display: flex;
        gap: 0.75rem;
        border-top: 1px solid #d8dee6;
        padding-top: 0.75rem;
      }
      .composer textarea {
        flex: 1;
        min-width: 0;
        resize: vertical;
        padding: 0.65rem;
        font: inherit;
        border: 1px solid #8b98a8;
        border-radius: 0.4rem;
      }
      .muted {
        color: #596675;
      }
      .error {
        color: #a52222;
      }
      .visually-hidden {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
      }
      @media (max-width: 700px) {
        .messaging-layout {
          grid-template-columns: 1fr;
        }
        .conversation {
          min-height: 22rem;
        }
        .message-list {
          max-height: 45vh;
        }
      }
    `,
  ],
})
export class MessagesPage {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(MessagingService);
  private readonly eventStream = inject(EventStreamService);
  protected readonly inbox = signal<readonly InboxItem[]>([]);
  protected readonly selected = signal<InboxItem | null>(null);
  protected readonly messages = signal<readonly ConversationMessage[]>([]);
  protected readonly loadingInbox = signal(false);
  protected readonly loadingMessages = signal(false);
  protected readonly sending = signal(false);
  protected readonly hasOlder = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly historyError = signal<string | null>(null);
  protected draft = '';
  private page = 0;

  constructor() {
    effect((onCleanup) => {
      if (this.auth.user()) {
        this.reload();
        const disconnect = this.eventStream.connect(() => {
          // The SSE payload is only a hint; reconcile current state from REST.
          this.reload();
          if (this.selected()) {
            this.page = 0;
            this.fetchMessages(false);
          }
        });
        onCleanup(disconnect);
      }
    });
  }

  protected reload(): void {
    this.error.set(null);
    this.loadingInbox.set(true);
    this.service.inbox().subscribe({
      next: (items) => {
        this.inbox.set(items);
        this.loadingInbox.set(false);
      },
      error: (err) => {
        this.fail(err);
        this.loadingInbox.set(false);
      },
    });
  }

  protected open(item: InboxItem): void {
    this.selected.set(item);
    this.messages.set([]);
    this.page = 0;
    this.hasOlder.set(false);
    this.fetchMessages(false);
  }

  protected loadOlder(): void {
    if (this.loadingMessages() || !this.hasOlder()) return;
    this.page++;
    this.fetchMessages(true);
  }

  protected retryHistory(): void {
    this.fetchMessages(this.page > 0);
  }

  protected hideSelected(): void {
    const item = this.selected();
    if (!item) return;
    this.service.hide(item.conversationId).subscribe({
      next: () => {
        this.inbox.update((rows) =>
          rows.filter((row) => row.conversationId !== item.conversationId),
        );
        this.selected.set(null);
        this.messages.set([]);
      },
      error: (err) => this.fail(err),
    });
  }

  protected send(): void {
    const item = this.selected();
    const body = this.draft.trim();
    if (!item || !body || this.sending()) return;
    this.sending.set(true);
    this.error.set(null);
    this.service.send(item.participantUsername, body).subscribe({
      next: (message) => {
        this.messages.update((current) => [message, ...current]);
        this.draft = '';
        this.sending.set(false);
        this.reload();
      },
      error: (err) => {
        this.fail(err);
        this.sending.set(false);
      },
    });
  }

  private fetchMessages(older: boolean): void {
    const item = this.selected();
    if (!item) return;
    this.loadingMessages.set(true);
    this.historyError.set(null);
    this.service.history(item.conversationId, this.page).subscribe({
      next: (response) => {
        this.messages.update((current) => {
          const combined = older
            ? [...current, ...response.items]
            : [...response.items, ...current];
          return [...new Map(combined.map((message) => [message.id, message])).values()].sort(
            (left, right) => right.id - left.id,
          );
        });
        this.hasOlder.set(response.hasNext);
        this.loadingMessages.set(false);
        this.service.markRead(item.conversationId).subscribe({
          next: () =>
            this.inbox.update((rows) =>
              rows.map((row) =>
                row.conversationId === item.conversationId ? { ...row, unreadCount: 0 } : row,
              ),
            ),
        });
      },
      error: (err) => {
        this.historyError.set(
          err.status === 0
            ? 'Unable to reach the service. Try again.'
            : (err.error?.detail ?? 'Unable to load messages.'),
        );
        this.loadingMessages.set(false);
      },
    });
  }

  private fail(err: HttpErrorResponse): void {
    this.error.set(
      err.status === 0
        ? 'Unable to reach the service. Try again.'
        : (err.error?.detail ?? 'Unable to load messages.'),
    );
  }
}
