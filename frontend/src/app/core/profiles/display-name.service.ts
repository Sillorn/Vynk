import { Injectable, inject, signal } from '@angular/core';
import { ProfileService } from './profile.service';

/** App-wide cache of "First Last" display names keyed by username. */
@Injectable({ providedIn: 'root' })
export class DisplayNameService {
  private readonly profiles = inject(ProfileService);
  private readonly names = signal<Record<string, string>>({});
  private readonly requested = new Set<string>();

  /** Reactive: reads the cache signal, falling back to the username until loaded. */
  name(username: string): string {
    return this.names()[username] ?? username;
  }

  load(username: string): void {
    if (!username || this.requested.has(username)) return;
    this.requested.add(username);
    this.profiles.getPublicProfile(username).subscribe({
      next: (profile) =>
        this.names.update((names) => ({
          ...names,
          [username]: `${profile.firstName} ${profile.lastName}`.trim() || username,
        })),
      // Allow a later retry; the username stays as the fallback label.
      error: () => this.requested.delete(username),
    });
  }
}
