import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  X,
  History,
  TrendingUp,
  User,
  FileText,
  HelpCircle,
  FolderKanban,
  BookOpen,
  ArrowRight,
  Loader2,
  Trash2,
} from 'lucide-react';
import {
  useGlobalSearch,
  useSavedAndTrendingSearches,
  useDeleteSavedSearch,
} from '../hooks/useSearch';
import type { SearchResultItemDto } from '../types/search.types';

interface GlobalSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialQuery?: string;
}

export function GlobalSearchModal({
  isOpen,
  onClose,
  initialQuery = '',
}: GlobalSearchModalProps) {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);

  const [searchTerm, setSearchTerm] = useState(initialQuery);
  const [debouncedQuery, setDebouncedQuery] = useState(initialQuery);

  // Debounce search query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchTerm.trim());
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  // Focus input when modal opens
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        inputRef.current?.focus();
        inputRef.current?.select();
      }, 50);
    } else {
      setSearchTerm('');
      setDebouncedQuery('');
    }
  }, [isOpen]);

  // Query saved & trending
  const { data: savedTrendingData } = useSavedAndTrendingSearches(isOpen && !debouncedQuery);
  const deleteSavedMutation = useDeleteSavedSearch();

  // Search live query
  const { data: searchResults, isFetching } = useGlobalSearch({
    query: debouncedQuery,
    type: 'ALL',
    limit: 6,
    enabled: isOpen && !!debouncedQuery,
  });

  if (!isOpen) return null;

  const handleSelectKeyword = (keyword: string) => {
    setSearchTerm(keyword);
    setDebouncedQuery(keyword);
  };

  const handleViewAllResults = () => {
    if (!searchTerm.trim()) return;
    onClose();
    navigate(`/search?q=${encodeURIComponent(searchTerm.trim())}`);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleViewAllResults();
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  const handleNavigateItem = (item: SearchResultItemDto) => {
    onClose();
    switch (item.type) {
      case 'USER':
        if (item.user) navigate(`/users/${item.user.userId}`);
        break;
      case 'POST':
        if (item.post) navigate(`/feed?postId=${item.post.postId}`);
        break;
      case 'QUESTION':
        if (item.question) navigate(`/sessions?questionId=${item.question.questionId}`);
        break;
      case 'SESSION':
        if (item.session) navigate(`/sessions/${item.session.sessionId}`);
        break;
      case 'SUBJECT':
        if (item.subject) navigate(`/search?q=${encodeURIComponent(item.subject.name)}&type=SUBJECT`);
        break;
      default:
        break;
    }
  };

  const results = searchResults?.items || [];
  const savedSearches = savedTrendingData?.savedSearches || [];
  const trendingSearches = savedTrendingData?.trendingSearches || [];

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center p-3 sm:p-6 pt-16 sm:pt-24 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div
        className="relative w-full max-w-2xl rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden flex flex-col max-h-[80vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Search Bar Input */}
        <div className="relative flex items-center px-4 sm:px-6 py-4 border-b border-slate-100 dark:border-slate-800">
          <Search className="w-5 h-5 text-indigo-500 shrink-0" />
          <input
            ref={inputRef}
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Tìm môn học, sinh viên, bài viết, câu hỏi ôn tập..."
            className="w-full pl-3 pr-10 text-sm sm:text-base font-medium bg-transparent text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 outline-none"
          />

          <div className="flex items-center gap-1.5 shrink-0">
            {searchTerm && (
              <button
                type="button"
                onClick={() => {
                  setSearchTerm('');
                  setDebouncedQuery('');
                  inputRef.current?.focus();
                }}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <X className="w-4 h-4" />
              </button>
            )}
            <kbd className="hidden sm:inline-flex items-center px-2 py-0.5 text-[11px] font-mono font-medium text-slate-500 dark:text-slate-400 bg-slate-100 dark:bg-slate-800 rounded-md border border-slate-200 dark:border-slate-700">
              ESC
            </kbd>
          </div>
        </div>

        {/* Content Body */}
        <div className="p-4 sm:p-6 overflow-y-auto space-y-6 flex-1">
          {/* Case 1: Has Search Query */}
          {debouncedQuery ? (
            <div>
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider">
                  Kết quả nhanh ({searchResults?.totalHits ?? results.length})
                </span>
                {isFetching && (
                  <span className="flex items-center gap-1.5 text-xs text-indigo-500">
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    Đang tra cứu...
                  </span>
                )}
              </div>

              {results.length === 0 && !isFetching ? (
                <div className="py-12 text-center">
                  <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400 mx-auto mb-3">
                    <Search className="w-6 h-6" />
                  </div>
                  <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Không tìm thấy kết quả phù hợp cho &quot;{debouncedQuery}&quot;
                  </p>
                  <p className="text-xs text-slate-400 mt-1">
                    Hãy thử từ khóa ngắn hơn hoặc kiểm tra lỗi chính tả
                  </p>
                </div>
              ) : (
                <div className="space-y-2">
                  {results.map((item, idx) => (
                    <div
                      key={idx}
                      onClick={() => handleNavigateItem(item)}
                      className="flex items-start gap-3 p-3 rounded-2xl border border-slate-100 dark:border-slate-800/80 hover:border-indigo-200 dark:hover:border-indigo-800/60 bg-white dark:bg-slate-900 hover:bg-indigo-50/20 dark:hover:bg-indigo-950/20 transition-all cursor-pointer group"
                    >
                      {/* Icon type */}
                      <div className="p-2 rounded-xl shrink-0 mt-0.5 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 group-hover:bg-indigo-100 dark:group-hover:bg-indigo-900/40 group-hover:text-indigo-600 transition-colors">
                        {item.type === 'USER' && <User className="w-4 h-4" />}
                        {item.type === 'POST' && <FileText className="w-4 h-4" />}
                        {item.type === 'QUESTION' && <HelpCircle className="w-4 h-4" />}
                        {item.type === 'SESSION' && <FolderKanban className="w-4 h-4" />}
                        {item.type === 'SUBJECT' && <BookOpen className="w-4 h-4" />}
                      </div>

                      {/* Content details */}
                      <div className="flex-1 min-w-0">
                        {item.type === 'USER' && item.user && (
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-sm text-slate-900 dark:text-slate-100 truncate">
                                {item.user.fullName || item.user.username}
                              </span>
                              <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                                @{item.user.username}
                              </span>
                            </div>
                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                              {item.user.schoolFaculty || item.user.role || 'Thành viên'}
                            </p>
                          </div>
                        )}

                        {item.type === 'POST' && item.post && (
                          <div>
                            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 block mb-0.5">
                              Bài viết từ {item.post.authorName}
                            </span>
                            <p className="text-sm font-medium text-slate-900 dark:text-slate-100 line-clamp-2">
                              {item.post.content}
                            </p>
                          </div>
                        )}

                        {item.type === 'QUESTION' && item.question && (
                          <div>
                            <div className="flex items-center gap-2 mb-0.5">
                              {item.question.subjectName && (
                                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                                  {item.question.subjectName}
                                </span>
                              )}
                              {item.question.difficulty && (
                                <span className="text-[10px] font-medium text-slate-500">
                                  {item.question.difficulty}
                                </span>
                              )}
                            </div>
                            <p className="text-sm font-medium text-slate-900 dark:text-slate-100 line-clamp-2">
                              {item.question.content}
                            </p>
                          </div>
                        )}

                        {item.type === 'SESSION' && item.session && (
                          <div>
                            <span className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 block mb-0.5">
                              Phiên nộp câu hỏi
                            </span>
                            <p className="text-sm font-bold text-slate-900 dark:text-slate-100">
                              {item.session.title}
                            </p>
                          </div>
                        )}

                        {item.type === 'SUBJECT' && item.subject && (
                          <div>
                            <span className="text-xs font-bold text-blue-600 dark:text-blue-400">
                              [{item.subject.code}]
                            </span>
                            <p className="text-sm font-bold text-slate-900 dark:text-slate-100">
                              {item.subject.name}
                            </p>
                          </div>
                        )}
                      </div>

                      <ArrowRight className="w-4 h-4 text-slate-300 dark:text-slate-600 group-hover:text-indigo-500 group-hover:translate-x-0.5 transition-all shrink-0 mt-1" />
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : (
            /* Case 2: No Search Query -> Show Recent & Trending */
            <div className="space-y-6">
              {/* Recent Searches */}
              {savedSearches.length > 0 && (
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="flex items-center gap-1.5 text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider">
                      <History className="w-3.5 h-3.5" />
                      Tìm kiếm gần đây
                    </span>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    {savedSearches.map((s) => (
                      <div
                        key={s.savedSearchId}
                        className="group flex items-center gap-1.5 pl-3 pr-1.5 py-1.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 hover:border-indigo-300 dark:hover:border-indigo-700 transition-colors cursor-pointer"
                        onClick={() => handleSelectKeyword(s.queryText)}
                      >
                        <span className="text-xs font-medium text-slate-700 dark:text-slate-300">
                          {s.queryText}
                        </span>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteSavedMutation.mutate(s.savedSearchId);
                          }}
                          className="p-1 text-slate-400 hover:text-rose-500 rounded-lg transition-colors cursor-pointer"
                          title="Xóa khỏi lịch sử"
                        >
                          <Trash2 className="w-3 h-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Trending Searches */}
              {trendingSearches.length > 0 && (
                <div>
                  <div className="flex items-center gap-1.5 text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider mb-3">
                    <TrendingUp className="w-3.5 h-3.5 text-amber-500" />
                    Từ khóa thịnh hành
                  </div>

                  <div className="flex flex-wrap gap-2">
                    {trendingSearches.map((trend, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => handleSelectKeyword(trend)}
                        className="px-3 py-1.5 rounded-xl text-xs font-semibold bg-indigo-50/60 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-100 dark:border-indigo-800/50 transition-colors flex items-center gap-1.5 cursor-pointer"
                      >
                        <span className="text-amber-500 font-bold">#{idx + 1}</span>
                        {trend}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer with Enter CTA */}
        {debouncedQuery && (
          <div className="p-3 sm:p-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50 flex items-center justify-between">
            <span className="text-xs text-slate-500 dark:text-slate-400">
              Nhấn <kbd className="px-1.5 py-0.5 font-mono text-[10px] bg-white dark:bg-slate-800 border rounded">Enter</kbd> để xem trang kết quả đầy đủ
            </span>

            <button
              type="button"
              onClick={handleViewAllResults}
              className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 transition-colors cursor-pointer"
            >
              <span>Xem tất cả</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
