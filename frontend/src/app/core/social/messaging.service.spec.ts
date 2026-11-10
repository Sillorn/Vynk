import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessagingService } from './messaging.service';

describe('MessagingService', () => {
  let service: MessagingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MessagingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests one inbox page with explicit paging parameters', () => {
    service.inbox(2, 20).subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/conversations');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ items: [], page: 2, size: 20, totalElements: 0, totalPages: 0, hasNext: false });
  });

  it('defaults to the first page of 20 conversations', () => {
    service.inbox().subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/conversations');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false });
  });

  it('reads the unread summary from its own endpoint', () => {
    let count = -1;
    service.unreadSummary().subscribe((summary) => (count = summary.unreadConversations));
    http.expectOne('/api/v1/conversations/unread-summary').flush({ unreadConversations: 4 });
    expect(count).toBe(4);
  });
});
