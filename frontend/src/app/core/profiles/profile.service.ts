import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { PublicProfile } from './profile.models';

const API_BASE = '/api/v1/profiles';

export interface PeopleCard {
  readonly username: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly headline: string;
  readonly location: string | null;
}

export interface PeoplePage {
  readonly items: readonly PeopleCard[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  searchPeople(query: string, page: number): Observable<PeoplePage> {
    return this.http.get<PeoplePage>(API_BASE, {
      params: { q: query, page, size: 12 },
      withCredentials: true,
    });
  }

  getOwnProfile(): Observable<PublicProfile> {
    return this.http.get<PublicProfile>(`${API_BASE}/me`, { withCredentials: true });
  }

  saveBasics(
    payload: Pick<PublicProfile, 'firstName' | 'lastName' | 'headline' | 'summary'> & {
      location: string;
    },
  ): Observable<PublicProfile> {
    return this.http.put<PublicProfile>(`${API_BASE}/me`, payload, { withCredentials: true });
  }

  saveAll(value: Record<string, unknown>): Observable<PublicProfile> {
    const keys = [
      'links',
      'experience',
      'education',
      'certifications',
      'projects',
      'skills',
      'languages',
    ] as const;
    const requests = [
      this.http.put<PublicProfile>(
        `${API_BASE}/me`,
        {
          firstName: value['firstName'],
          lastName: value['lastName'],
          headline: value['headline'],
          summary: value['summary'],
          location: value['location'],
        },
        { withCredentials: true },
      ),
      ...keys.map((key) =>
        this.http.put<PublicProfile>(
          `${API_BASE}/me/${key}`,
          { items: value[key] },
          { withCredentials: true },
        ),
      ),
    ];
    return forkJoin(requests).pipe(map((profiles) => profiles[profiles.length - 1]));
  }

  /** Reads a public profile. Anonymous access is allowed and only public data is returned. */
  getPublicProfile(username: string): Observable<PublicProfile> {
    return this.http.get<PublicProfile>(`${API_BASE}/${encodeURIComponent(username)}`, {
      withCredentials: true,
    });
  }
}
