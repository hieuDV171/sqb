import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@base-ui/react/button';
import { Input } from '@base-ui/react/input';
import { Label } from '@/components/ui/label';
import { axiosClient } from '@/api/axiosClient';
import { GraduationCap, ArrowRight, AlertCircle, Eye, EyeOff } from 'lucide-react';

export function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.toLowerCase().endsWith('hust.edu.vn') && !email.toLowerCase().endsWith('gmail.com')) {
      setError('Vui lòng sử dụng Email sinh viên (@hust.edu.vn) hoặc Email cá nhân hợp lệ');
      return;
    }

    if (password.length < 8) {
      setError('Mật khẩu phải chứa ít nhất 8 ký tự');
      return;
    }

    if (password !== confirmPassword) {
      setError('Xác nhận mật khẩu không trùng khớp');
      return;
    }

    setIsLoading(true);
    try {
      await axiosClient.post('/auth/admin/register', {
        email,
        password,
      });

      // Chuyển sang màn hình xác thực OTP và truyền email theo state
      navigate('/verify-otp', { state: { email } });
    } catch (err: any) {
      setError(err?.message || 'Đăng ký tài khoản thất bại. Email có thể đã tồn tại!');
    } finally {
      setIsLoading(false);
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
            Tạo tài khoản SQB Network
          </CardTitle>
          <CardDescription className="text-sm text-slate-500">
            Mạng xã hội & Ngân hàng đề thi Đại học Bách Khoa Hà Nội
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 pt-2">
          {error && (
            <div className="mb-4 flex items-center gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-600 border border-red-100">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email">Email sinh viên / giảng viên</Label>
              <Input
                id="email"
                type="email"
                placeholder="a.bc201234@hust.edu.vn"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full border border-slate-200 rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="password">Mật khẩu</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Tối thiểu 8 ký tự"
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

            <div className="space-y-1.5">
              <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="Nhập lại mật khẩu"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                className="w-full border border-slate-200 rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold text-sm transition-colors cursor-pointer disabled:opacity-50 mt-2 shadow-md shadow-indigo-100"
            >
              {isLoading ? 'Đang khởi tạo tài khoản...' : 'Đăng ký tài khoản'}
              {!isLoading && <ArrowRight className="w-4 h-4" />}
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-slate-500 border-t border-slate-100 pt-4">
            Đã có tài khoản?{' '}
            <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-700">
              Đăng nhập ngay
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
