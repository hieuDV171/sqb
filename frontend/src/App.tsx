import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/auth/LoginPage";
import { DeviceManagementPage } from "./pages/devices/DeviceManagementPage";
import { LeaderboardPage } from "./pages/leaderboard/LeaderboardPage";
import { FeedPage } from "./pages/feed/FeedPage";
import { SessionsPage } from "./pages/session/SessionsPage";
import { SubmissionDetailPage } from "./pages/session/SubmissionDetailPage";
import { MessagesPage } from "./pages/chat/MessagesPage";
import { ShopPage } from "./pages/shop/ShopPage";
import { ProfilePage } from "./pages/profile/ProfilePage";
import { FriendsPage } from "./pages/friends/FriendsPage";
import { BlockedUsersPage } from "./pages/profile/BlockedUsersPage";
import { PendingReviewsPage } from "./pages/lecturer/PendingReviewsPage";
import { ReviewWorkspacePage } from "./pages/lecturer/ReviewWorkspacePage";
import { LecturerQuestionsPage } from "./pages/lecturer/LecturerQuestionsPage";
import { LecturerExamsPage } from "./pages/lecturer/LecturerExamsPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";
import { AdminReportsPage } from "./pages/admin/AdminReportsPage";
import { AdminSettingsPage } from "./pages/admin/AdminSettingsPage";
import { ForbiddenPage } from "./pages/error/ForbiddenPage";
import { NotFoundPage } from "./pages/error/NotFoundPage";
import { ProtectedRoute } from "./components/routing/ProtectedRoute";
import { PublicRoute } from "./components/routing/PublicRoute";
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

        {/* Error Pages */}
        <Route path="/403" element={<ForbiddenPage />} />

        {/* Protected Common Routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Navigate to={getDefaultRoute()} replace />} />
          <Route path="/feed" element={<FeedPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/users/:userId" element={<ProfilePage />} />
          <Route path="/friends" element={<FriendsPage />} />
          <Route path="/blocked-users" element={<BlockedUsersPage />} />
          <Route path="/leaderboard" element={<LeaderboardPage />} />
          <Route path="/devices" element={<DeviceManagementPage />} />
          <Route path="/questions" element={<SessionsPage />} />
          <Route path="/sessions" element={<SessionsPage />} />
          <Route path="/sessions/propose" element={<SessionsPage />} />
          <Route path="/sessions/:sessionId" element={<SubmissionDetailPage />} />
          <Route path="/messages" element={<MessagesPage />} />
          <Route path="/shop" element={<ShopPage />} />

          {/* Lecturer Restricted Routes */}
          <Route element={<RoleGuard allowedRoles={['LECTURER', 'ADMIN']} />}>
            <Route path="/lecturer/sessions" element={<PendingReviewsPage />} />
            <Route path="/lecturer/sessions/:sessionId" element={<ReviewWorkspacePage />} />
            <Route path="/lecturer/sessions/:sessionId/review" element={<ReviewWorkspacePage />} />
            <Route path="/lecturer/reviews" element={<PendingReviewsPage />} />
            <Route path="/lecturer/reviews/:sessionId" element={<ReviewWorkspacePage />} />
            <Route path="/lecturer/questions" element={<LecturerQuestionsPage />} />
            <Route path="/lecturer/exams" element={<LecturerExamsPage />} />
          </Route>

          {/* Admin Restricted Routes */}
          <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/reports" element={<AdminReportsPage />} />
            <Route path="/admin/settings" element={<AdminSettingsPage />} />
          </Route>
        </Route>

        {/* 404 Catch-all */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
