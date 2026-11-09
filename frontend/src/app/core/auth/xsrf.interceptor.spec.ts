import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { xsrfInterceptor } from './xsrf.interceptor';

describe('xsrfInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([xsrfInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    controller.verify();
  });

  it('leaves GET requests untouched', () => {
    document.cookie = 'XSRF-TOKEN=abc; path=/';
    http.get('/api/v1/x').subscribe();
    const req = controller.expectOne('/api/v1/x');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush({});
  });

  it('adds the token header and credentials to mutating requests', () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/';
    http.post('/api/v1/x', {}).subscribe();
    const req = controller.expectOne('/api/v1/x');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('abc123');
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });

  it('still sends credentials when no token cookie exists', () => {
    http.delete('/api/v1/x').subscribe();
    const req = controller.expectOne('/api/v1/x');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });
});
