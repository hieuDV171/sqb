import { lazy } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/auth/LoginPage";
import { ForbiddenPage } from "./pages/error/ForbiddenPage";
import { NotFoundPage } from "./pages/error/NotFoundPage";
import { ProtectedRoute } from "./components/routing/ProtectedRoute";
import { PublicRoute } from "./components/routing/PublicRoute";
import { OnboardingRoute } from "./components/routing/OnboardingRoute";
import { AdminHostRoute } from "./components/routing/AdminHostRoute";
import { AccountSetupPage } from "./pages/auth/AccountSetupPage";
import { RoleGuard } from "./components/routing/RoleGuard";
import { GlobalToastContainer } from "./components/ui/GlobalToastContainer";
import { FeedbackButton } from "./features/feedback/components/FeedbackButton";
import { useAuthStore } from "./stores/useAuthStore";

const DeviceManagementPage = lazy(() => import("./pages/devices/DeviceManagementPage").then((module) => ({ default: module.DeviceManagementPage })));
const LeaderboardPage = lazy(() => import("./pages/leaderboard/LeaderboardPage").then((module) => ({ default: module.LeaderboardPage })));
const FeedRoute = lazy(() => import("./pages/feed/FeedPage").then((module) => ({ default: module.FeedRoute })));
const PostDetailPage = lazy(() => import("./pages/feed/PostDetailPage").then((module) => ({ default: module.PostDetailPage })));
const SessionsPage = lazy(() => import("./pages/session/SessionsPage").then((module) => ({ default: module.SessionsPage })));
const SubmissionDetailPage = lazy(() => import("./pages/session/SubmissionDetailPage").then((module) => ({ default: module.SubmissionDetailPage })));
const PracticeWorkspacePage = lazy(() => import("./pages/session/PracticeWorkspacePage").then((module) => ({ default: module.PracticeWorkspacePage })));
const PracticeCatalogPage = lazy(() => import("./pages/session/PracticeCatalogPage").then((module) => ({ default: module.PracticeCatalogPage })));
const MessagesPage = lazy(() => import("@/features/chat/pages/MessagesPage").then((module) => ({ default: module.MessagesPage })));
const ShopPage = lazy(() => import("./pages/shop/ShopPage").then((module) => ({ default: module.ShopPage })));
const ProfilePage = lazy(() => import("./pages/profile/ProfilePage").then((module) => ({ default: module.ProfilePage })));
const FriendsPage = lazy(() => import("./pages/friends/FriendsPage").then((module) => ({ default: module.FriendsPage })));
const BlockedUsersPage = lazy(() => import("./pages/profile/BlockedUsersPage").then((module) => ({ default: module.BlockedUsersPage })));
const PendingReviewsPage = lazy(() => import("./pages/lecturer/PendingReviewsPage").then((module) => ({ default: module.PendingReviewsPage })));
const ReviewWorkspacePage = lazy(() => import("./pages/lecturer/ReviewWorkspacePage").then((module) => ({ default: module.ReviewWorkspacePage })));
const LecturerQuestionsPage = lazy(() => import("./pages/lecturer/LecturerQuestionsPage").then((module) => ({ default: module.LecturerQuestionsPage })));
const LecturerExamsPage = lazy(() => import("./pages/lecturer/LecturerExamsPage").then((module) => ({ default: module.LecturerExamsPage })));
const CourseClassesPage = lazy(() => import("./pages/lecturer/CourseClassesPage").then((module) => ({ default: module.CourseClassesPage })));
const CourseClassDetailPage = lazy(() => import("./pages/lecturer/CourseClassDetailPage").then((module) => ({ default: module.CourseClassDetailPage })));
const AdminUsersPage = lazy(() => import("./pages/admin/AdminUsersPage").then((module) => ({ default: module.AdminUsersPage })));
const AdminReportsPage = lazy(() => import("./pages/admin/AdminReportsPage").then((module) => ({ default: module.AdminReportsPage })));
const AdminReportDetailPage = lazy(() => import("./pages/admin/AdminReportDetailPage").then((module) => ({ default: module.AdminReportDetailPage })));
const AdminSettingsPage = lazy(() => import("./pages/admin/AdminSettingsPage").then((module) => ({ default: module.AdminSettingsPage })));
const AdminLeaderboardsPage = lazy(() => import("./pages/admin/AdminLeaderboardsPage").then((module) => ({ default: module.AdminLeaderboardsPage })));
const AccountSettingsPage = lazy(() => import("./pages/settings/AccountSettingsPage").then((module) => ({ default: module.AccountSettingsPage })));
const AdminBadgesPage = lazy(() => import("./pages/admin/AdminBadgesPage").then((module) => ({ default: module.AdminBadgesPage })));
const NotificationsPage = lazy(() => import("./pages/notification/NotificationsPage").then((module) => ({ default: module.NotificationsPage })));
const SearchPage = lazy(() => import("./pages/search/SearchPage").then((module) => ({ default: module.SearchPage })));

