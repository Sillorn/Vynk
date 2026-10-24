import { Route, Routes } from '@angular/router';

const placeholderPage = (
  path: string,
  title: string,
  heading: string,
  description: string,
): Route => ({
  path,
  title,
  data: { heading, description },
  loadComponent: () =>
    import('./shared/placeholder-page/placeholder-page').then((m) => m.PlaceholderPage),
});

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'feed' },
  placeholderPage(
    'feed',
    'Feed',
    'Your professional feed',
    'Follow updates from the Vynk community. Public posts and a connections-only view will appear here.',
  ),
  placeholderPage(
    'people',
    'People',
    'Find people',
    'Search public profiles by name, headline or skills and start building your professional network.',
  ),
  placeholderPage(
    'network',
    'My network',
    'Your network',
    'Review invitations, accepted connections and the people you have met on Vynk.',
  ),
  placeholderPage(
    'messages',
    'Messages',
    'Private messages',
    'Hold focused conversations with your accepted connections. Message history stays private to both participants.',
  ),
  placeholderPage(
    'notifications',
    'Notifications',
    'Notifications',
    'Connection invitations, acceptance updates and unread message counts will collect here.',
  ),
  placeholderPage(
    'settings',
    'Settings',
    'Account settings',
    'Change your password, sign out of this session or delete your account.',
  ),
  placeholderPage(
    'login',
    'Sign in',
    'Welcome back',
    'Sign in with your username and password to continue.',
  ),
  placeholderPage(
    'register',
    'Create account',
    'Create your account',
    'Build a professional profile with your name, headline and a short summary.',
  ),
  {
    path: 'profile/:username',
    title: 'Profile',
    data: {
      heading: 'Professional profile',
      description:
        'Public profile details, experience, education, skills and a downloadable CV will appear here.',
    },
    loadComponent: () =>
      import('./shared/placeholder-page/placeholder-page').then((m) => m.PlaceholderPage),
  },
  {
    path: '**',
    title: 'Page not found',
    loadComponent: () =>
      import('./shared/not-found-page/not-found-page').then((m) => m.NotFoundPage),
  },
];
