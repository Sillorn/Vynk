import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import {
  PostEngagement,
  PostService,
  SocialComment,
  SocialPost,
} from '../../../core/social/post.service';

@Component({
  selector: 'app-feed-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <section class="page feed-page">
      <header class="page-intro">
        <p class="eyebrow">Community</p>
        <h1>Your professional feed</h1>
        <p class="lede">Share an update and discover what the Vynk community is talking about.</p>
      </header>

      <nav class="feed-tabs" aria-label="Feed type">
        <button
          mat-button
          [class.active]="feedType() === 'public'"
          [attr.aria-pressed]="feedType() === 'public'"
          (click)="selectFeed('public')"
        >
          Public feed
        </button>
        @if (auth.user()) {
          <button
            mat-button
            [class.active]="feedType() === 'connections'"
            [attr.aria-pressed]="feedType() === 'connections'"
            (click)="selectFeed('connections')"
          >
            Connections
          </button>
        }
      </nav>

      @if (auth.user(); as user) {
        <form class="surface-card composer" (ngSubmit)="publish()">
          <label for="post-content">Share an update</label>
          <textarea
            id="post-content"
            name="content"
            [(ngModel)]="draft"
            maxlength="5000"
            required
            rows="4"
            placeholder="What would you like to share?"
          ></textarea>
          <div class="form-actions">
            <span class="muted">{{ draft.length }}/5000</span
            ><button
              mat-flat-button
              color="primary"
              type="submit"
              [disabled]="saving() || !draft.trim()"
            >
              {{ saving() ? 'Publishing…' : 'Publish' }}
            </button>
          </div>
          @if (actionError()) {
            <p class="error" role="alert">{{ actionError() }}</p>
          }
        </form>
      } @else {
        <p class="surface-card muted">
          Sign in to share an update. The public feed is open to everyone.
        </p>
      }

      @if (loading()) {
        <div class="state" role="status">
          <mat-spinner diameter="32" /><span>Loading posts…</span>
        </div>
      } @else if (error()) {
        <div class="surface-card state error" role="alert">
          <p>{{ error() }}</p>
          <button mat-button (click)="load(page())">Try again</button>
        </div>
      } @else if (!posts().length) {
        <div class="surface-card state">
          <h2>{{ feedType() === 'connections' ? 'No connection posts yet' : 'No posts yet' }}</h2>
          <p>
            {{
              feedType() === 'connections'
                ? 'Posts from your connections will appear here.'
                : 'Be the first to share something with the community.'
            }}
          </p>
        </div>
      } @else {
        <div class="post-list" aria-label="Public posts">
          @for (post of posts(); track post.id) {
            <article class="surface-card post-card">
              <header>
                <strong>{{ post.username }}</strong
                ><time [attr.datetime]="post.createdAt">{{ post.createdAt | date: 'medium' }}</time>
              </header>
              @if (editingId() === post.id) {
                <textarea
                  [id]="'edit-' + post.id"
                  [value]="editContent()"
                  (input)="editContent.set($any($event.target).value)"
                  maxlength="5000"
                  rows="4"
                  [attr.aria-label]="'Edit post by ' + post.username"
                ></textarea>
                <div class="form-actions">
                  <button mat-button (click)="cancelEdit()">Cancel</button
                  ><button
                    mat-flat-button
                    (click)="saveEdit(post)"
                    [disabled]="saving() || !editContent().trim()"
                  >
                    Save
                  </button>
                </div>
              } @else {
                <p class="post-content">{{ post.content }}</p>
                @if (auth.user()?.username?.toLowerCase() === post.username.toLowerCase()) {
                  <footer>
                    <button mat-button (click)="startEdit(post)">Edit</button
                    ><button mat-button (click)="remove(post)">Delete</button>
                  </footer>
                }
              }
              <div class="engagement-actions">
                @if (auth.user()) {
                  <button
                    mat-button
                    [attr.aria-pressed]="engagement()[post.id]?.liked ?? false"
                    [disabled]="engagingId() === post.id"
                    (click)="toggleLike(post)"
                  >
                    {{ engagement()[post.id]?.liked ? 'Unlike' : 'Like' }} ·
                    {{ engagement()[post.id]?.likeCount ?? '…' }}
                  </button>
                } @else {
                  <span>{{ engagement()[post.id]?.likeCount ?? '…' }} likes</span>
                }
                <button
                  mat-button
                  [attr.aria-expanded]="expandedPostId() === post.id"
                  (click)="toggleComments(post)"
                >
                  Comments · {{ engagement()[post.id]?.commentCount ?? '…' }}
                </button>
              </div>
              @if (expandedPostId() === post.id) {
                <section
                  class="comments"
                  [attr.aria-label]="'Comments on post by ' + post.username"
                >
                  @if (commentsLoading()) {
                    <p role="status">Loading comments…</p>
                  } @else if (!comments().length) {
                    <p class="muted">No comments yet.</p>
                  } @else {
                    @for (comment of comments(); track comment.id) {
                      <article class="comment">
                        <strong>{{ comment.username }}</strong>
                        <p>{{ comment.content }}</p>
                        <time [attr.datetime]="comment.createdAt">{{
                          comment.createdAt | date: 'medium'
                        }}</time>
                      </article>
                    }
                  }
                  @if (auth.user()) {
                    <form class="comment-form" (ngSubmit)="addComment(post)">
                      <label [for]="'comment-' + post.id">Add a comment</label>
                      <textarea
                        [id]="'comment-' + post.id"
                        name="comment"
                        [(ngModel)]="commentDraft"
                        maxlength="2000"
                        required
                        rows="2"
                      ></textarea>
                      <button
                        mat-flat-button
                        type="submit"
                        [disabled]="saving() || !commentDraft.trim()"
                      >
                        Comment
                      </button>
                    </form>
                  } @else {
                    <p class="muted">Sign in to join the conversation.</p>
                  }
                </section>
              }
              @if (actionError()) {
                <p class="error" role="alert">{{ actionError() }}</p>
              }
            </article>
          }
        </div>
        <div class="pagination">
          <button mat-button (click)="load(page() - 1)" [disabled]="page() === 0 || loading()">
            Previous</button
          ><span>Page {{ page() + 1 }} of {{ totalPages() }}</span
          ><button mat-button (click)="load(page() + 1)" [disabled]="!hasNext() || loading()">
            Next
          </button>
        </div>
      }
    </section>
  `,
  styles: [
    `
      .feed-page {
        max-width: 760px;
        margin: 0 auto;
      }
      .feed-tabs,
      .form-actions,
      .pagination {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
      }
      .feed-tabs {
        justify-content: flex-start;
        border-bottom: 1px solid #d8dee6;
        margin-bottom: 1rem;
      }
      .feed-tabs .active {
        border-bottom: 2px solid currentColor;
      }
      .composer,
      .post-card {
        display: grid;
        gap: 0.8rem;
        margin: 1rem 0;
        padding: 1.25rem;
      }
      textarea {
        width: 100%;
        box-sizing: border-box;
        resize: vertical;
        padding: 0.75rem;
        font: inherit;
        border: 1px solid #8b98a8;
        border-radius: 0.5rem;
      }
      .post-card header {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        color: #445;
      }
      .post-card time,
      .muted {
        color: #5e6875;
        font-size: 0.9rem;
      }
      .post-content {
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        margin: 0.3rem 0;
      }
      .post-card footer,
      .engagement-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
      }
      .comments {
        display: grid;
        gap: 0.75rem;
        border-top: 1px solid #d8dee6;
        padding-top: 0.75rem;
      }
      .comment {
        border-bottom: 1px solid #e5e9ef;
        padding: 0.5rem 0;
      }
      .comment p {
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        margin: 0.25rem 0;
      }
      .comment time {
        color: #5e6875;
        font-size: 0.85rem;
      }
      .comment-form {
        display: grid;
        gap: 0.5rem;
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
        margin: 1.5rem 0;
      }
      @media (max-width: 600px) {
        .post-card header {
          flex-direction: column;
          gap: 0.2rem;
        }
      }
    `,
  ],
})
export class FeedPage {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(PostService);
  protected readonly posts = signal<readonly SocialPost[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly hasNext = signal(false);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly editContent = signal('');
  protected readonly feedType = signal<'public' | 'connections'>('public');
  protected readonly engagement = signal<Record<number, PostEngagement>>({});
  protected readonly expandedPostId = signal<number | null>(null);
  protected readonly comments = signal<readonly SocialComment[]>([]);
  protected readonly commentsLoading = signal(false);
  protected readonly engagingId = signal<number | null>(null);
  protected commentDraft = '';
  protected draft = '';

  constructor() {
    this.load(0);
  }

  protected selectFeed(type: 'public' | 'connections'): void {
    if (this.feedType() === type) return;
    this.feedType.set(type);
    this.load(0);
  }

  protected load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    const request =
      this.feedType() === 'connections'
        ? this.service.connectionsFeed(page)
        : this.service.feed(page);
    request.subscribe({
      next: (result) => {
        this.posts.set(result.items);
        this.engagement.set({});
        this.expandedPostId.set(null);
        for (const post of result.items) {
          this.service.engagement(post.id).subscribe({
            next: (state) =>
              this.engagement.update((current) => ({ ...current, [post.id]: state })),
          });
        }
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
        this.hasNext.set(result.hasNext);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(describeError(err));
        this.loading.set(false);
      },
    });
  }

  protected publish(): void {
    const content = this.draft.trim();
    if (!content || this.saving()) return;
    this.saving.set(true);
    this.actionError.set(null);
    this.service.create(content).subscribe({
      next: () => {
        this.draft = '';
        this.saving.set(false);
        this.load(0);
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(describeError(err));
        this.saving.set(false);
      },
    });
  }

  protected toggleLike(post: SocialPost): void {
    this.engagingId.set(post.id);
    this.service.toggleLike(post.id).subscribe({
      next: (result) => {
        this.engagement.update((current) => ({
          ...current,
          [post.id]: {
            liked: result.liked,
            likeCount: result.likeCount,
            commentCount: current[post.id]?.commentCount ?? 0,
          },
        }));
        this.engagingId.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(describeError(err));
        this.engagingId.set(null);
      },
    });
  }

  protected toggleComments(post: SocialPost): void {
    if (this.expandedPostId() === post.id) {
      this.expandedPostId.set(null);
      return;
    }
    this.expandedPostId.set(post.id);
    this.commentsLoading.set(true);
    this.comments.set([]);
    this.service.comments(post.id).subscribe({
      next: (result) => {
        this.comments.set(result.items);
        this.commentsLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(describeError(err));
        this.commentsLoading.set(false);
      },
    });
  }

  protected addComment(post: SocialPost): void {
    const content = this.commentDraft.trim();
    if (!content || this.saving()) return;
    this.saving.set(true);
    this.service.addComment(post.id, content).subscribe({
      next: (comment) => {
        this.commentDraft = '';
        this.comments.update((items) => [...items, comment]);
        this.engagement.update((current) => ({
          ...current,
          [post.id]: {
            liked: current[post.id]?.liked ?? false,
            likeCount: current[post.id]?.likeCount ?? 0,
            commentCount: (current[post.id]?.commentCount ?? 0) + 1,
          },
        }));
        this.saving.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(describeError(err));
        this.saving.set(false);
      },
    });
  }

  protected startEdit(post: SocialPost): void {
    this.editingId.set(post.id);
    this.editContent.set(post.content);
  }
  protected cancelEdit(): void {
    this.editingId.set(null);
    this.editContent.set('');
  }

  protected saveEdit(post: SocialPost): void {
    this.saving.set(true);
    this.actionError.set(null);
    this.service.update(post.id, this.editContent().trim()).subscribe({
      next: () => {
        this.cancelEdit();
        this.saving.set(false);
        this.load(this.page());
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(describeError(err));
        this.saving.set(false);
      },
    });
  }

  protected remove(post: SocialPost): void {
    if (!globalThis.confirm('Delete this post? This cannot be undone.')) return;
    this.saving.set(true);
    this.actionError.set(null);
    this.service.delete(post.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.load(this.page());
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(describeError(err));
        this.saving.set(false);
      },
    });
  }
}

function describeError(error: HttpErrorResponse): string {
  if (error.status === 0)
    return 'The server could not be reached. Check your connection and try again.';
  const detail = error.error?.detail;
  return typeof detail === 'string' && detail.trim()
    ? detail
    : 'Something went wrong. Please try again.';
}
