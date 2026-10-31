import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PeopleCard, ProfileService } from '../../../core/profiles/profile.service';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-people-directory',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <section class="page directory">
      <header class="page-intro">
        <p class="eyebrow">Community</p>
        <h1>Find people</h1>
        <p class="lede">Discover professionals by name, headline or skills.</p>
      </header>

      <form class="search surface-card" role="search" (ngSubmit)="search()">
        <label for="people-query">Search people</label>
        <div class="search-row">
          <input
            id="people-query"
            name="query"
            type="search"
            [(ngModel)]="draft"
            maxlength="100"
            autocomplete="off"
            placeholder="Name, headline or skill"
          />
          <button mat-flat-button type="submit" [disabled]="loading()">Search</button>
        </div>
      </form>

      @if (loading()) {
        <div class="state" role="status">
          <mat-spinner diameter="32" /><span>Searching people…</span>
        </div>
      } @else if (error()) {
        <div class="surface-card state error" role="alert">
          <p>{{ error() }}</p>
          <button mat-button (click)="load(page())">Try again</button>
        </div>
      } @else if (!people().length) {
        <div class="surface-card state">
          <h2>No people found</h2>
          <p>Try a different name, headline or skill.</p>
        </div>
      } @else {
        <p class="result-count" aria-live="polite">
          {{ totalElements() }} {{ totalElements() === 1 ? 'person' : 'people' }} found
        </p>
        <ul class="people-list" aria-label="People search results">
          @for (person of people(); track person.username) {
            <li class="surface-card person-card">
              <div class="avatar" aria-hidden="true">{{ initials(person) }}</div>
              <div class="details">
                <h2>
                  <a [routerLink]="['/profile', person.username]"
                    >{{ person.firstName }} {{ person.lastName }}</a
                  >
                </h2>
                <p>{{ person.headline }}</p>
                @if (person.location) {
                  <span class="location">{{ person.location }}</span>
                }
                <span class="username">@{{ person.username }}</span>
              </div>
              <a
                mat-button
                [routerLink]="['/profile', person.username]"
                [attr.aria-label]="'View ' + person.firstName + ' ' + person.lastName + ' profile'"
                >View profile</a
              >
            </li>
          }
        </ul>
        @if (totalPages() > 1) {
          <nav class="pagination" aria-label="People search pages">
            <button mat-button (click)="go(page() - 1)" [disabled]="page() === 0">Previous</button>
            <span>Page {{ page() + 1 }} of {{ totalPages() }}</span>
            <button mat-button (click)="go(page() + 1)" [disabled]="!hasNext()">Next</button>
          </nav>
        }
      }
    </section>
  `,
  styles: [
    `
      .directory {
        max-width: 820px;
        margin: 0 auto;
      }
      .search {
        display: grid;
        gap: 0.65rem;
        padding: 1rem;
        margin: 1.5rem 0;
      }
      .search-row {
        display: flex;
        gap: 0.75rem;
      }
      input {
        box-sizing: border-box;
        min-width: 0;
        flex: 1;
        padding: 0.7rem 0.8rem;
        border: 1px solid #7b8795;
        border-radius: 0.5rem;
        font: inherit;
      }
      .people-list {
        list-style: none;
        padding: 0;
        display: grid;
        gap: 0.75rem;
      }
      .person-card {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1rem;
      }
      .avatar {
        display: grid;
        place-items: center;
        flex: 0 0 3rem;
        width: 3rem;
        height: 3rem;
        border-radius: 50%;
        background: #e5eaf2;
        color: #263c58;
        font-weight: 700;
      }
      .details {
        flex: 1;
        min-width: 0;
      }
      h2 {
        margin: 0;
        font-size: 1.1rem;
      }
      h2 a {
        color: inherit;
        text-decoration: none;
      }
      h2 a:hover {
        text-decoration: underline;
      }
      .details p {
        margin: 0.25rem 0;
        overflow-wrap: anywhere;
      }
      .location,
      .username,
      .result-count {
        color: #596574;
        font-size: 0.9rem;
      }
      .location {
        margin-right: 0.75rem;
      }
      .state {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 1rem;
        padding: 2rem;
      }
      .error {
        color: #a52222;
      }
      .pagination {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 1rem;
        margin: 1.5rem 0;
      }
      @media (max-width: 540px) {
        .person-card {
          align-items: flex-start;
          flex-wrap: wrap;
        }
        .details {
          flex-basis: calc(100% - 4rem);
        }
        .search-row {
          align-items: stretch;
        }
      }
    `,
  ],
})
export class PeopleDirectory {
  private readonly service = inject(ProfileService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly people = signal<readonly PeopleCard[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly hasNext = signal(false);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected draft = '';
  private query = '';

  constructor() {
    this.query = this.route.snapshot.queryParamMap.get('q') ?? '';
    this.draft = this.query;
    const requestedPage = Number(this.route.snapshot.queryParamMap.get('page') ?? 0);
    this.load(Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0);
  }

  protected search(): void {
    this.query = this.draft.trim();
    this.syncUrl(0);
    this.load(0);
  }

  protected go(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.syncUrl(page);
    this.load(page);
  }

  protected load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.searchPeople(this.query, page).subscribe({
      next: (result) => {
        const user = this.auth.user()?.username.toLowerCase();
        const isSelf = (person: PeopleCard) => person.username.toLowerCase() === user;
        const items = user ? result.items.filter((person) => !isSelf(person)) : result.items;
        this.people.set(items);
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
        this.totalElements.set(
          Math.max(0, result.totalElements - (user && result.items.some(isSelf) ? 1 : 0)),
        );
        this.hasNext.set(result.hasNext);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(
          err.status === 0
            ? 'Unable to reach the server. Check your connection and try again.'
            : 'People could not be loaded. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }

  protected initials(person: PeopleCard): string {
    return `${person.firstName.trim().charAt(0)}${person.lastName.trim().charAt(0)}`.toUpperCase();
  }

  private syncUrl(page: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { q: this.query || null, page: page || null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
