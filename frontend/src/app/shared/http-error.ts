import { HttpErrorResponse } from '@angular/common/http';

/**
 * Turns an API failure into a user-facing message: a network message for status 0,
 * the RFC 9457 `detail` when present, otherwise the supplied fallback.
 */
export function describeHttpError(error: HttpErrorResponse, fallback: string): string {
  if (error.status === 0) {
    return 'The server could not be reached. Check your connection and try again.';
  }
  const detail = error.error?.detail;
  return typeof detail === 'string' && detail.trim() ? detail : fallback;
}
