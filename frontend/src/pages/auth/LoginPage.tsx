import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from '@/hooks/useAuth';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Button } from "@base-ui/react/button";
import { Input } from "@base-ui/react/input";
import { DevicePlatform } from "@/types/auth.types";
import { getBrowserDeviceName } from "@/lib/utils";
import { axiosClient } from "@/api/axiosClient";
import { GraduationCap, ArrowRight, AlertCircle, Eye, EyeOff, KeyRound, CheckCircle2, X } from "lucide-react";

function getOrCreateDeviceId() {
    let deviceId = localStorage.getItem("sqb_device_id");
    if (!deviceId) {
        deviceId = "web-browser-" + crypto.randomUUID();
        localStorage.setItem("sqb_device_id", deviceId);
    }
    return deviceId;
}

export function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const { login, isLoading, error } = useAuth();

    // Reset Password State
    const [isResetOpen, setIsResetOpen] = useState(false);
    const [resetEmail, setResetEmail] = useState("");
    const [newResetPassword, setNewResetPassword] = useState("");
    const [resetLoading, setResetLoading] = useState(false);
    const [resetError, setResetError] = useState<string | null>(null);
    const [resetSuccess, setResetSuccess] = useState(false);

    const handleSubmit = (e: React.SubmitEvent) => {
        e.preventDefault();
        login({
            email,
            password,
            deviceId: getOrCreateDeviceId(),
            platform: DevicePlatform.WEB,
            deviceName: getBrowserDeviceName(),
        });
    };

    const handleResetPassword = async (e: React.SubmitEvent) => {
        e.preventDefault();
        setResetError(null);
        setResetSuccess(false);

        if (newResetPassword.length < 8) {
            setResetError("Mật khẩu mới phải từ 8 ký tự trở lên");
            return;
        }

        setResetLoading(true);
        try {
            await axiosClient.post('/auth/admin/reset-password', {
                email: resetEmail,
                newPassword: newResetPassword,
            });
            setResetSuccess(true);
            setTimeout(() => {
                setIsResetOpen(false);
                setResetSuccess(false);
                setEmail(resetEmail);
            }, 1800);
        } catch (err: any) {
            setResetError(err?.message || "Đặt lại mật khẩu thất bại. Vui lòng kiểm tra lại email!");
        } finally {
            setResetLoading(false);
        }
    };
    
    return (
        <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4 font-sans">
            <Card className="w-full max-w-md bg-white shadow-xl border border-slate-200/80 rounded-2xl overflow-hidden">
                <CardHeader className="space-y-2 text-center pt-8 pb-4">
                    <div className="w-12 h-12 rounded-2xl bg-indigo-600 flex items-center justify-center text-white mx-auto shadow-md shadow-indigo-200">
                        <GraduationCap className="w-7 h-7" />
                    </div>
                    <CardTitle className="text-2xl font-bold text-slate-900">
                        Đăng nhập SQB Network
                    </CardTitle>
                    <CardDescription className="text-sm text-slate-500">
                        Mạng xã hội & Ngân hàng đề thi Đại học HUST
                    </CardDescription>
                </CardHeader>
                <CardContent className="p-6 pt-2">
                    <form onSubmit={handleSubmit} className="space-y-4">
                        {error && (
                            <div className="flex items-center gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-600 border border-red-100">
                                <AlertCircle className="w-4 h-4 shrink-0" />
                                <span>{error}</span>
                            </div>
                        )}

                        <div className="space-y-1.5">
                            <Label htmlFor="email">Email sinh viên / giảng viên</Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="a.bc123456@hust.edu.vn"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                className="w-full border border-slate-200 rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                            />
                        </div>

                        <div className="space-y-1.5">
                            <div className="flex items-center justify-between">
                                <Label htmlFor="password">Mật khẩu</Label>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setResetEmail(email);
                                        setIsResetOpen(true);
                                    }}
                                    className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 cursor-pointer"
                                >
                                    Quên mật khẩu?
                                </button>
                            </div>
                            <div className="relative">
                                <Input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    placeholder="••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-3.5 py-2.5 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-3 text-slate-400 hover:text-slate-600"
                                >
                                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        <Button
                            type="submit"
                            disabled={isLoading}
                            className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold text-sm transition-colors cursor-pointer disabled:opacity-50 mt-2 shadow-md shadow-indigo-100"
                        >
                            {isLoading ? "Đang xác thực..." : "Đăng nhập"}
                            {!isLoading && <ArrowRight className="w-4 h-4" />}
                        </Button>
                    </form>

                    <div className="mt-6 text-center text-sm text-slate-500 border-t border-slate-100 pt-4">
                        Chưa có tài khoản?{" "}
                        <Link to="/register" className="font-semibold text-indigo-600 hover:text-indigo-700">
                            Đăng ký ngay
                        </Link>
                    </div>
                </CardContent>
            </Card>

            {/* Modal Quên / Đặt lại mật khẩu */}
            {isResetOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
                    <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl relative animate-in fade-in zoom-in-95 duration-200">
                        <button
                            onClick={() => setIsResetOpen(false)}
                            className="absolute right-4 top-4 text-slate-400 hover:text-slate-600 p-1 rounded-full hover:bg-slate-100"
                        >
                            <X className="w-5 h-5" />
                        </button>

                        <div className="flex items-center gap-2 text-indigo-600 mb-2">
                            <KeyRound className="w-6 h-6" />
                            <h3 className="text-xl font-bold text-slate-900">Đặt lại mật khẩu</h3>
                        </div>
                        <p className="text-xs text-slate-500 mb-4">
                            Nhập email tài khoản và mật khẩu mới của bạn để khôi phục truy cập.
                        </p>

                        {resetError && (
                            <div className="mb-4 flex items-center gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-600">
                                <AlertCircle className="w-4 h-4 shrink-0" />
                                <span>{resetError}</span>
                            </div>
                        )}

                        {resetSuccess && (
                            <div className="mb-4 flex items-center gap-2 rounded-xl bg-green-50 p-3 text-sm text-green-700">
                                <CheckCircle2 className="w-4 h-4 shrink-0" />
                                <span>Đặt lại mật khẩu thành công!</span>
                            </div>
                        )}

                        <form onSubmit={handleResetPassword} className="space-y-4">
                            <div className="space-y-1">
                                <Label htmlFor="resetEmail">Email sinh viên</Label>
                                <Input
                                    id="resetEmail"
                                    type="email"
                                    placeholder="a.bc123456@hust.edu.vn"
                                    value={resetEmail}
                                    onChange={(e) => setResetEmail(e.target.value)}
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-3.5 py-2 text-sm"
                                />
                            </div>

                            <div className="space-y-1">
                                <Label htmlFor="newResetPassword">Mật khẩu mới</Label>
                                <Input
                                    id="newResetPassword"
                                    type="password"
                                    placeholder="Tối thiểu 8 ký tự"
                                    value={newResetPassword}
                                    onChange={(e) => setNewResetPassword(e.target.value)}
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-3.5 py-2 text-sm"
                                />
                            </div>

                            <div className="flex justify-end gap-2 pt-2">
                                <Button
                                    type="button"
                                    onClick={() => setIsResetOpen(false)}
                                    className="px-4 py-2 border rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-100 cursor-pointer"
                                >
                                    Hủy
                                </Button>
                                <Button
                                    type="submit"
                                    disabled={resetLoading}
                                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-sm font-medium cursor-pointer disabled:opacity-50"
                                >
                                    {resetLoading ? "Đang đặt lại..." : "Xác nhận đặt lại"}
                                </Button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
