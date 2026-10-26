import { Injectable } from '@angular/core';

/** Maintains an authenticated same-origin SSE connection with bounded exponential retry. */
@Injectable({ providedIn: 'root' })
export class EventStreamService {
  connect(onEvent: (eventName: string) => void): () => void {
    let source: EventSource | null = null;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;
    let retryDelay = 1000;
    let closed = false;

    const open = () => {
      if (closed) return;
      source = new EventSource('/api/v1/events', { withCredentials: true });
      source.onopen = () => {
        retryDelay = 1000;
      };
      source.addEventListener('message', () => onEvent('message'));
      source.onerror = () => {
        source?.close();
        source = null;
        if (closed || retryTimer !== null) return;
        retryTimer = setTimeout(() => {
          retryTimer = null;
          open();
        }, retryDelay);
        retryDelay = Math.min(retryDelay * 2, 30_000);
      };
    };

    open();
    return () => {
      closed = true;
      source?.close();
      source = null;
      if (retryTimer !== null) clearTimeout(retryTimer);
    };
  }
}
