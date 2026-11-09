import { TestBed } from '@angular/core/testing';
import { EventStreamService } from './event-stream.service';

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  closed = false;
  private handlers = new Map<string, () => void>();
  constructor(
    readonly url: string,
    readonly init?: EventSourceInit,
  ) {
    FakeEventSource.instances.push(this);
  }
  addEventListener(name: string, handler: () => void) {
    this.handlers.set(name, handler);
  }
  close() {
    this.closed = true;
  }
  dispatch(name: string) {
    this.handlers.get(name)?.();
  }
}

describe('EventStreamService', () => {
  let service: EventStreamService;

  beforeEach(() => {
    vi.useFakeTimers();
    FakeEventSource.instances = [];
    vi.stubGlobal('EventSource', FakeEventSource);
    service = TestBed.inject(EventStreamService);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('shares one credentialed connection between subscribers', () => {
    service.connect(() => {});
    service.connect(() => {});
    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.instances[0].url).toBe('/api/v1/events');
    expect(FakeEventSource.instances[0].init?.withCredentials).toBe(true);
  });

  it('fans events out to all subscribers', () => {
    const a = vi.fn();
    const b = vi.fn();
    service.connect(a);
    service.connect(b);
    FakeEventSource.instances[0].dispatch('message');
    FakeEventSource.instances[0].dispatch('notification');
    expect(a.mock.calls).toEqual([['message'], ['notification']]);
    expect(b).toHaveBeenCalledTimes(2);
  });

  it('closes when the last subscriber leaves and reopens on demand', () => {
    const off1 = service.connect(() => {});
    const off2 = service.connect(() => {});
    off1();
    expect(FakeEventSource.instances[0].closed).toBe(false);
    off2();
    expect(FakeEventSource.instances[0].closed).toBe(true);
    service.connect(() => {});
    expect(FakeEventSource.instances).toHaveLength(2);
  });

  it('retries with exponential backoff and signals reconnects only after an error', () => {
    const onReconnect = vi.fn();
    service.connect(() => {}, onReconnect);
    FakeEventSource.instances[0].onopen?.();
    expect(onReconnect).not.toHaveBeenCalled();

    FakeEventSource.instances[0].onerror?.();
    expect(FakeEventSource.instances[0].closed).toBe(true);
    vi.advanceTimersByTime(999);
    expect(FakeEventSource.instances).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(FakeEventSource.instances).toHaveLength(2);

    // Second failure waits twice as long.
    FakeEventSource.instances[1].onerror?.();
    vi.advanceTimersByTime(1999);
    expect(FakeEventSource.instances).toHaveLength(2);
    vi.advanceTimersByTime(1);
    expect(FakeEventSource.instances).toHaveLength(3);

    FakeEventSource.instances[2].onopen?.();
    expect(onReconnect).toHaveBeenCalledTimes(1);
    FakeEventSource.instances[2].onopen?.();
    expect(onReconnect).toHaveBeenCalledTimes(1);
  });

  it('does not reconnect after all subscribers unsubscribed during backoff', () => {
    const off = service.connect(() => {});
    FakeEventSource.instances[0].onerror?.();
    off();
    vi.advanceTimersByTime(60_000);
    expect(FakeEventSource.instances).toHaveLength(1);
  });
});
