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
  {
    path: 'settings',
    title: 'Settings',
    loadComponent: () =>
      import('./features/settings/settings-page/settings-page').then((m) => m.SettingsPage),
  },
  {
    path: 'login',
    title: 'Sign in',
    loadComponent: () => import('./features/auth/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    title: 'Create account',
    loadComponent: () =>
      import('./features/auth/register-page/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'profile/:username',
    title: 'Profile',
    loadComponent: () =>
      import('./features/profiles/public-profile-page/public-profile-page').then(
        (m) => m.PublicProfilePage,
      ),
  },
  {
    path: '**',
    title: 'Page not found',
    loadComponent: () =>
      import('./shared/not-found-page/not-found-page').then((m) => m.NotFoundPage),
  },
];
