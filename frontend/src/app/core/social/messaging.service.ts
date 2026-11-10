import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface ConversationMessage {
  readonly id: number;
  readonly conversationId: number;
  readonly senderId: number;
  readonly senderUsername: string;
  readonly body: string;
  readonly createdAt: string;
}

export interface InboxItem {
  readonly conversationId: number;
  readonly participantId: number;
  readonly participantUsername: string;
  readonly latestMessage: ConversationMessage | null;
  readonly unreadCount: number;
}

export interface InboxPage {
  readonly items: readonly InboxItem[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

export interface UnreadSummary {
  readonly unreadConversations: number;
}

export interface MessagePage {
  readonly items: readonly ConversationMessage[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/v1/conversations';

  inbox(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<InboxPage>(this.endpoint, { params, withCredentials: true });
  }

  /** Badge source: counts every visible conversation, not just the loaded inbox page. */
  unreadSummary() {
    return this.http.get<UnreadSummary>(`${this.endpoint}/unread-summary`, {
      withCredentials: true,
    });
  }

  history(conversationId: number, page = 0, size = 30) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<MessagePage>(`${this.endpoint}/${conversationId}/messages`, {
      params,
      withCredentials: true,
    });
  }

  /** The same clientRequestId must be reused when retrying a failed send. */
  send(username: string, body: string, clientRequestId: string) {
    return this.http.post<ConversationMessage>(
      `${this.endpoint}/with/${encodeURIComponent(username)}/messages`,
      { body, clientRequestId },
      { withCredentials: true },
    );
  }

  hide(conversationId: number) {
    return this.http.delete<void>(`${this.endpoint}/${conversationId}`, { withCredentials: true });
  }

  markRead(conversationId: number) {
    return this.http.post<void>(
      `${this.endpoint}/${conversationId}/read`,
      {},
      { withCredentials: true },
    );
  }
}
