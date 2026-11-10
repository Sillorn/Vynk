import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { PageResponse } from './post.service';

export interface ConnectionRequest {
  readonly id: number;
  readonly username: string;
  readonly createdAt: string;
}

/** Largest page the backend accepts for connection lists (PN-066). */
const MAX_PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class ConnectionService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/v1/connections';

  contacts(page = 0, size = 20) {
    return this.http.get<PageResponse<string>>(this.endpoint, {
      params: this.pageParams(page, size),
      withCredentials: true,
    });
  }

  /** Walks every page of contacts. Use only where the full list is required. */
  allContacts(): Observable<string[]> {
    return this.collectAll((page, size) => this.contacts(page, size));
  }

  isConnected(username: string) {
    return this.http.get<boolean>(`${this.endpoint}/${encodeURIComponent(username)}/status`, {
      withCredentials: true,
    });
  }

  incoming(page = 0, size = 20) {
    return this.http.get<PageResponse<ConnectionRequest>>(`${this.endpoint}/requests/incoming`, {
      params: this.pageParams(page, size),
      withCredentials: true,
    });
  }

  outgoing(page = 0, size = 20) {
    return this.http.get<PageResponse<ConnectionRequest>>(`${this.endpoint}/requests/outgoing`, {
      params: this.pageParams(page, size),
      withCredentials: true,
    });
  }

  /** Walks every page of outgoing requests. Use only where the full list is required. */
  allOutgoing(): Observable<ConnectionRequest[]> {
    return this.collectAll((page, size) => this.outgoing(page, size));
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

  private pageParams(page: number, size: number): HttpParams {
    return new HttpParams().set('page', page).set('size', size);
  }

  private collectAll<T>(
    fetch: (page: number, size: number) => Observable<PageResponse<T>>,
    page = 0,
    acc: readonly T[] = [],
  ): Observable<T[]> {
    return fetch(page, MAX_PAGE_SIZE).pipe(
      switchMap((response) => {
        const items = [...acc, ...response.items];
        return response.hasNext ? this.collectAll(fetch, page + 1, items) : of(items);
      }),
    );
  }
}
