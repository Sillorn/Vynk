import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App shell', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('creates the application shell', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('exposes a skip link that targets the main landmark', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const skipLink = element.querySelector<HTMLAnchorElement>('.skip-link');
    const main = element.querySelector<HTMLElement>('#main-content');

    expect(skipLink?.textContent).toContain('Skip to main content');
    expect(skipLink?.getAttribute('href')).toBe('#main-content');
    expect(main?.tagName.toLowerCase()).toBe('main');
  });

  it('renders the brand and primary navigation', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const nav = element.querySelector<HTMLElement>('nav[aria-label="Primary"]');
    const links = Array.from(nav?.querySelectorAll<HTMLAnchorElement>('a') ?? []);

    expect(element.querySelector('.brand-name')?.textContent).toContain('Pro Network');
    expect(links.map((link) => link.textContent?.trim())).toEqual([
      'Feed',
      'People',
      'My network',
      'Messages',
      'Notifications',
    ]);
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/feed',
      '/people',
      '/network',
      '/messages',
      '/notifications',
    ]);
  });

  it('offers sign-in and registration actions', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const actions = Array.from(element.querySelectorAll<HTMLAnchorElement>('.toolbar-actions a'));

    expect(actions.map((action) => action.textContent?.trim())).toEqual([
      'Sign in',
      'Create account',
    ]);
  });
});
