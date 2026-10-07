import { useNavigate } from 'react-router-dom';
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
import type { NotificationDto, NotificationType } from '../types/notification.types';
import { useMarkAsRead } from '../hooks/useNotification';

interface NotificationItemProps {
  notification: NotificationDto;
  onItemClick?: () => void;
}

export function NotificationItem({ notification, onItemClick }: NotificationItemProps) {
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
          navigate(`/feed?postId=${targetId}`);
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
      const diffMs = Date.now() - date.getTime();
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
    <div
      onClick={handleClick}
      className={`group relative flex items-start gap-3.5 p-3.5 sm:p-4 rounded-2xl transition-all cursor-pointer ${
        isRead
          ? 'bg-transparent hover:bg-slate-50 dark:hover:bg-slate-800/40 text-slate-600 dark:text-slate-300'
          : 'bg-indigo-50/40 dark:bg-indigo-950/20 hover:bg-indigo-50/70 dark:hover:bg-indigo-950/30 text-slate-900 dark:text-slate-100 font-medium'
      }`}
    >
      {/* Icon or Actor Avatar */}
      <div className="relative shrink-0 mt-0.5">
        {notification.actor?.avatarUrl ? (
          <div className="w-10 h-10 rounded-full overflow-hidden border border-slate-200 dark:border-slate-700 bg-slate-100">
            <img
              src={notification.actor.avatarUrl}
              alt={notification.actor.username}
              className="w-full h-full object-cover"
            />
          </div>
        ) : (
          <div className={`w-10 h-10 rounded-2xl flex items-center justify-center ${iconBg}`}>
            <TypeIcon className="w-5 h-5" />
          </div>
        )}

        {/* Mini badge icon on avatar */}
        {notification.actor?.avatarUrl && (
          <div
            className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full ring-2 ring-white dark:ring-slate-900 flex items-center justify-center ${iconBg}`}
          >
            <TypeIcon className="w-3 h-3" />
          </div>
        )}
      </div>

      {/* Text Info */}
      <div className="flex-1 min-w-0 pr-2">
        <p className="text-xs sm:text-sm font-semibold text-slate-900 dark:text-slate-100 leading-snug">
          {notification.title}
        </p>
        <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 line-clamp-2 leading-relaxed">
          {notification.body}
        </p>
        <span className="inline-block text-[11px] text-slate-400 dark:text-slate-500 mt-1.5">
          {formatRelativeTime(notification.createdAt)}
        </span>
      </div>

      {/* Unread indicator dot */}
      {!isRead && (
        <div className="shrink-0 mt-1.5 w-2 h-2 rounded-full bg-indigo-600 dark:bg-indigo-400 ring-4 ring-indigo-100 dark:ring-indigo-950/50 animate-pulse" />
      )}
    </div>
  );
}
