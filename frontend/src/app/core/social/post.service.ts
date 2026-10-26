import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface SocialPost {
  readonly id: number;
  readonly username: string;
  readonly content: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PostEngagement {
  readonly liked: boolean;
  readonly likeCount: number;
  readonly commentCount: number;
}

export interface SocialComment {
  readonly id: number;
  readonly postId: number;
  readonly username: string;
  readonly content: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PageResponse<T> {
  readonly items: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

@Injectable({ providedIn: 'root' })
export class PostService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/v1/posts';

  feed(page: number) {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<PageResponse<SocialPost>>(this.endpoint, {
      params,
      withCredentials: true,
    });
  }

  connectionsFeed(page: number) {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<PageResponse<SocialPost>>(`${this.endpoint}/connections`, {
      params,
      withCredentials: true,
    });
  }

  engagement(id: number) {
    return this.http.get<PostEngagement>(`${this.endpoint}/${id}/engagement`, {
      withCredentials: true,
    });
  }

  toggleLike(id: number) {
    return this.http.post<{ liked: boolean; likeCount: number }>(
      `${this.endpoint}/${id}/likes`,
      {},
      { withCredentials: true },
    );
  }

  comments(id: number) {
    const params = new HttpParams().set('page', 0).set('size', 100);
    return this.http.get<PageResponse<SocialComment>>(`${this.endpoint}/${id}/comments`, {
      params,
      withCredentials: true,
    });
  }

  addComment(id: number, content: string) {
    return this.http.post<SocialComment>(
      `${this.endpoint}/${id}/comments`,
      { content },
      { withCredentials: true },
    );
  }

  create(content: string) {
    return this.http.post<SocialPost>(this.endpoint, { content }, { withCredentials: true });
  }

  update(id: number, content: string) {
    return this.http.put<SocialPost>(
      `${this.endpoint}/${id}`,
      { content },
      { withCredentials: true },
    );
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.endpoint}/${id}`, { withCredentials: true });
  }
}
