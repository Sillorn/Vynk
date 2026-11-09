import { TestBed } from '@angular/core/testing';
import { PwaUpdateService } from './pwa-update.service';

describe('PwaUpdateService', () => {
  type Listener = () => void;

  function fakeWorker(state = 'installed') {
    const listeners: Record<string, Listener> = {};
    return {
      state,
      postMessage: vi.fn(),
      addEventListener: (n: string, l: Listener) => (listeners[n] = l),
      fire: (n: string) => listeners[n]?.(),
      set: (s: string) => (fakeWorkerState.state = s),
    };
  }
  const fakeWorkerState = { state: '' };

  function setup(controller: unknown) {
    const regListeners: Record<string, Listener> = {};
    const registration = {
      waiting: null as ReturnType<typeof fakeWorker> | null,
      installing: null as ReturnType<typeof fakeWorker> | null,
      addEventListener: (n: string, l: Listener) => (regListeners[n] = l),
      update: vi.fn().mockResolvedValue(undefined),
    };
    const sw = {
      controller,
      register: vi.fn().mockResolvedValue(registration),
      addEventListener: vi.fn(),
    };
    Object.defineProperty(navigator, 'serviceWorker', { value: sw, configurable: true });
    return { registration, regListeners, sw };
  }

  beforeEach(() => {
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    // @ts-expect-error cleanup of test stub
    delete navigator.serviceWorker;
  });

  it('does nothing in dev mode', () => {
    const { sw } = setup({});
    TestBed.inject(PwaUpdateService).init(false);
    expect(sw.register).not.toHaveBeenCalled();
  });

  it('does not flag the first install (no controller)', async () => {
    const { registration, regListeners } = setup(null);
    const service = TestBed.inject(PwaUpdateService);
    service.init(true);
    await vi.waitFor(() => expect(Object.keys(regListeners)).toContain('updatefound'));
    const worker = fakeWorker();
    registration.installing = worker;
    regListeners['updatefound']();
    worker.fire('statechange');
    expect(service.updateAvailable()).toBe(false);
  });

  it('flags a replacement worker and activates it on request', async () => {
    const { registration, regListeners } = setup({});
    const service = TestBed.inject(PwaUpdateService);
    service.init(true);
    await vi.waitFor(() => expect(Object.keys(regListeners)).toContain('updatefound'));
    const worker = fakeWorker();
    registration.installing = worker;
    registration.waiting = worker;
    regListeners['updatefound']();
    worker.fire('statechange');
    expect(service.updateAvailable()).toBe(true);

    service.applyUpdate();
    expect(worker.postMessage).toHaveBeenCalledWith({ type: 'SKIP_WAITING' });
  });
});
