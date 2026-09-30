import type { ProfileResponse } from '@/types/user.types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import {
  GraduationCap,
  Building2,
  BookOpen,
  Users2,
  Mail,
  Calendar,
  Clock,
  IdCard,
} from 'lucide-react';

interface ProfileAcademicInfoProps {
  profile: ProfileResponse;
}

export function ProfileAcademicInfo({ profile }: ProfileAcademicInfoProps) {
  const isLecturer = profile.role === 'LECTURER';

  const academicFields = [
    {
      icon: IdCard,
      label: isLecturer ? 'Mã số giảng viên (MSGV)' : 'Mã số sinh viên (MSSV)',
      value: profile.studentLecturerCode || 'Chưa cập nhật',
      highlight: true,
    },
    {
      icon: Building2,
      label: 'Khoa / Viện đào tạo',
      value: profile.schoolFaculty || 'Trường Công nghệ Thông tin & Truyền thông',
    },
    {
      icon: BookOpen,
      label: 'Chuyên ngành',
      value: profile.major || 'Khoa học Máy tính & Kỹ thuật Phần mềm',
    },
    {
      icon: Users2,
      label: isLecturer ? 'Bộ môn phụ trách' : 'Lớp quản lý / sinh hoạt',
      value: profile.className || (isLecturer ? 'Khoa học Máy tính' : 'Việt Nhật 01 - K66'),
    },
    {
      icon: Mail,
      label: 'Email liên hệ',
      value: profile.email,
    },
    {
      icon: Calendar,
      label: 'Ngày sinh',
      value: profile.dateOfBirth
        ? new Date(profile.dateOfBirth).toLocaleDateString('vi-VN')
        : 'Chưa công khai',
    },
    {
      icon: Clock,
      label: 'Múi giờ hệ thống',
      value: profile.timezone || 'Asia/Ho_Chi_Minh (GMT+7)',
    },
  ];

  return (
    <Card className="rounded-3xl border-slate-200/80 dark:border-slate-800 shadow-xs">
      <CardHeader className="pb-3">
        <CardTitle className="text-base font-bold flex items-center gap-2 text-slate-900 dark:text-white">
          <div className="w-8 h-8 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
            <GraduationCap className="w-4 h-4" />
          </div>
          <span>Thông tin Học thuật & Hồ sơ</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {academicFields.map((field, idx) => {
            const Icon = field.icon;
            return (
              <div
                key={idx}
                className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 flex items-start gap-3 transition-colors hover:border-slate-200 dark:hover:border-slate-700"
              >
                <div className="p-2 rounded-xl bg-white dark:bg-slate-800 shadow-2xs text-slate-500 dark:text-slate-400 shrink-0">
                  <Icon className="w-4 h-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-[11px] text-slate-400 dark:text-slate-500 font-medium">
                    {field.label}
                  </p>
                  <p
                    className={`text-xs sm:text-sm font-semibold truncate mt-0.5 ${
                      field.highlight
                        ? 'font-mono text-indigo-600 dark:text-indigo-400 font-bold'
                        : 'text-slate-800 dark:text-slate-200'
                    }`}
                  >
                    {field.value}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
