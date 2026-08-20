import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { VerifyOtpPage } from "./pages/auth/VerifyOtpPage";
import { DeviceManagementPage } from "./pages/devices/DeviceManagementPage";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { useAuthStore } from "./stores/useAuthStore";

export default function App() {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    return (
        <BrowserRouter>
            <Routes>
                {/* Public Auth Routes */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/verify-otp" element={<VerifyOtpPage />} />

                {/* Protected Routes */}
                <Route element={<ProtectedRoute />}>
                    <Route path="/devices" element={<DeviceManagementPage />} />
                </Route>

                {/* Fallback Catch-all Route */}
                <Route
                    path="*"
                    element={
                        <Navigate
                            to={isAuthenticated ? "/devices" : "/login"}
                            replace
                        />
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}
