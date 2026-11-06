import { GraduationCap } from 'lucide-react';
import type { ProfileResponse } from '@/types/user.types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export function ProfileAcademicInfo({ profile }: { profile: ProfileResponse }) {
  const fields = [
    ['Mã sinh viên / cán bộ', profile.studentLecturerCode],
    ['Khoa / Viện', profile.schoolFaculty],
    ['Chuyên ngành', profile.major],
    ['Lớp / Bộ môn', profile.className],
    ['Email liên hệ', profile.email],
    ['Ngày sinh', profile.dateOfBirth?.split('-').reverse().join('/')],
    ['Múi giờ', profile.timezone],
  ].filter(([, value]) => Boolean(value));

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2 className="flex items-center gap-2 text-lg font-semibold">
            <span className="grid size-9 place-items-center rounded-xl bg-academic-soft text-academic">
              <GraduationCap className="size-5" aria-hidden="true" />
            </span>
            Thông tin hồ sơ
          </h2>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {fields.length ? (
          <dl className="divide-y divide-border">
            {fields.map(([label, value]) => (
              <div key={label} className="grid gap-1 py-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)] sm:gap-4">
                <dt className="text-sm text-muted-foreground">{label}</dt>
                <dd className="wrap-break-word text-sm font-medium">{value}</dd>
              </div>
            ))}
          </dl>
        ) : (
          <p className="text-sm text-muted-foreground">Chưa có thông tin hồ sơ để hiển thị.</p>
        )}
      </CardContent>
    </Card>
  );
}
