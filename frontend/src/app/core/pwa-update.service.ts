import { Injectable, isDevMode, signal } from '@angular/core';

/**
 * Registers the service worker (production, secure contexts only) and exposes an
 * `updateAvailable` signal when a new version is installed and waiting. The user decides
 * when to apply it so in-progress work (drafts, forms) is never reloaded unexpectedly.
 */
@Injectable({ providedIn: 'root' })
export class PwaUpdateService {
  readonly updateAvailable = signal(false);
  private registration: ServiceWorkerRegistration | null = null;
  private reloading = false;

  /** `enabled` defaults to production builds only; tests override it. */
  init(enabled = !isDevMode()): void {
    if (
      !enabled ||
      typeof window === 'undefined' ||
      !('serviceWorker' in navigator) ||
      !window.isSecureContext
    ) {
      return;
    }
    const start = () =>
      navigator.serviceWorker
        .register('/sw.js')
        .then((registration) => this.watch(registration))
        .catch((error: unknown) => {
          console.warn('Vynk offline shell could not be installed.', error);
        });
    if (document.readyState === 'complete') void start();
    else window.addEventListener('load', () => void start(), { once: true });

    navigator.serviceWorker.addEventListener('controllerchange', () => {
      if (this.reloading) window.location.reload();
    });
    // Look for new versions when the tab becomes visible again.
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') void this.registration?.update().catch(() => {});
    });
  }

  /** Activates the waiting worker; the page reloads once it takes control. */
  applyUpdate(): void {
    const waiting = this.registration?.waiting;
    if (!waiting) return;
    this.reloading = true;
    waiting.postMessage({ type: 'SKIP_WAITING' });
  }

  private watch(registration: ServiceWorkerRegistration): void {
    this.registration = registration;
    const flag = (worker: ServiceWorker | null) => {
      // Only a *replacement* is an update; the first install has no controller.
      if (worker && navigator.serviceWorker.controller) this.updateAvailable.set(true);
    };
    flag(registration.waiting);
    registration.addEventListener('updatefound', () => {
      const installing = registration.installing;
      installing?.addEventListener('statechange', () => {
        if (installing.state === 'installed') flag(installing);
      });
    });
  }
}
