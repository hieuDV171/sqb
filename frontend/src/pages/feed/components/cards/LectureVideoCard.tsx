import { useState } from 'react';
import type { ActivityFeedItemDto } from '@/types/activityFeed.types';
import { Play, BookOpen, GraduationCap, Video, Clock, Heart, MessageSquare } from 'lucide-react';

interface LectureVideoCardProps {
  item: ActivityFeedItemDto;
}

export function LectureVideoCard({ item }: LectureVideoCardProps) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [likes, setLikes] = useState(item.content.reactCount || 48);
  const [liked, setLiked] = useState(false);

  const handleToggleLike = () => {
    setLiked(!liked);
    setLikes(liked ? likes - 1 : likes + 1);
  };

  return (
    <article className="rounded-2xl border border-indigo-200/80 dark:border-slate-800 bg-linear-to-b from-indigo-50/30 dark:from-slate-900 via-white dark:via-slate-900 to-white dark:to-slate-900 p-4 sm:p-5 shadow-xs space-y-3.5 transition-all hover:shadow-md">
      {/* Header: Actor & Academic Tag */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-linear-to-tr from-sky-500 to-indigo-600 flex items-center justify-center text-white font-bold text-sm shadow-xs overflow-hidden shrink-0">
            {item.actor.avatarUrl ? (
              <img src={item.actor.avatarUrl} alt={item.actor.fullName} className="w-full h-full object-cover" />
            ) : (
              item.actor.fullName.charAt(0)
            )}
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h4 className="font-bold text-sm sm:text-base text-slate-900 dark:text-slate-100 leading-tight">
                {item.actor.fullName}
              </h4>
              <span className="text-[10px] font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-900 flex items-center gap-1">
                <GraduationCap className="w-3 h-3" />
                Giảng viên
              </span>
              <span className="text-[10px] font-bold bg-indigo-100 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-400 px-2 py-0.5 rounded-full flex items-center gap-1">
                <Video className="w-3 h-3" />
                Video bài giảng mới
              </span>
            </div>

            <div className="flex items-center gap-1.5 text-xs text-slate-400 dark:text-slate-500 mt-0.5">
              <span>{item.createdAt}</span>
              {item.content.subject_code && (
                <>
                  <span>•</span>
                  <span className="inline-flex items-center gap-1 text-indigo-600 dark:text-indigo-400 font-semibold bg-indigo-50 dark:bg-indigo-950/50 px-2 py-0.5 rounded-md">
                    <BookOpen className="w-3 h-3" />
                    {item.content.subject_code} • {item.content.subject_name}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Video Headline & Description */}
      <div className="space-y-1">
        <h3 className="font-bold text-base text-slate-900 dark:text-slate-100 leading-snug">
          {item.content.title || 'Chuyên đề: Bài giảng ôn tập trọng tâm'}
        </h3>
        <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
          {item.content.description}
        </p>
      </div>

      {/* Video Player Mockup: Play button is centered */}
      <div className="relative rounded-2xl overflow-hidden aspect-video bg-slate-950 flex items-center justify-center group shadow-md">
        {item.content.media_url ? (
          <img
            src={item.content.media_url}
            alt="Video Thumbnail"
            className="absolute inset-0 w-full h-full object-cover opacity-85 group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="absolute inset-0 bg-linear-to-tr from-slate-900 to-indigo-950" />
        )}

        {/* Video Overlay Info */}
        <div className="absolute top-3 left-3 px-2.5 py-1 bg-black/60 backdrop-blur-md text-white text-[11px] font-semibold rounded-lg flex items-center gap-1.5 z-10">
          <Clock className="w-3.5 h-3.5 text-amber-400" />
          <span>Thời lượng: 42 phút</span>
        </div>

        {/* Play Button - Centered */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-10">
          <button
            type="button"
            onClick={() => setIsPlaying(!isPlaying)}
            className="pointer-events-auto w-14 h-14 sm:w-16 sm:h-16 rounded-full bg-indigo-600/90 hover:bg-indigo-600 text-white flex items-center justify-center shadow-xl shadow-indigo-950/60 hover:scale-110 active:scale-95 transition-all cursor-pointer group-hover:bg-indigo-500"
          >
            <Play className="w-7 h-7 fill-white translate-x-0.5" />
          </button>
        </div>
      </div>

      {/* Bottom Actions Bar */}
      <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleToggleLike}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs sm:text-sm font-semibold transition-colors cursor-pointer ${
              liked 
                ? 'text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/50' 
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            <Heart className={`w-4 h-4 ${liked ? 'fill-rose-500 text-rose-500' : ''}`} />
            <span>{likes}</span>
          </button>

          <button
            type="button"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs sm:text-sm font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <MessageSquare className="w-4 h-4" />
            <span>{item.content.commentCount || 12} thảo luận</span>
          </button>
        </div>

        <button
          type="button"
          className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs active:scale-98 transition-all cursor-pointer flex items-center gap-1.5"
        >
          <Play className="w-3.5 h-3.5 fill-white" />
          <span>Vào học ngay</span>
        </button>
      </div>
    </article>
  );
}
