import React, { useState } from 'react';
import type { ActivityFeedItemDto } from '../../types/feed.types';
import { Video, BookOpen, Play, MessageSquare, Heart } from 'lucide-react';
import { CommentSection } from '../CommentSection';

interface LectureVideoCardProps {
  item: ActivityFeedItemDto;
}

export const LectureVideoCard: React.FC<LectureVideoCardProps> = ({ item }) => {
  const { actor, content, createdAt, targetId } = item;
  const [isPlaying, setIsPlaying] = useState(false);
  const [showComments, setShowComments] = useState(false);
  const [isLiked, setIsLiked] = useState(content.reactedByMe || false);
  const [likeCount, setLikeCount] = useState(content.reactCount || 12);

  const toggleLike = () => {
    setIsLiked(!isLiked);
    setLikeCount((c) => (isLiked ? c - 1 : c + 1));
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-5 shadow-xs mb-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-3.5">
        <div className="flex items-center gap-3">
          <img
            src={
              actor.avatarUrl ||
              'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
            }
            alt={actor.fullName}
            className="w-10 h-10 rounded-full object-cover ring-2 ring-purple-500/40"
          />
          <div>
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="font-bold text-slate-900 dark:text-white text-sm">
                {actor.fullName}
              </span>
              {actor.role && (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-purple-100 dark:bg-purple-900/50 text-purple-700 dark:text-purple-300">
                  {actor.role === 'LECTURER' ? 'Giảng viên' : 'Sinh viên'}
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-400">
              {new Date(createdAt).toLocaleDateString('vi-VN', {
                hour: '2-digit',
                minute: '2-digit',
                day: '2-digit',
                month: '2-digit',
              })}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-purple-100 dark:bg-purple-900/60 text-purple-700 dark:text-purple-300 font-semibold text-xs">
          <Video className="w-3.5 h-3.5" />
          <span>Video học tập</span>
        </div>
      </div>

      {/* Video Content & Player */}
      <div className="mb-3">
        {content.subject_name && (
          <div className="flex items-center gap-1.5 text-xs text-purple-600 dark:text-purple-400 font-semibold mb-1.5">
            <BookOpen className="w-3.5 h-3.5" />
            <span>
              {content.subject_code ? `[${content.subject_code}] ` : ''}
              {content.subject_name}
            </span>
          </div>
        )}

        <h4 className="font-bold text-slate-900 dark:text-white text-base mb-1.5">
          {content.title || 'Video bài giảng & giải đề mẫu'}
        </h4>

        {content.description && (
          <p className="text-xs md:text-sm text-slate-600 dark:text-slate-300 mb-3 whitespace-pre-wrap">
            {content.description}
          </p>
        )}

        {/* Video Player */}
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-black flex items-center justify-center group">
          {content.media_url ? (
            <video
              src={content.media_url}
              controls={isPlaying}
              autoPlay={isPlaying}
              className="w-full h-full object-contain"
            />
          ) : (
            <div className="text-white text-xs">Không có nguồn video</div>
          )}

          {!isPlaying && content.media_url && (
            <button
              type="button"
              onClick={() => setIsPlaying(true)}
              className="absolute inset-0 flex items-center justify-center bg-black/40 hover:bg-black/50 transition-colors cursor-pointer group"
            >
              <div className="w-14 h-14 rounded-full bg-purple-600 text-white flex items-center justify-center shadow-lg shadow-purple-600/40 group-hover:scale-110 active:scale-95 transition-all">
                <Play className="w-6 h-6 fill-current ml-0.5" />
              </div>
            </button>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between pt-2.5 border-t border-slate-100 dark:border-slate-800 text-xs text-slate-500 dark:text-slate-400">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={toggleLike}
            className={`flex items-center gap-1.5 py-1 px-2 rounded-xl transition-colors cursor-pointer ${
              isLiked
                ? 'text-rose-600 font-semibold'
                : 'hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-slate-800'
            }`}
          >
            <Heart className={`w-4 h-4 ${isLiked ? 'fill-current text-rose-600' : ''}`} />
            <span>{likeCount}</span>
          </button>

          <button
            type="button"
            onClick={() => setShowComments(!showComments)}
            className="flex items-center gap-1.5 py-1 px-2 rounded-xl hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <MessageSquare className="w-4 h-4" />
            <span>{content.commentCount || 0} bình luận</span>
          </button>
        </div>
      </div>

      {/* Comment Section Drawer */}
      {showComments && <CommentSection targetType="POST" targetId={targetId} />}
    </div>
  );
};
