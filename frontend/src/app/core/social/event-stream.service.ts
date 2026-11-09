import { Injectable } from '@angular/core';

export type StreamEventName = 'message' | 'notification';

interface Listener {
  readonly onEvent: (eventName: StreamEventName) => void;
  readonly onReconnect?: () => void;
}

/**
 * Maintains one authenticated same-origin SSE connection per browser tab, shared by all
 * subscribers, with bounded exponential retry. The connection is open only while at least
 * one subscriber is registered.
 *
 * Events missed while disconnected are not replayed, so `onReconnect` lets subscribers
 * reconcile their state from REST after the stream comes back.
 */
@Injectable({ providedIn: 'root' })
export class EventStreamService {
  private readonly listeners = new Set<Listener>();
  private source: EventSource | null = null;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private retryDelay = 1000;
  private hadError = false;

  connect(onEvent: (eventName: StreamEventName) => void, onReconnect?: () => void): () => void {
    const listener: Listener = { onEvent, onReconnect };
    this.listeners.add(listener);
    if (!this.source && this.retryTimer === null) this.open();
    return () => {
      this.listeners.delete(listener);
      if (this.listeners.size === 0) this.close();
    };
  }

  private open(): void {
    if (this.listeners.size === 0) return;
    const source = new EventSource('/api/v1/events', { withCredentials: true });
    this.source = source;
    source.onopen = () => {
      this.retryDelay = 1000;
      if (this.hadError) {
        this.hadError = false;
        [...this.listeners].forEach((listener) => listener.onReconnect?.());
      }
    };
    source.addEventListener('message', () => this.emit('message'));
    source.addEventListener('notification', () => this.emit('notification'));
    source.onerror = () => {
      source.close();
      if (this.source === source) this.source = null;
      this.hadError = true;
      if (this.listeners.size === 0 || this.retryTimer !== null) return;
      this.retryTimer = setTimeout(() => {
        this.retryTimer = null;
        this.open();
      }, this.retryDelay);
      this.retryDelay = Math.min(this.retryDelay * 2, 30_000);
    };
  }

  private emit(eventName: StreamEventName): void {
    [...this.listeners].forEach((listener) => listener.onEvent(eventName));
  }

  private close(): void {
    this.source?.close();
    this.source = null;
    if (this.retryTimer !== null) clearTimeout(this.retryTimer);
    this.retryTimer = null;
    this.retryDelay = 1000;
    this.hadError = false;
  }
}
