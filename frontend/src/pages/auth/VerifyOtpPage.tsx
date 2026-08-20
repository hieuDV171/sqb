import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@base-ui/react/button';
import { Input } from '@base-ui/react/input';
import { axiosClient } from '@/api/axiosClient';
import { Mail, CheckCircle2, AlertCircle, RefreshCw, ArrowLeft } from 'lucide-react';

export function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const [email, setEmail] = useState<string>(location.state?.email || '');
  const [otp, setOtp] = useState<string[]>(['', '', '', '', '', '']);
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(60);

  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    let timer: any;
    if (countdown > 0) {
      timer = setInterval(() => setCountdown((prev) => prev - 1), 1000);
    }
    return () => clearInterval(timer);
  }, [countdown]);

  const handleOtpChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);

    // Auto focus next input
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    const code = otp.join('');
    if (code.length < 6) {
      setError('Vui lòng nhập đủ 6 chữ số mã xác thực OTP');
      return;
    }

    setIsLoading(true);
    try {
      await axiosClient.post('/auth/admin/verify', {
        email,
        verifyCode: code,
      });

      setSuccessMsg('Xác thực tài khoản thành công! Đang chuyển hướng sang Đăng nhập...');
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err: any) {
      setError(err?.message || 'Mã xác thực không đúng hoặc đã hết hạn!');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    if (countdown > 0) return;
    setIsResending(true);
    setError(null);
    setSuccessMsg(null);

    try {
      await axiosClient.post('/auth/admin/resend-verify', { email });
      setSuccessMsg('Đã gửi lại mã OTP mới về Email của bạn!');
      setCountdown(60);
    } catch (err: any) {
      setError(err?.message || 'Gửi lại mã OTP thất bại!');
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4 font-sans">
      <Card className="w-full max-w-md bg-white shadow-xl border border-slate-200/80 rounded-2xl overflow-hidden">
        <CardHeader className="space-y-2 text-center pt-8 pb-4">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto border border-indigo-100 shadow-xs">
            <Mail className="w-6 h-6" />
          </div>
          <CardTitle className="text-2xl font-bold text-slate-900">
            Xác thực Email OTP
          </CardTitle>
          <CardDescription className="text-sm text-slate-500">
            Mã OTP 6 số đã được gửi tới email <span className="font-semibold text-slate-800">{email || 'của bạn'}</span>
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 pt-2">
          {error && (
            <div className="mb-4 flex items-center gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-600 border border-red-100">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {successMsg && (
            <div className="mb-4 flex items-center gap-2 rounded-xl bg-green-50 p-3 text-sm text-green-700 border border-green-100">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          <form onSubmit={handleVerify} className="space-y-6">
            {!email && (
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-600">Email của bạn</label>
                <Input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Nhập email sinh viên"
                  required
                  className="w-full border border-slate-200 rounded-xl px-3.5 py-2 text-sm"
                />
              </div>
            )}

            {/* OTP 6 digits input boxes */}
            <div className="flex justify-between gap-2 my-4">
              {otp.map((digit, index) => (
                <input
                  key={index}
                  ref={(el) => { inputRefs.current[index] = el; }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleOtpChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  className="w-12 h-14 text-center text-xl font-bold text-slate-900 border border-slate-300 rounded-xl focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/20 focus:outline-none transition-all"
                />
              ))}
            </div>

            <Button
              type="submit"
              disabled={isLoading || otp.join('').length < 6}
              className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold text-sm transition-colors cursor-pointer disabled:opacity-50 shadow-md shadow-indigo-100"
            >
              {isLoading ? 'Đang xác thực...' : 'Xác thực tài khoản'}
            </Button>
          </form>

          {/* Resend OTP */}
          <div className="mt-6 flex items-center justify-between text-sm text-slate-500 border-t border-slate-100 pt-4">
            <Link to="/login" className="flex items-center gap-1 text-slate-600 hover:text-slate-900 font-medium">
              <ArrowLeft className="w-4 h-4" /> Quay lại Đăng nhập
            </Link>

            <button
              onClick={handleResend}
              disabled={countdown > 0 || isResending}
              type="button"
              className="font-semibold text-indigo-600 hover:text-indigo-700 disabled:text-slate-400 cursor-pointer disabled:cursor-not-allowed flex items-center gap-1"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isResending ? 'animate-spin' : ''}`} />
              {countdown > 0 ? `Gửi lại mã (${countdown}s)` : 'Gửi lại mã OTP'}
            </button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
