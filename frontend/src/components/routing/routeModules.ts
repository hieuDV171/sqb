import { matchPath } from 'react-router-dom';

// Shared by React.lazy and startup warming so both request the same chunks.
export const routeModules = {
  AppLayout: () => import('@/layouts/AppLayout').then((module) => ({ default: module.AppLayout })),
  LoginPage: () => import('@/pages/auth/LoginPage').then((module) => ({ default: module.LoginPage })),
  AccountSetupPage: () => import('@/pages/auth/AccountSetupPage').then((module) => ({ default: module.AccountSetupPage })),
  DeviceManagementPage: () => import('@/pages/devices/DeviceManagementPage').then((module) => ({ default: module.DeviceManagementPage })),
  LeaderboardPage: () => import('@/pages/leaderboard/LeaderboardPage').then((module) => ({ default: module.LeaderboardPage })),
  FeedRoute: () => import('@/pages/feed/FeedPage').then((module) => ({ default: module.FeedRoute })),
  PostDetailPage: () => import('@/pages/feed/PostDetailPage').then((module) => ({ default: module.PostDetailPage })),
  SessionsPage: () => import('@/pages/session/SessionsPage').then((module) => ({ default: module.SessionsPage })),
  SubmissionDetailPage: () => import('@/pages/session/SubmissionDetailPage').then((module) => ({ default: module.SubmissionDetailPage })),
  PracticeWorkspacePage: () => import('@/pages/session/PracticeWorkspacePage').then((module) => ({ default: module.PracticeWorkspacePage })),
  PracticeCatalogPage: () => import('@/pages/session/PracticeCatalogPage').then((module) => ({ default: module.PracticeCatalogPage })),
  MessagesPage: () => import('@/features/chat/pages/MessagesPage').then((module) => ({ default: module.MessagesPage })),
  ShopPage: () => import('@/pages/shop/ShopPage').then((module) => ({ default: module.ShopPage })),
  ProfilePage: () => import('@/pages/profile/ProfilePage').then((module) => ({ default: module.ProfilePage })),
  FriendsPage: () => import('@/pages/friends/FriendsPage').then((module) => ({ default: module.FriendsPage })),
  BlockedUsersPage: () => import('@/pages/profile/BlockedUsersPage').then((module) => ({ default: module.BlockedUsersPage })),
  PendingReviewsPage: () => import('@/pages/lecturer/PendingReviewsPage').then((module) => ({ default: module.PendingReviewsPage })),
  ReviewWorkspacePage: () => import('@/pages/lecturer/ReviewWorkspacePage').then((module) => ({ default: module.ReviewWorkspacePage })),
  LecturerQuestionsPage: () => import('@/pages/lecturer/LecturerQuestionsPage').then((module) => ({ default: module.LecturerQuestionsPage })),
  LecturerExamsPage: () => import('@/pages/lecturer/LecturerExamsPage').then((module) => ({ default: module.LecturerExamsPage })),
  CourseClassesPage: () => import('@/pages/lecturer/CourseClassesPage').then((module) => ({ default: module.CourseClassesPage })),
  CourseClassDetailPage: () => import('@/pages/lecturer/CourseClassDetailPage').then((module) => ({ default: module.CourseClassDetailPage })),
  AdminUsersPage: () => import('@/pages/admin/AdminUsersPage').then((module) => ({ default: module.AdminUsersPage })),
  AdminReportsPage: () => import('@/pages/admin/AdminReportsPage').then((module) => ({ default: module.AdminReportsPage })),
  AdminReportDetailPage: () => import('@/pages/admin/AdminReportDetailPage').then((module) => ({ default: module.AdminReportDetailPage })),
  AdminSettingsPage: () => import('@/pages/admin/AdminSettingsPage').then((module) => ({ default: module.AdminSettingsPage })),
  AdminLeaderboardsPage: () => import('@/pages/admin/AdminLeaderboardsPage').then((module) => ({ default: module.AdminLeaderboardsPage })),
  AccountSettingsPage: () => import('@/pages/settings/AccountSettingsPage').then((module) => ({ default: module.AccountSettingsPage })),
  AdminBadgesPage: () => import('@/pages/admin/AdminBadgesPage').then((module) => ({ default: module.AdminBadgesPage })),
  NotificationsPage: () => import('@/pages/notification/NotificationsPage').then((module) => ({ default: module.NotificationsPage })),
  SearchPage: () => import('@/pages/search/SearchPage').then((module) => ({ default: module.SearchPage })),
} as const;

const startupRoutes: [string, keyof typeof routeModules][] = [
  ['/login', 'LoginPage'],
  ['/account-setup', 'AccountSetupPage'],
  ['/feed', 'FeedRoute'],
  ['/posts/:postId', 'PostDetailPage'],
  ['/profile', 'ProfilePage'],
  ['/users/:userId', 'ProfilePage'],
  ['/friends', 'FriendsPage'],
  ['/blocked-users', 'BlockedUsersPage'],
  ['/leaderboard', 'LeaderboardPage'],
  ['/gamification', 'LeaderboardPage'],
  ['/devices', 'DeviceManagementPage'],
  ['/settings', 'AccountSettingsPage'],
  ['/questions', 'SessionsPage'],
  ['/questions/practice', 'PracticeCatalogPage'],
  ['/sessions', 'SessionsPage'],
  ['/sessions/propose', 'SessionsPage'],
  ['/sessions/:sessionId', 'SubmissionDetailPage'],
  ['/sessions/:sessionId/practice', 'PracticeWorkspacePage'],
  ['/sessions/:sessionId/questions', 'PracticeWorkspacePage'],
  ['/messages', 'MessagesPage'],
  ['/messages/:conversationId', 'MessagesPage'],
  ['/shop', 'ShopPage'],
  ['/notifications', 'NotificationsPage'],
  ['/search', 'SearchPage'],
  ['/lecturer/sessions', 'PendingReviewsPage'],
  ['/lecturer/sessions/:sessionId', 'ReviewWorkspacePage'],
  ['/lecturer/sessions/:sessionId/review', 'ReviewWorkspacePage'],
  ['/lecturer/reviews', 'PendingReviewsPage'],
  ['/lecturer/reviews/:sessionId', 'ReviewWorkspacePage'],
  ['/lecturer/questions', 'LecturerQuestionsPage'],
  ['/lecturer/exams', 'LecturerExamsPage'],
  ['/lecturer/course-classes', 'CourseClassesPage'],
  ['/lecturer/course-classes/:courseClassId', 'CourseClassDetailPage'],
  ['/admin/users', 'AdminUsersPage'],
  ['/admin/badges', 'AdminBadgesPage'],
  ['/admin/reports', 'AdminReportsPage'],
  ['/admin/reports/:reportId', 'AdminReportDetailPage'],
  ['/admin/settings', 'AdminSettingsPage'],
  ['/admin/leaderboards', 'AdminLeaderboardsPage'],

];

export async function warmInitialRoute(pathname: string): Promise<void> {
  const route = startupRoutes.find(([path]) => matchPath(path, pathname));
  const isPublic = route?.[1] === 'LoginPage' || route?.[1] === 'AccountSetupPage';
  // Imports only: guards still wait for the validated cookie session/profile.
  await Promise.all([
    ...(!isPublic ? [routeModules.AppLayout()] : []),
    ...(route ? [routeModules[route[1]]()] : []),
  ]);
}
