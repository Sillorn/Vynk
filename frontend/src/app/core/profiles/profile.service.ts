import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PublicProfile } from './profile.models';

const API_BASE = '/api/v1/profiles';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  /** Reads a public profile. Anonymous access is allowed and only public data is returned. */
  getPublicProfile(username: string): Observable<PublicProfile> {
    return this.http.get<PublicProfile>(`${API_BASE}/${encodeURIComponent(username)}`, {
      withCredentials: true,
    });
  }
}
