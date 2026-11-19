import { lazy } from "react";
import { routeModules } from "./components/routing/routeModules";
import { RouteContent } from "./components/routing/RouteContent";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ForbiddenPage } from "./pages/error/ForbiddenPage";
import { NotFoundPage } from "./pages/error/NotFoundPage";
import { ProtectedRoute } from "./components/routing/ProtectedRoute";
import { PublicRoute } from "./components/routing/PublicRoute";
import { OnboardingRoute } from "./components/routing/OnboardingRoute";
import { AdminHostRoute } from "./components/routing/AdminHostRoute";
import { RoleGuard } from "./components/routing/RoleGuard";
import { GlobalToastContainer } from "./components/ui/GlobalToastContainer";
import { FeedbackButton } from "./features/feedback/components/FeedbackButton";
import { useAuthStore } from "./stores/useAuthStore";

const LoginPage = lazy(routeModules.LoginPage);
const AccountSetupPage = lazy(routeModules.AccountSetupPage);
const DeviceManagementPage = lazy(routeModules.DeviceManagementPage);
const LeaderboardPage = lazy(routeModules.LeaderboardPage);
const FeedRoute = lazy(routeModules.FeedRoute);
const PostDetailPage = lazy(routeModules.PostDetailPage);
const SessionsPage = lazy(routeModules.SessionsPage);
const SubmissionDetailPage = lazy(routeModules.SubmissionDetailPage);
const PracticeWorkspacePage = lazy(routeModules.PracticeWorkspacePage);
const PracticeCatalogPage = lazy(routeModules.PracticeCatalogPage);
const MessagesPage = lazy(routeModules.MessagesPage);
const ShopPage = lazy(routeModules.ShopPage);
const ProfilePage = lazy(routeModules.ProfilePage);
const FriendsPage = lazy(routeModules.FriendsPage);
const BlockedUsersPage = lazy(routeModules.BlockedUsersPage);
const PendingReviewsPage = lazy(routeModules.PendingReviewsPage);
const ReviewWorkspacePage = lazy(routeModules.ReviewWorkspacePage);
const LecturerQuestionsPage = lazy(routeModules.LecturerQuestionsPage);
const LecturerExamsPage = lazy(routeModules.LecturerExamsPage);
const CourseClassesPage = lazy(routeModules.CourseClassesPage);
const CourseClassDetailPage = lazy(routeModules.CourseClassDetailPage);
const AdminUsersPage = lazy(routeModules.AdminUsersPage);
const AdminReportsPage = lazy(routeModules.AdminReportsPage);
const AdminReportDetailPage = lazy(routeModules.AdminReportDetailPage);
const AdminSettingsPage = lazy(routeModules.AdminSettingsPage);
const AdminLeaderboardsPage = lazy(routeModules.AdminLeaderboardsPage);
const AccountSettingsPage = lazy(routeModules.AccountSettingsPage);
const AdminBadgesPage = lazy(routeModules.AdminBadgesPage);
const NotificationsPage = lazy(routeModules.NotificationsPage);
const SearchPage = lazy(routeModules.SearchPage);

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
          <Route path="/login" element={<RouteContent routeKey="login"><LoginPage /></RouteContent>} />
        </Route>

        <Route element={<OnboardingRoute />}>
          <Route path="/account-setup" element={<RouteContent routeKey="account-setup"><AccountSetupPage /></RouteContent>} />
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
