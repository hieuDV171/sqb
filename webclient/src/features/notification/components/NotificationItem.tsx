import { useNavigate } from 'react-router-dom';
import { UserAvatar } from '@/components/ui/user-avatar';
import {
  UserPlus,
  MessageSquare,
  CheckCircle2,
  Trophy,
  AlertTriangle,
  Flame,
  Award,
  Bell,
} from 'lucide-react';
import type { NotificationDto, NotificationType } from '@/features/notification';
import { useMarkAsRead } from '@/features/notification';

interface NotificationItemProps {
  notification: NotificationDto;
  now: number;
  onItemClick?: () => void;
}

export function NotificationItem({ notification, now, onItemClick }: NotificationItemProps) {
  const navigate = useNavigate();
  const markAsReadMutation = useMarkAsRead();

  const isRead = !!notification.readAt;

  const getIconAndStyle = (type: NotificationType) => {
    switch (type) {
      case 'FRIEND_REQUEST':
        return {
          icon: UserPlus,
          bg: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400',
        };
      case 'NEW_COMMENT':
        return {
          icon: MessageSquare,
          bg: 'bg-blue-50 text-blue-600 dark:bg-blue-950/40 dark:text-blue-400',
        };
      case 'QUESTION_APPROVED':
        return {
          icon: CheckCircle2,
          bg: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400',
        };
      case 'EARNED_BADGE':
        return {
          icon: Trophy,
          bg: 'bg-amber-50 text-amber-600 dark:bg-amber-950/40 dark:text-amber-400',
        };
      case 'BADGE_REVOKED':
      case 'QUESTION_ERROR_PENALTY':
        return {
          icon: AlertTriangle,
          bg: 'bg-rose-50 text-rose-600 dark:bg-rose-950/40 dark:text-rose-400',
        };
      case 'GAME5_SEMESTER_RECAP':
        return {
          icon: Flame,
          bg: 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400',
        };
      case 'LEADERBOARD_HONOR':
        return {
          icon: Award,
          bg: 'bg-amber-50 text-amber-600 dark:bg-amber-950/40 dark:text-amber-400',
        };
      default:
        return {
          icon: Bell,
          bg: 'bg-slate-50 text-slate-600 dark:bg-slate-800 dark:text-slate-400',
        };
    }
  };

  const { icon: TypeIcon, bg: iconBg } = getIconAndStyle(notification.type);

  const handleClick = () => {
    if (!isRead) {
      markAsReadMutation.mutate(notification.notificationId);
    }
    if (onItemClick) {
      onItemClick();
    }

    // Determine target route
    if (notification.target) {
      const { type, targetId, url } = notification.target;
      if (url) {
        navigate(url);
        return;
      }
      switch (type) {
        case 'POST':
          navigate(`/posts/${targetId}`);
          break;
        case 'SESSION':
          navigate(`/sessions/${targetId}`);
          break;
        case 'QUESTION':
          navigate(`/sessions?questionId=${targetId}`);
          break;
        case 'USER':
          navigate(`/users/${targetId}`);
          break;
        case 'BADGE':
          navigate('/gamification?tab=badges');
          break;
        case 'CONVERSATION':
          navigate(`/messages/${targetId}`);
          break;
        default:
          break;
      }
    }
  };

  const formatRelativeTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      const diffMs = now - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      if (diffMins < 1) return 'Vừa xong';
      if (diffMins < 60) return `${diffMins} phút trước`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours} giờ trước`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays < 7) return `${diffDays} ngày trước`;
      return date.toLocaleDateString('vi-VN');
    } catch {
      return '';
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className={`group relative flex w-full items-start gap-3 rounded-xl p-3 text-left transition-colors focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-ring sm:p-4 ${
        isRead
          ? 'hover:bg-muted'
          : 'bg-academic-soft/55 hover:bg-academic-soft'
      }`}
    >
      {/* Icon or Actor Avatar */}
      <span className="relative shrink-0 mt-0.5">
        {notification.actor ? (
          <UserAvatar
            name={notification.actor.fullName || notification.actor.username}
            avatarUrl={notification.actor.avatarUrl}
            className="border border-border" decorative
          />
        ) : (
          <span className={`w-10 h-10 rounded-2xl flex items-center justify-center ${iconBg}`}>
            <TypeIcon className="w-5 h-5" />
          </span>
        )}

        {/* Mini badge icon on avatar */}
        {notification.actor && (
          <span
            className={`absolute -bottom-1 -right-1 grid size-5 place-items-center rounded-full ring-2 ring-card ${iconBg}`}
          >
            <TypeIcon className="w-3 h-3" />
          </span>
        )}
      </span>

      {/* Text Info */}
      <span className="flex-1 min-w-0 pr-2">
        <span className="block break-words text-sm font-semibold leading-snug text-foreground">
          {notification.title}
        </span>
        <span className="mt-1 line-clamp-2 break-words text-sm leading-relaxed text-muted-foreground">
          {notification.body}
        </span>
        <span className="mt-1.5 block text-xs text-muted-foreground">
          {formatRelativeTime(notification.createdAt)}
        </span>
      </span>

      {/* Unread indicator dot */}
      {!isRead && (
        <>
          <span className="sr-only">Chưa đọc</span>
          <span aria-hidden="true" className="mt-2 size-2 shrink-0 rounded-full bg-primary" />
        </>
      )}
    </button>
  );
}
