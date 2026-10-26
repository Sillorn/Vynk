import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface NotificationItem {
  readonly id: number;
  readonly type: string;
  readonly actorUsername: string;
  readonly createdAt: string;
  readonly readAt: string | null;
}

export interface NotificationPage {
  readonly items: readonly NotificationItem[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/v1/notifications';

  list(page = 0, size = 30) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<NotificationPage>(this.endpoint, { params, withCredentials: true });
  }

  markRead(id: number) {
    return this.http.post<void>(`${this.endpoint}/${id}/read`, {}, { withCredentials: true });
  }
}
