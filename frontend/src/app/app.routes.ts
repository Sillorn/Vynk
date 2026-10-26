import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'feed' },
  {
    path: 'feed',
    title: 'Feed',
    loadComponent: () => import('./features/social/feed-page/feed-page').then((m) => m.FeedPage),
  },
  {
    path: 'people',
    title: 'People',
    loadComponent: () =>
      import('./features/profiles/people-directory/people-directory').then(
        (m) => m.PeopleDirectory,
      ),
  },
  {
    path: 'network',
    title: 'My network',
    loadComponent: () =>
      import('./features/social/network-page/network-page').then((m) => m.NetworkPage),
  },
  {
    path: 'messages',
    title: 'Messages',
    loadComponent: () =>
      import('./features/social/messages-page/messages-page').then((m) => m.MessagesPage),
  },
  {
    path: 'notifications',
    title: 'Notifications',
    loadComponent: () =>
      import('./features/social/notifications-page/notifications-page').then(
        (m) => m.NotificationsPage,
      ),
  },
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
    path: 'profile/edit',
    title: 'Edit profile',
    loadComponent: () =>
      import('./features/profiles/profile-editor/profile-editor').then((m) => m.ProfileEditor),
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
