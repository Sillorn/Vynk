import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/auth/auth.service';
import { EventStreamService } from '../../../core/social/event-stream.service';
import { ConnectionService } from '../../../core/social/connection.service';
import { ProfileService } from '../../../core/profiles/profile.service';
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
            <div class="contact-picker">
              <label for="new-conversation">Start a conversation</label>
              <select
                id="new-conversation"
                (change)="startConversation($any($event.target).value)"
              >
              <option value="">Choose a contact…</option>
              @for (contact of availableContacts(); track contact) {
                <option [value]="contact">{{ displayName(contact) }}</option>
              }
              </select>
            </div>
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
                    >{{ displayName(item.participantUsername) }} <span class="username">&#64;{{ item.participantUsername }}</span>
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
                <h2>{{ displayName(selected()?.participantUsername ?? '') }} <span class="username conversation-username">&#64;{{ selected()?.participantUsername }}</span></h2>
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
                        : displayName(message.senderUsername)
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
      .contact-picker {
        display: grid;
        gap: 0.45rem;
        margin: 0 0 1rem;
        padding: 0.85rem;
        border: 1px solid var(--mat-sys-outline-variant);
        border-radius: 0.75rem;
        background: var(--mat-sys-surface-container-low);
      }
      .contact-picker label {
        color: var(--mat-sys-on-surface-variant);
        font-size: 0.85rem;
        font-weight: 650;
      }
      .contact-picker select {
        width: 100%;
        min-height: 2.75rem;
        padding: 0.6rem 2.5rem 0.6rem 0.75rem;
        appearance: none;
        color: var(--mat-sys-on-surface);
        font: inherit;
        font-size: 0.95rem;
        border: 1px solid var(--mat-sys-outline);
        border-radius: 0.5rem;
        background-color: var(--mat-sys-surface-container-high);
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16' fill='none' stroke='%23596675' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m4 6 4 4 4-4'/%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-position: right 0.75rem center;
        cursor: pointer;
      }
      .contact-picker select:focus-visible {
        outline: 3px solid var(--mat-sys-primary-container);
        outline-offset: 2px;
        border-color: var(--mat-sys-primary);
      }
      .contact-picker select:hover {
        border-color: var(--mat-sys-on-surface-variant);
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
        border-top: 1px solid var(--mat-sys-outline-variant);
        cursor: pointer;
      }
      .inbox-item:hover,
      .inbox-item.selected {
        background: var(--mat-sys-surface-container-high);
      }
      .inbox-item__name {
        font-weight: 650;
        display: flex;
        justify-content: space-between;
      }
      .username {
        color: color-mix(in srgb, var(--mat-sys-on-surface-variant) 70%, transparent);
        font-size: 0.85em;
        font-weight: normal;
      }
      .conversation-username {
        display: inline-block;
        margin-left: 0.35rem;
        font-size: 0.65em;
        vertical-align: baseline;
      }
      .inbox-item__preview {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        color: var(--mat-sys-on-surface-variant);
      }
      .inbox-item time,
      .message-list time {
        font-size: 0.8rem;
        color: var(--mat-sys-on-surface-variant);
      }
      .badge {
        display: inline-grid;
        place-items: center;
        min-width: 1.4rem;
        height: 1.4rem;
        border-radius: 99px;
        background: var(--mat-sys-primary);
        color: var(--mat-sys-on-primary);
        font-size: 0.75rem;
      }
      .conversation {
        display: flex;
        flex-direction: column;
        min-height: 28rem;
      }
      .conversation__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        border-bottom: 1px solid var(--mat-sys-outline-variant);
      }
      .conversation__header h2 {
        flex: 1;
      }
      .conversation__header .conversation-username {
        float: right;
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
        background: var(--mat-sys-surface-container-high);
        color: var(--mat-sys-on-surface);
      }
      .message-list li.own {
        align-self: flex-end;
        background: var(--mat-sys-primary-container);
        color: var(--mat-sys-on-primary-container);
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
        border-top: 1px solid var(--mat-sys-outline-variant);
        padding-top: 0.75rem;
      }
      .composer textarea {
        flex: 1;
        min-width: 0;
        resize: vertical;
        padding: 0.65rem;
        font: inherit;
        color: var(--mat-sys-on-surface);
        background: var(--mat-sys-surface-container-high);
        border: 1px solid var(--mat-sys-outline);
        border-radius: 0.4rem;
      }
      .muted {
        color: var(--mat-sys-on-surface-variant);
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
  private readonly connectionService = inject(ConnectionService);
  private readonly profileService = inject(ProfileService);
  private readonly names = signal<Record<string, string>>({});
  protected displayName(username: string): string {
    return this.names()[username] ?? username;
  }
  protected readonly contacts = signal<readonly string[]>([]);
  protected readonly availableContacts = () => {
    const conversations = new Set(this.inbox().map((item) => item.participantUsername));
    return this.contacts().filter((contact) => !conversations.has(contact));
  };
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
        this.connectionService.contacts().subscribe({ next: (contacts) => {
          this.contacts.set(contacts);
          contacts.forEach((username) => this.loadName(username));
        } });
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

  private loadName(username: string): void {
    if (!username || this.names()[username]) return;
    this.profileService.getPublicProfile(username).subscribe({
      next: (profile) => this.names.update((names) => ({
        ...names,
        [username]: `${profile.firstName} ${profile.lastName}`.trim() || username,
      })),
    });
  }

  protected reload(): void {
    this.error.set(null);
    this.loadingInbox.set(true);
    this.service.inbox().subscribe({
      next: (items) => {
        this.inbox.set(items);
        items.forEach((item) => this.loadName(item.participantUsername));
        this.loadingInbox.set(false);
      },
      error: (err) => {
        this.fail(err);
        this.loadingInbox.set(false);
      },
    });
  }

  protected startConversation(username: string): void {
    if (!username) return;
    const existing = this.inbox().find((item) => item.participantUsername === username);
    if (existing) {
      this.open(existing);
      return;
    }
    this.selected.set({ conversationId: 0, participantId: 0, participantUsername: username, latestMessage: null, unreadCount: 0 });
    this.messages.set([]);
    this.page = 0;
    this.hasOlder.set(false);
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
        const item: InboxItem = {
          conversationId: message.conversationId,
          participantId: 0,
          participantUsername: message.senderUsername === this.auth.user()?.username
            ? this.selected()?.participantUsername ?? '' : message.senderUsername,
          latestMessage: message,
          unreadCount: 0,
        };
        this.selected.set(item);
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
    if (!item.conversationId) {
      this.loadingMessages.set(false);
      return;
    }
    this.service.history(item.conversationId, this.page).subscribe({
      next: (response) => {
        response.items.forEach((message) => {
          if (message.senderUsername !== this.auth.user()?.username) this.loadName(message.senderUsername);
        });
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
