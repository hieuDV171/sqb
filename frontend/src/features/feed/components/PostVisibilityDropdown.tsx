import React, { useState, useRef, useEffect } from 'react';
import type { PostVisibility } from '@/types/post.types';
import { Globe, Users, Lock, ChevronDown, Check } from 'lucide-react';
import { cn } from '@/lib/utils';

interface VisibilityOption {
  value: PostVisibility;
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  colorClass: string;
  bgClass: string;
}

const VISIBILITY_OPTIONS: VisibilityOption[] = [
  {
    value: 'PUBLIC',
    label: 'Công khai',
    description: 'Tất cả mọi người đều có thể xem',
    icon: Globe,
    colorClass: 'text-emerald-500 dark:text-emerald-400',
    bgClass: 'bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-300',
  },
  {
    value: 'FRIENDS',
    label: 'Bạn bè',
    description: 'Chỉ bạn bè mới có thể xem',
    icon: Users,
    colorClass: 'text-indigo-500 dark:text-indigo-400',
    bgClass: 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300',
  },
  {
    value: 'ONLY_ME',
    label: 'Chỉ mình tôi',
    description: 'Chỉ riêng bạn nhìn thấy bài viết này',
    icon: Lock,
    colorClass: 'text-amber-500 dark:text-amber-400',
    bgClass: 'bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-300',
  },
];

interface PostVisibilityDropdownProps {
  value: PostVisibility;
  onChange: (value: PostVisibility) => void;
  disabled?: boolean;
}

export const PostVisibilityDropdown: React.FC<PostVisibilityDropdownProps> = ({
  value,
  onChange,
  disabled = false,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [openUpward, setOpenUpward] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const currentOption =
    VISIBILITY_OPTIONS.find((opt) => opt.value === value) || VISIBILITY_OPTIONS[0];
  const CurrentIcon = currentOption.icon;

  useEffect(() => {
    if (isOpen && dropdownRef.current) {
      const rect = dropdownRef.current.getBoundingClientRect();
      const spaceBelow = window.innerHeight - rect.bottom;
      if (spaceBelow < 220 && rect.top > 220) {
        setOpenUpward(true);
      } else {
        setOpenUpward(false);
      }
    }
  }, [isOpen]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleKeyDown);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  return (
    <div className="relative inline-block text-left" ref={dropdownRef}>
      {/* Trigger Button */}
      <button
        type="button"
        onClick={() => !disabled && setIsOpen((prev) => !prev)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        className={cn(
          'flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-semibold transition-all duration-200 cursor-pointer border select-none',
          'bg-slate-100 hover:bg-slate-200/80 text-slate-700 border-slate-200/80',
          'dark:bg-slate-800 dark:hover:bg-slate-700/80 dark:text-slate-200 dark:border-slate-700/80',
          isOpen && 'ring-2 ring-blue-500/20 border-blue-500/50 bg-white dark:bg-slate-800 shadow-xs',
          disabled && 'opacity-60 cursor-not-allowed'
        )}
      >
        <CurrentIcon className={cn('w-3.5 h-3.5 shrink-0', currentOption.colorClass)} />
        <span className="leading-none">{currentOption.label}</span>
        <ChevronDown
          className={cn(
            'w-3.5 h-3.5 text-slate-400 transition-transform duration-200 shrink-0 ml-0.5',
            isOpen && 'rotate-180 text-blue-500'
          )}
        />
      </button>

      {/* Floating Dropdown Menu */}
      {isOpen && (
        <div
          role="listbox"
          className={cn(
            'absolute left-0 w-72 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/90 dark:border-slate-800 shadow-2xl shadow-slate-900/15 dark:shadow-black/50 p-1.5 z-50 animate-in fade-in duration-150',
            openUpward
              ? 'bottom-full mb-2 origin-bottom-left zoom-in-95'
              : 'top-full mt-2 origin-top-left zoom-in-95'
          )}
        >
          <div className="px-2.5 py-1.5 text-[11px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider">
            Ai có thể xem bài viết?
          </div>

          <div className="space-y-1">
            {VISIBILITY_OPTIONS.map((option) => {
              const Icon = option.icon;
              const isSelected = value === option.value;

              return (
                <button
                  key={option.value}
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  onClick={() => {
                    onChange(option.value);
                    setIsOpen(false);
                  }}
                  className={cn(
                    'w-full flex items-center justify-between p-2 rounded-xl text-left transition-all cursor-pointer group',
                    isSelected
                      ? 'bg-blue-50/80 dark:bg-blue-950/40 text-blue-900 dark:text-blue-200'
                      : 'hover:bg-slate-100/80 dark:hover:bg-slate-800/60 text-slate-800 dark:text-slate-200'
                  )}
                >
                  <div className="flex items-start gap-2.5 min-w-0 pr-2">
                    <div
                      className={cn(
                        'p-1.5 rounded-lg shrink-0 mt-0.5 transition-transform group-hover:scale-105',
                        option.bgClass
                      )}
                    >
                      <Icon className={cn('w-4 h-4', option.colorClass)} />
                    </div>
                    <div className="min-w-0">
                      <div className="text-xs font-bold leading-snug">{option.label}</div>
                      <div className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight mt-0.5 line-clamp-2">
                        {option.description}
                      </div>
                    </div>
                  </div>

                  {isSelected && (
                    <Check className="w-4 h-4 text-blue-600 dark:text-blue-400 shrink-0 ml-1.5" />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
