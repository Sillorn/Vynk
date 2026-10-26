import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface ConnectionRequest {
  readonly id: number;
  readonly username: string;
  readonly createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ConnectionService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/v1/connections';
  incoming() {
    return this.http.get<ConnectionRequest[]>(`${this.endpoint}/requests/incoming`, {
      withCredentials: true,
    });
  }
  outgoing() {
    return this.http.get<ConnectionRequest[]>(`${this.endpoint}/requests/outgoing`, {
      withCredentials: true,
    });
  }
  accept(id: number) {
    return this.http.post<void>(
      `${this.endpoint}/requests/${id}/accept`,
      {},
      { withCredentials: true },
    );
  }
  decline(id: number) {
    return this.http.post<void>(
      `${this.endpoint}/requests/${id}/decline`,
      {},
      { withCredentials: true },
    );
  }
  cancel(id: number) {
    return this.http.delete<void>(`${this.endpoint}/requests/${id}`, { withCredentials: true });
  }
  send(username: string) {
    return this.http.post<void>(
      `${this.endpoint}/${encodeURIComponent(username)}/requests`,
      {},
      { withCredentials: true },
    );
  }
  disconnect(username: string) {
    return this.http.delete<void>(`${this.endpoint}/${encodeURIComponent(username)}`, {
      withCredentials: true,
    });
  }
}
