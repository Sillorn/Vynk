import { HttpErrorResponse } from '@angular/common/http';
import { describeHttpError } from './http-error';

describe('describeHttpError', () => {
  it('reports an unreachable server for status 0', () => {
    expect(describeHttpError(new HttpErrorResponse({ status: 0 }), 'fb')).toContain(
      'could not be reached',
    );
  });

  it('prefers the RFC 9457 detail', () => {
    const err = new HttpErrorResponse({ status: 400, error: { detail: 'Too short' } });
    expect(describeHttpError(err, 'fb')).toBe('Too short');
  });

  it('uses the fallback for missing or blank detail', () => {
    expect(describeHttpError(new HttpErrorResponse({ status: 500 }), 'fb')).toBe('fb');
    expect(
      describeHttpError(new HttpErrorResponse({ status: 500, error: { detail: ' ' } }), 'fb'),
    ).toBe('fb');
  });
});