export default function App() {
  const { isAuthenticated, user } = useAuthStore();

  const getDefaultRoute = () => {
    if (!isAuthenticated) return "/login";
    if (user?.role === "ADMIN") return "/admin/users";
    return "/feed";
  };

  return (
    <BrowserRouter>
      <GlobalToastContainer />
      <Routes>
        <Route element={<AdminHostRoute />}>
        {/* Public Routes (Không cho phép người đã login vào lại trang đăng nhập) */}
        <Route element={<PublicRoute />}>
          <Route path="/login" element={<LoginPage />} />
        </Route>

        <Route element={<OnboardingRoute />}>
          <Route path="/account-setup" element={<AccountSetupPage />} />
        </Route>

        {/* Error Pages */}
        <Route path="/403" element={<ForbiddenPage />} />

        {/* Protected Common Routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Navigate to={getDefaultRoute()} replace />} />
          <Route path="/feed" element={<FeedRoute />} />
          <Route path="/posts/:postId" element={<PostDetailPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/users/:userId" element={<ProfilePage />} />
          <Route path="/friends" element={<FriendsPage />} />
          <Route path="/blocked-users" element={<BlockedUsersPage />} />
          <Route path="/leaderboard" element={<LeaderboardPage />} />
          <Route path="/gamification" element={<LeaderboardPage />} />
          <Route path="/devices" element={<DeviceManagementPage />} />
          <Route path="/settings" element={<AccountSettingsPage />} />
          <Route path="/questions" element={<SessionsPage />} />
          <Route path="/questions/practice" element={<PracticeCatalogPage />} />
          <Route path="/sessions" element={<SessionsPage />} />
          <Route path="/sessions/propose" element={<SessionsPage />} />
          <Route path="/sessions/:sessionId" element={<SubmissionDetailPage />} />
          <Route path="/sessions/:sessionId/practice" element={<PracticeWorkspacePage />} />
          <Route path="/sessions/:sessionId/questions" element={<PracticeWorkspacePage />} />
          <Route path="/users/:userId/sessions" element={<Navigate to="/questions/practice" replace />} />
          <Route path="/messages" element={<MessagesPage />} />
          <Route path="/messages/:conversationId" element={<MessagesPage />} />
          <Route path="/shop" element={<ShopPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/search" element={<SearchPage />} />

          {/* Lecturer Restricted Routes */}
          <Route element={<RoleGuard allowedRoles={['LECTURER', 'ADMIN']} />}>
            <Route path="/lecturer/sessions" element={<PendingReviewsPage />} />
            <Route path="/lecturer/sessions/:sessionId" element={<ReviewWorkspacePage />} />
            <Route path="/lecturer/sessions/:sessionId/review" element={<ReviewWorkspacePage />} />
            <Route path="/lecturer/reviews" element={<PendingReviewsPage />} />
            <Route path="/lecturer/reviews/:sessionId" element={<ReviewWorkspacePage />} />
            <Route path="/lecturer/questions" element={<LecturerQuestionsPage />} />
            <Route path="/lecturer/exams" element={<LecturerExamsPage />} />
            <Route path="/lecturer/course-classes" element={<CourseClassesPage />} />
            <Route path="/lecturer/course-classes/:courseClassId" element={<CourseClassDetailPage />} />
          </Route>

          {/* Admin Restricted Routes */}
          <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
            <Route path="/admin" element={<Navigate to="/admin/users" replace />} />
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/badges" element={<AdminBadgesPage />} />
            <Route path="/admin/reports" element={<AdminReportsPage />} />
            <Route path="/admin/reports/:reportId" element={<AdminReportDetailPage />} />
            <Route path="/admin/settings" element={<AdminSettingsPage />} />
            <Route path="/admin/leaderboards" element={<AdminLeaderboardsPage />} />
          </Route>
        </Route>

        {/* 404 Catch-all */}
        <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
      <FeedbackButton />
    </BrowserRouter>
  );
}
