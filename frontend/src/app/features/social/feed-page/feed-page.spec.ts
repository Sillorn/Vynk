import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { DisplayNameService } from '../../../core/profiles/display-name.service';
import { PostService } from '../../../core/social/post.service';
import { FeedPage } from './feed-page';

const post = (id: number, username = 'bob') => ({
  id,
  username,
  content: `post ${id}`,
  audience: 'PUBLIC',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
});
const pageOf = (...items: unknown[]) => ({
  items,
  page: 0,
  size: 20,
  totalElements: items.length,
  totalPages: 1,
  hasNext: false,
});

interface Internals {
  posts(): { id: number }[];
  error(): string | null;
  actionError(): string | null;
  engagement(): Record<number, { liked: boolean; likeCount: number; commentCount: number }>;
  selectFeed(t: 'public' | 'connections'): void;
  toggleLike(p: unknown): void;
  loading(): boolean;
}

describe('FeedPage', () => {
  const service = {
    feed: vi.fn(),
    connectionsFeed: vi.fn(),
    engagement: vi.fn(),
    toggleLike: vi.fn(),
    comments: vi.fn(),
  };
  const names = { load: vi.fn(), name: (u: string) => u };

  function create() {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { user: signal({ username: 'ada' }) } },
        { provide: PostService, useValue: service },
        { provide: DisplayNameService, useValue: names },
      ],
    });
    const fixture = TestBed.createComponent(FeedPage);
    fixture.detectChanges();
    return { fixture, page: fixture.componentInstance as unknown as Internals };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    service.engagement.mockReturnValue(of({ liked: false, likeCount: 0, commentCount: 0 }));
  });

  it('loads the public feed, engagement and author names', () => {
    service.feed.mockReturnValue(of(pageOf(post(1), post(2, 'cy'))));
    const { page } = create();
    expect(page.posts().map((p) => p.id)).toEqual([1, 2]);
    expect(service.engagement).toHaveBeenCalledTimes(2);
    expect(names.load.mock.calls.map((c) => c[0]).sort()).toEqual(['bob', 'cy']);
    expect(page.loading()).toBe(false);
  });

  it('shows a load error', () => {
    service.feed.mockReturnValue(throwError(() => ({ status: 0 })));
    const { page } = create();
    expect(page.error()).toContain('could not be reached');
    expect(page.loading()).toBe(false);
  });

  it('ignores a stale response after switching feeds', () => {
    const slow = new Subject<unknown>();
    service.feed.mockReturnValue(slow);
    service.connectionsFeed.mockReturnValue(of(pageOf(post(9))));
    const { page } = create();
    page.selectFeed('connections');
    slow.next(pageOf(post(1)));
    expect(page.posts().map((p) => p.id)).toEqual([9]);
  });

  it('updates engagement after liking', () => {
    service.feed.mockReturnValue(of(pageOf(post(1))));
    service.toggleLike.mockReturnValue(of({ liked: true, likeCount: 3 }));
    const { page } = create();
    page.toggleLike(post(1));
    expect(page.engagement()[1]).toEqual({ liked: true, likeCount: 3, commentCount: 0 });
  });

  it('reports a failed like', () => {
    service.feed.mockReturnValue(of(pageOf(post(1))));
    service.toggleLike.mockReturnValue(
      throwError(() => ({ status: 500, error: { detail: 'No' } })),
    );
    const { page } = create();
    page.toggleLike(post(1));
    expect(page.actionError()).toBe('No');
  });
});
