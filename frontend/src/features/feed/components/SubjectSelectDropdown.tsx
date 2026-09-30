import React, { useState, useRef, useEffect, useMemo } from 'react';
import type { SubjectResponse } from '@/features/session/types/session.types';
import { Tag, ChevronDown, Check, Search, X } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SubjectSelectDropdownProps {
  subjects: SubjectResponse[];
  value?: number;
  onChange: (subjectId: number | undefined) => void;
  disabled?: boolean;
  placeholder?: string;
}

export const SubjectSelectDropdown: React.FC<SubjectSelectDropdownProps> = ({
  subjects,
  value,
  onChange,
  disabled = false,
  placeholder = 'Chọn môn học bài giảng * (Bắt buộc)',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const selectedSubject = subjects.find((s) => s.subjectId === value);

  const filteredSubjects = useMemo(() => {
    if (!searchQuery.trim()) return subjects;
    const q = searchQuery.toLowerCase().trim();
    return subjects.filter(
      (s) =>
        s.code.toLowerCase().includes(q) ||
        s.name.toLowerCase().includes(q)
    );
  }, [subjects, searchQuery]);

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
      setTimeout(() => {
        searchInputRef.current?.focus();
      }, 50);
    } else {
      setSearchQuery('');
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  return (
    <div className="relative inline-block w-full sm:w-auto text-left" ref={dropdownRef}>
      {/* Trigger Button */}
      <button
        type="button"
        onClick={() => !disabled && setIsOpen((prev) => !prev)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        className={cn(
          'flex items-center justify-between gap-2 px-3 py-2 rounded-2xl text-xs font-semibold transition-all duration-200 cursor-pointer border select-none w-full sm:min-w-[280px]',
          selectedSubject
            ? 'bg-indigo-50/90 dark:bg-indigo-950/60 text-indigo-950 dark:text-indigo-200 border-indigo-200/90 dark:border-indigo-800'
            : 'bg-indigo-50/50 dark:bg-indigo-950/30 text-indigo-800/80 dark:text-indigo-300 border-indigo-200/60 dark:border-indigo-800/60',
          isOpen && 'ring-2 ring-indigo-500/25 border-indigo-500/60 bg-white dark:bg-slate-900 shadow-xs',
          disabled && 'opacity-60 cursor-not-allowed'
        )}
      >
        <div className="flex items-center gap-2 min-w-0 pr-1">
          <Tag className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400 shrink-0" />
          {selectedSubject ? (
            <div className="truncate text-left">
              <span className="font-bold text-indigo-600 dark:text-indigo-400 mr-1.5">
                [{selectedSubject.code}]
              </span>
              <span>{selectedSubject.name}</span>
            </div>
          ) : (
            <span className="text-indigo-700/80 dark:text-indigo-300 font-medium">
              {placeholder}
            </span>
          )}
        </div>

        <ChevronDown
          className={cn(
            'w-4 h-4 text-indigo-500 dark:text-indigo-400 transition-transform duration-200 shrink-0',
            isOpen && 'rotate-180'
          )}
        />
      </button>

      {/* Floating Menu */}
      {isOpen && (
        <div
          role="listbox"
          className="absolute left-0 mt-2 w-full sm:w-84 origin-top-left rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/90 dark:border-slate-800 shadow-xl shadow-slate-900/10 dark:shadow-black/40 p-2 z-50 animate-in fade-in zoom-in-95 duration-150"
        >
          {/* Search bar inside dropdown */}
          {subjects.length > 3 && (
            <div className="relative mb-2">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
              <input
                ref={searchInputRef}
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Tìm mã hoặc tên môn học..."
                className="w-full pl-8 pr-7 py-1.5 bg-slate-50 dark:bg-slate-800/80 rounded-xl text-xs text-slate-800 dark:text-slate-100 placeholder:text-slate-400 focus:outline-hidden focus:ring-1 focus:ring-indigo-500/40 border border-slate-200/70 dark:border-slate-700/70"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 cursor-pointer"
                >
                  <X className="w-3 h-3" />
                </button>
              )}
            </div>
          )}

          <div className="max-h-56 overflow-y-auto space-y-1 scrollbar-thin">
            {filteredSubjects.length === 0 ? (
              <div className="p-3 text-center text-xs text-slate-400 dark:text-slate-500">
                Không tìm thấy môn học phù hợp
              </div>
            ) : (
              filteredSubjects.map((s) => {
                const isSelected = value === s.subjectId;

                return (
                  <button
                    key={s.subjectId}
                    type="button"
                    role="option"
                    aria-selected={isSelected}
                    onClick={() => {
                      onChange(s.subjectId);
                      setIsOpen(false);
                    }}
                    className={cn(
                      'w-full flex items-center justify-between p-2 rounded-xl text-left transition-colors cursor-pointer group text-xs',
                      isSelected
                        ? 'bg-indigo-50/90 dark:bg-indigo-950/50 text-indigo-900 dark:text-indigo-200 font-semibold'
                        : 'hover:bg-slate-100/80 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-300'
                    )}
                  >
                    <div className="flex items-center gap-2 min-w-0 pr-2">
                      <span className="px-1.5 py-0.5 rounded-md bg-indigo-100/80 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 font-bold text-[10px] shrink-0 font-mono">
                        {s.code}
                      </span>
                      <span className="truncate">{s.name}</span>
                    </div>

                    {isSelected && (
                      <Check className="w-4 h-4 text-indigo-600 dark:text-indigo-400 shrink-0" />
                    )}
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
};
