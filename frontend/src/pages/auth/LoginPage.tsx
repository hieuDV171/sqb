import React, { useState } from "react";
import { useAuth } from '@/hooks/useAuth';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DevicePlatform } from "@/types/auth.types";
import { getBrowserDeviceName } from "@/lib/utils";
import { GraduationCap, ArrowRight, AlertCircle, Eye, EyeOff, Info, HelpCircle, X } from "lucide-react";

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
    const [showHelpModal, setShowHelpModal] = useState(false);
    const { login, isLoading, error } = useAuth();

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        login({
            email: email.trim(),
            password,
            deviceId: getOrCreateDeviceId(),
            platform: DevicePlatform.WEB,
            deviceName: getBrowserDeviceName(),
        });
    };

    return (
        <div className="flex min-h-screen items-center justify-center bg-slate-50 dark:bg-slate-950 p-4 font-sans transition-colors duration-200">
            <Card className="w-full max-w-md bg-white dark:bg-slate-900 shadow-xl border border-slate-200/80 dark:border-slate-800 rounded-2xl overflow-hidden">
                <CardHeader className="space-y-2 text-center pt-8 pb-4">
                    <div className="w-12 h-12 rounded-2xl bg-indigo-600 flex items-center justify-center text-white mx-auto shadow-md shadow-indigo-200 dark:shadow-none">
                        <GraduationCap className="w-7 h-7" />
                    </div>
                    <CardTitle className="text-2xl font-bold text-slate-900 dark:text-slate-100">
                        Đăng nhập SQB Network
                    </CardTitle>
                    <CardDescription className="text-sm text-slate-500 dark:text-slate-400">
                        Mạng xã hội & Ngân hàng đề thi Đại học HUST
                    </CardDescription>
                </CardHeader>
                <CardContent className="p-6 pt-2">
                    <form onSubmit={handleSubmit} className="space-y-4">
                        {error && (
                            <div className="flex items-center gap-2 rounded-xl bg-red-50 dark:bg-red-950/40 p-3 text-sm text-red-600 dark:text-red-400 border border-red-100 dark:border-red-900/50">
                                <AlertCircle className="w-4 h-4 shrink-0" />
                                <span>{error}</span>
                            </div>
                        )}

                        <div className="space-y-1.5">
                            <Label htmlFor="email" className="text-slate-700 dark:text-slate-200 text-sm font-medium">
                                Email sinh viên / giảng viên
                            </Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="a.bc123456@hust.edu.vn"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                className="w-full rounded-xl px-3.5 py-2.5 text-sm"
                            />
                        </div>

                        <div className="space-y-1.5">
                            <div className="flex items-center justify-between">
                                <Label htmlFor="password" className="text-slate-700 dark:text-slate-200 text-sm font-medium">
                                    Mật khẩu
                                </Label>
                                <button
                                    type="button"
                                    onClick={() => setShowHelpModal(true)}
                                    className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 cursor-pointer flex items-center gap-1"
                                >
                                    <HelpCircle className="w-3.5 h-3.5" />
                                    <span>Quên mật khẩu?</span>
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
                                    className="w-full rounded-xl px-3.5 py-2.5 pr-10 text-sm"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-3 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 cursor-pointer"
                                >
                                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        <Button
                            type="submit"
                            disabled={isLoading}
                            className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold text-sm transition-colors cursor-pointer disabled:opacity-50 mt-2 shadow-md shadow-indigo-100 dark:shadow-none"
                        >
                            {isLoading ? "Đang xác thực..." : "Đăng nhập"}
                            {!isLoading && <ArrowRight className="w-4 h-4" />}
                        </Button>
                    </form>

                    {/* Notice for Students & Lecturers */}
                    <div className="mt-4 p-3 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200/60 dark:border-slate-800 flex items-start gap-2.5 text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                        <Info className="w-4 h-4 text-indigo-500 shrink-0 mt-0.5" />
                        <div>
                            Tài khoản Sinh viên và Giảng viên được cấp tự động theo danh sách lớp học phần đầu kỳ.
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Modal Hướng dẫn Quên mật khẩu / Cấp tài khoản */}
            {showHelpModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
                    <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 max-w-md w-full shadow-2xl relative animate-in fade-in zoom-in-95 duration-200">
                        <button
                            onClick={() => setShowHelpModal(false)}
                            className="absolute right-4 top-4 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
                        >
                            <X className="w-5 h-5" />
                        </button>

                        <div className="flex items-center gap-2.5 text-indigo-600 dark:text-indigo-400 mb-3">
                            <div className="w-10 h-10 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 flex items-center justify-center">
                                <HelpCircle className="w-5 h-5" />
                            </div>
                            <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                                Quy trình cấp lại mật khẩu
                            </h3>
                        </div>

                        <div className="space-y-3 text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
                            <p>
                                Theo quy định bảo mật của hệ thống SQB Network:
                            </p>
                            <div className="bg-indigo-50/60 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/40 rounded-xl p-3.5 space-y-2 text-xs text-indigo-950 dark:text-indigo-200">
                                <p className="font-semibold text-indigo-900 dark:text-indigo-300">
                                    📌 Dành cho Sinh viên:
                                </p>
                                <p>
                                    Nếu bạn quên mật khẩu hoặc chưa được tạo tài khoản, vui lòng đăng ký trực tiếp với <strong>Giảng viên phụ trách lớp học phần</strong>.
                                </p>
                                <p>
                                    Giảng viên sẽ tổng hợp danh sách gửi Quản trị viên (Admin) để reset tài khoản về mật khẩu mặc định của hệ thống.
                                </p>
                            </div>
                            <p className="text-xs text-slate-500 dark:text-slate-400">
                                Sau khi nhận lại mật khẩu mặc định từ Giảng viên, bạn có thể đăng nhập và chủ động đổi mật khẩu mới trong phần cài đặt tài khoản.
                            </p>
                        </div>

                        <div className="mt-5 flex justify-end">
                            <Button
                                type="button"
                                onClick={() => setShowHelpModal(false)}
                                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-sm font-semibold"
                            >
                                Đã hiểu
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
