import { Link } from 'react-router-dom';
import { BookOpen, ClipboardList, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const studyLinks = [
  { path: '/questions', label: 'Phiên nộp của tôi', icon: ClipboardList },
  { path: '/questions/practice', label: 'Kho luyện tập', icon: BookOpen },
  { path: '/sessions/propose', label: 'Đề xuất phiên mới', icon: Plus },
] as const;

export function StudyNavigation({ current }: { current: typeof studyLinks[number]['path'] }) {
  return (
    <nav aria-label="Ngân hàng câu hỏi" className="flex flex-wrap gap-2">
      {studyLinks.map(({ path, label, icon: Icon }) => (
        <Button key={path} variant={path === current ? 'secondary' : 'outline'}
          render={<Link to={path} />} nativeButton={false}
          aria-current={path === current ? 'page' : undefined}
          className={cn('min-h-11', path === current && 'bg-academic-soft text-academic')}>
          <Icon aria-hidden="true" /> {label}
        </Button>
      ))}
    </nav>
  );
}
