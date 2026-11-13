import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/auth/LoginPage";
import { DeviceManagementPage } from "./pages/devices/DeviceManagementPage";
import { LeaderboardPage } from "./pages/leaderboard/LeaderboardPage";
import { FeedRoute } from "./pages/feed/FeedPage";
import { PostDetailPage } from "./pages/feed/PostDetailPage";
import { SessionsPage } from "./pages/session/SessionsPage";
import { SubmissionDetailPage } from "./pages/session/SubmissionDetailPage";
import { PracticeWorkspacePage } from "./pages/session/PracticeWorkspacePage";
import { PracticeCatalogPage } from "./pages/session/PracticeCatalogPage";
import { MessagesPage } from "@/features/chat";
import { ShopPage } from "./pages/shop/ShopPage";
import { ProfilePage } from "./pages/profile/ProfilePage";
import { FriendsPage } from "./pages/friends/FriendsPage";
import { BlockedUsersPage } from "./pages/profile/BlockedUsersPage";
import { PendingReviewsPage } from "./pages/lecturer/PendingReviewsPage";
import { ReviewWorkspacePage } from "./pages/lecturer/ReviewWorkspacePage";
import { LecturerQuestionsPage } from "./pages/lecturer/LecturerQuestionsPage";
import { LecturerExamsPage } from "./pages/lecturer/LecturerExamsPage";
import { CourseClassesPage } from "./pages/lecturer/CourseClassesPage";
import { CourseClassDetailPage } from "./pages/lecturer/CourseClassDetailPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";
import { AdminReportsPage } from "./pages/admin/AdminReportsPage";
import { AdminSettingsPage } from "./pages/admin/AdminSettingsPage";
import { AdminLeaderboardsPage } from "./pages/admin/AdminLeaderboardsPage";
import { AccountSettingsPage } from "./pages/settings/AccountSettingsPage";
import { AdminBadgesPage } from "./pages/admin/AdminBadgesPage";
import { NotificationsPage } from "./pages/notification/NotificationsPage";
import { SearchPage } from "./pages/search/SearchPage";
import { ForbiddenPage } from "./pages/error/ForbiddenPage";
import { NotFoundPage } from "./pages/error/NotFoundPage";
import { ProtectedRoute } from "./components/routing/ProtectedRoute";
import { PublicRoute } from "./components/routing/PublicRoute";
import { OnboardingRoute } from "./components/routing/OnboardingRoute";
import { AccountSetupPage } from "./pages/auth/AccountSetupPage";
import { RoleGuard } from "./components/routing/RoleGuard";
import { GlobalToastContainer } from "./components/ui/GlobalToastContainer";
import { useAuthStore } from "./stores/useAuthStore";

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
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/badges" element={<AdminBadgesPage />} />
            <Route path="/admin/reports" element={<AdminReportsPage />} />
            <Route path="/admin/settings" element={<AdminSettingsPage />} />
            <Route path="/admin/leaderboards" element={<AdminLeaderboardsPage />} />
          </Route>
        </Route>

        {/* 404 Catch-all */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
