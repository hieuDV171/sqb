import {
  useEffect,
  useRef,
  useState,
  type RefObject,
} from 'react';
import { createPortal } from 'react-dom';
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
  RefreshCw,
} from 'lucide-react';
import {
  useDeleteSavedSearch,
  useGlobalSearch,
  useRecordSearch,
  useSavedAndTrendingSearches,
} from '../hooks/useSearch';
import type {
  SearchResultItemDto,
  UserSearchResultDto,
} from '../types/search.types';

interface GlobalSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialQuery?: string;
  triggerRef?: RefObject<HTMLElement | null>;
}

interface PanelPosition {
  top: number;
  left: number;
  width: number;
}

const DESKTOP_BREAKPOINT = 768;
const PANEL_MAX_WIDTH = 672;
const VIEWPORT_GUTTER = 16;

function getUserDisplayName(user: UserSearchResultDto) {
  return (
    user.fullName?.trim() ||
    user.studentLecturerCode?.trim() ||
    user.email?.trim() ||
    'Thành viên SQB'
  );
}

function getUserIdentifier(user: UserSearchResultDto) {
  return user.studentLecturerCode?.trim() || user.email?.trim();
}

export function GlobalSearchModal({
  isOpen,
  onClose,
  initialQuery = '',
  triggerRef,
}: GlobalSearchModalProps) {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const wasOpenRef = useRef(false);
  const [searchTerm, setSearchTerm] = useState(initialQuery);
  const [debouncedQuery, setDebouncedQuery] = useState(initialQuery);
  const [isDesktop, setIsDesktop] = useState(
    () => typeof window !== 'undefined' && window.innerWidth >= DESKTOP_BREAKPOINT
  );
  const [panelPosition, setPanelPosition] = useState<PanelPosition | null>(null);

  useEffect(() => {
    const handler = window.setTimeout(() => {
      setDebouncedQuery(searchTerm.trim());
    }, 300);
    return () => window.clearTimeout(handler);
  }, [searchTerm]);

  useEffect(() => {
    if (isOpen) {
      wasOpenRef.current = true;
      const focusTimer = window.setTimeout(() => {
        inputRef.current?.focus();
        inputRef.current?.select();
      }, 50);
      return () => window.clearTimeout(focusTimer);
    }

    const resetTimer = window.setTimeout(() => {
      setSearchTerm('');
      setDebouncedQuery('');
    }, 0);
    if (wasOpenRef.current) {
      wasOpenRef.current = false;
      window.setTimeout(() => triggerRef?.current?.focus(), 0);
    }
    return () => window.clearTimeout(resetTimer);
  }, [isOpen, triggerRef]);

  useEffect(() => {
    if (!isOpen) return;

    const updateLayout = () => {
      const desktop = window.innerWidth >= DESKTOP_BREAKPOINT;
      setIsDesktop(desktop);

      if (!desktop || !triggerRef?.current) {
        setPanelPosition(null);
        return;
      }

      const rect = triggerRef.current.getBoundingClientRect();
      const width = Math.min(PANEL_MAX_WIDTH, window.innerWidth - VIEWPORT_GUTTER * 2);
      const idealLeft = rect.left + rect.width / 2 - width / 2;
      const left = Math.max(
        VIEWPORT_GUTTER,
        Math.min(idealLeft, window.innerWidth - width - VIEWPORT_GUTTER)
      );

      setPanelPosition({
        top: rect.bottom + 8,
        left,
        width,
      });
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
      }
    };

    updateLayout();
    window.addEventListener('resize', updateLayout);
    window.addEventListener('scroll', updateLayout, true);
    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('resize', updateLayout);
      window.removeEventListener('scroll', updateLayout, true);
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, onClose, triggerRef]);

  useEffect(() => {
    if (!isOpen || isDesktop) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isDesktop, isOpen]);

  const {
    data: savedTrendingData,
    isError: isSavedTrendingError,
    refetch: refetchSavedTrending,
  } = useSavedAndTrendingSearches(isOpen && !debouncedQuery);
  const deleteSavedMutation = useDeleteSavedSearch();
  const recordSearchMutation = useRecordSearch();

  const {
    data: searchResults,
    isLoading,
    isFetching,
    isError,
    refetch,
  } = useGlobalSearch({
    query: debouncedQuery,
    type: 'ALL',
    limit: 6,
    enabled: isOpen && debouncedQuery.length >= 2,
  });

  if (!isOpen || typeof document === 'undefined') return null;

  const recordConfirmedQuery = () => {
    const query = searchTerm.trim();
    if (query.length >= 2) {
      recordSearchMutation.mutate(query);
    }
  };

  const handleSelectKeyword = (keyword: string) => {
    setSearchTerm(keyword);
    setDebouncedQuery(keyword);
    inputRef.current?.focus();
  };

  const handleViewAllResults = () => {
    const query = searchTerm.trim();
    if (!query) return;
    recordConfirmedQuery();
    onClose();
    navigate(`/search?q=${encodeURIComponent(query)}`);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleViewAllResults();
    }
  };

  const handleNavigateItem = (item: SearchResultItemDto) => {
    recordConfirmedQuery();
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
        if (item.subject) {
          navigate(
            `/search?q=${encodeURIComponent(item.subject.subjectName)}&type=SUBJECT`
          );
        }
        break;
    }
  };

  const results = searchResults?.items || [];
  const savedSearches = savedTrendingData?.savedSearches || [];
  const trendingSearches = savedTrendingData?.trendingSearches || [];
  const canSearch = debouncedQuery.length >= 2;

  const panel = (
    <div
      className={`fixed inset-0 z-[60] ${
        isDesktop
          ? 'bg-transparent'
          : 'flex items-start justify-center bg-black/50 px-3 pb-3 pt-16 backdrop-blur-xs'
      }`}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        id="global-search-panel"
        role="dialog"
        aria-label="Tìm kiếm toàn cục SQB"
        aria-modal={!isDesktop}
        style={
          isDesktop && panelPosition
            ? {
                position: 'absolute',
                top: panelPosition.top,
                left: panelPosition.left,
                width: panelPosition.width,
              }
            : undefined
        }
        className={`flex max-h-[min(76vh,680px)] w-full flex-col overflow-hidden border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900 ${
          isDesktop
            ? 'rounded-2xl animate-in fade-in zoom-in-95 duration-150'
            : 'max-w-2xl rounded-2xl animate-in fade-in slide-in-from-top-2 duration-150'
        }`}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="relative flex min-h-14 items-center border-b border-slate-100 px-4 dark:border-slate-800 sm:px-5">
          <Search aria-hidden="true" className="h-5 w-5 shrink-0 text-indigo-500" />
          <input
            ref={inputRef}
            type="search"
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            onKeyDown={handleKeyDown}
            aria-label="Nhập từ khóa tìm kiếm"
            aria-controls="global-search-results"
            placeholder="Tìm môn học, sinh viên, bài viết, câu hỏi ôn tập..."
            className="h-14 w-full bg-transparent px-3 text-base font-medium text-slate-900 outline-none placeholder:text-slate-400 dark:text-slate-100 dark:placeholder:text-slate-500"
          />

          <div className="flex shrink-0 items-center gap-1.5">
            {searchTerm && (
              <button
                type="button"
                onClick={() => {
                  setSearchTerm('');
                  setDebouncedQuery('');
                  inputRef.current?.focus();
                }}
                className="inline-flex h-9 w-9 cursor-pointer items-center justify-center rounded-xl text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:hover:bg-slate-800 dark:hover:text-slate-200"
                aria-label="Xóa từ khóa"
              >
                <X aria-hidden="true" className="h-4 w-4" />
              </button>
            )}
            {isDesktop ? (
              <kbd className="inline-flex items-center rounded-lg border border-slate-200 bg-slate-100 px-2 py-1 font-mono text-[11px] font-medium text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
                ESC
              </kbd>
            ) : (
              <button
                type="button"
                onClick={onClose}
                className="inline-flex h-9 w-9 cursor-pointer items-center justify-center rounded-xl text-slate-500 transition-colors hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:text-slate-300 dark:hover:bg-slate-800"
                aria-label="Đóng tìm kiếm"
              >
                <X aria-hidden="true" className="h-5 w-5" />
              </button>
            )}
          </div>
        </div>

        <div id="global-search-results" className="flex-1 overflow-y-auto p-3 sm:p-4">
          {debouncedQuery ? (
            <div>
              <div className="mb-3 flex items-center justify-between gap-3 px-1">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Kết quả nhanh ({searchResults?.totalHits ?? results.length})
                </span>
                {isFetching && !isLoading && (
                  <span
                    className="flex items-center gap-1.5 text-xs text-indigo-600 dark:text-indigo-400"
                    role="status"
                  >
                    <Loader2 aria-hidden="true" className="h-3.5 w-3.5 animate-spin" />
                    Đang cập nhật
                  </span>
                )}
              </div>

              {!canSearch ? (
                <p className="px-1 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
                  Nhập ít nhất 2 ký tự để tìm kiếm.
                </p>
              ) : isLoading ? (
                <div className="space-y-2" aria-label="Đang tải kết quả tìm kiếm">
                  {[1, 2, 3].map((item) => (
                    <div
                      key={item}
                      className="flex animate-pulse items-center gap-3 rounded-xl border border-slate-100 p-3 dark:border-slate-800"
                    >
                      <div className="h-9 w-9 rounded-xl bg-slate-200 dark:bg-slate-800" />
                      <div className="flex-1 space-y-2">
                        <div className="h-3 w-1/3 rounded bg-slate-200 dark:bg-slate-800" />
                        <div className="h-3 w-2/3 rounded bg-slate-100 dark:bg-slate-800/70" />
                      </div>
                    </div>
                  ))}
                </div>
              ) : isError ? (
                <div
                  role="alert"
                  className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-center dark:border-rose-900/50 dark:bg-rose-950/20"
                >
                  <p className="text-sm font-semibold text-rose-800 dark:text-rose-200">
                    Không thể tải kết quả tìm kiếm
                  </p>
                  <button
                    type="button"
                    onClick={() => refetch()}
                    className="mt-3 inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-xl px-3 text-xs font-bold text-rose-700 transition-colors hover:bg-rose-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 dark:text-rose-300 dark:hover:bg-rose-950/50"
                  >
                    <RefreshCw aria-hidden="true" className="h-4 w-4" />
                    Thử lại
                  </button>
                </div>
              ) : results.length === 0 ? (
                <div className="py-10 text-center">
                  <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-400 dark:bg-slate-800">
                    <Search aria-hidden="true" className="h-6 w-6" />
                  </div>
                  <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Không tìm thấy kết quả cho &quot;{debouncedQuery}&quot;
                  </p>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                    Hãy thử từ khóa ngắn hơn hoặc kiểm tra lỗi chính tả.
                  </p>
                </div>
              ) : (
                <div className="space-y-1.5">
                  {results.map((item, index) => (
                    <button
                      key={`${item.type}-${item.relevanceScore ?? index}-${index}`}
                      type="button"
                      onClick={() => handleNavigateItem(item)}
                      className="group flex min-h-14 w-full cursor-pointer items-start gap-3 rounded-xl border border-transparent p-3 text-left transition-colors hover:border-indigo-100 hover:bg-indigo-50/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:hover:border-indigo-900 dark:hover:bg-indigo-950/30"
                    >
                      <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-600 transition-colors group-hover:bg-indigo-100 group-hover:text-indigo-600 dark:bg-slate-800 dark:text-slate-300 dark:group-hover:bg-indigo-900/50 dark:group-hover:text-indigo-300">
                        {item.type === 'USER' && <User aria-hidden="true" className="h-4 w-4" />}
                        {item.type === 'POST' && <FileText aria-hidden="true" className="h-4 w-4" />}
                        {item.type === 'QUESTION' && <HelpCircle aria-hidden="true" className="h-4 w-4" />}
                        {item.type === 'SESSION' && <FolderKanban aria-hidden="true" className="h-4 w-4" />}
                        {item.type === 'SUBJECT' && <BookOpen aria-hidden="true" className="h-4 w-4" />}
                      </span>

                      <span className="min-w-0 flex-1">
                        {item.type === 'USER' && item.user && (
                          <span className="block">
                            <span className="flex items-center gap-2">
                              <span className="truncate text-sm font-bold text-slate-900 dark:text-slate-100">
                                {getUserDisplayName(item.user)}
                              </span>
                              {getUserIdentifier(item.user) && (
                                <span className="max-w-[45%] truncate rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                                  {getUserIdentifier(item.user)}
                                </span>
                              )}
                            </span>
                            <span className="mt-0.5 block truncate text-xs text-slate-500 dark:text-slate-400">
                              {item.user.schoolFaculty || item.user.major || item.user.role || 'Thành viên SQB'}
                            </span>
                          </span>
                        )}

                        {item.type === 'POST' && item.post && (
                          <span className="block">
                            <span className="mb-0.5 block text-xs font-semibold text-slate-500 dark:text-slate-400">
                              Bài viết từ {item.post.authorName || 'Thành viên SQB'}
                            </span>
                            <span className="line-clamp-2 text-sm font-medium text-slate-900 dark:text-slate-100">
                              {item.post.content}
                            </span>
                          </span>
                        )}

                        {item.type === 'QUESTION' && item.question && (
                          <span className="block">
                            <span className="mb-0.5 flex items-center gap-2">
                              {item.question.subjectName && (
                                <span className="rounded bg-blue-50 px-2 py-0.5 text-[10px] font-bold text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                                  {item.question.subjectName}
                                </span>
                              )}
                              {item.question.difficulty && (
                                <span className="text-[10px] font-medium text-slate-500">
                                  {item.question.difficulty}
                                </span>
                              )}
                            </span>
                            <span className="line-clamp-2 text-sm font-medium text-slate-900 dark:text-slate-100">
                              {item.question.content}
                            </span>
                          </span>
                        )}

                        {item.type === 'SESSION' && item.session && (
                          <span className="block">
                            <span className="mb-0.5 block text-xs font-semibold text-indigo-600 dark:text-indigo-400">
                              Phiên đóng góp câu hỏi
                            </span>
                            <span className="line-clamp-2 text-sm font-bold text-slate-900 dark:text-slate-100">
                              {item.session.title}
                            </span>
                          </span>
                        )}

                        {item.type === 'SUBJECT' && item.subject && (
                          <span className="block">
                            <span className="text-xs font-bold text-blue-600 dark:text-blue-400">
                              [{item.subject.subjectCode}]
                            </span>
                            <span className="block text-sm font-bold text-slate-900 dark:text-slate-100">
                              {item.subject.subjectName}
                            </span>
                          </span>
                        )}
                      </span>

                      <ArrowRight aria-hidden="true" className="mt-1 h-4 w-4 shrink-0 text-slate-300 transition-transform group-hover:translate-x-0.5 group-hover:text-indigo-500 dark:text-slate-600" />
                    </button>
                  ))}
                </div>
              )}
            </div>
          ) : isSavedTrendingError ? (
            <div
              role="alert"
              className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-center dark:border-rose-900/50 dark:bg-rose-950/20"
            >
              <p className="text-sm font-semibold text-rose-800 dark:text-rose-200">
                Không thể tải lịch sử tìm kiếm
              </p>
              <button
                type="button"
                onClick={() => refetchSavedTrending()}
                className="mt-3 inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-xl px-3 text-xs font-bold text-rose-700 transition-colors hover:bg-rose-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 dark:text-rose-300 dark:hover:bg-rose-950/50"
              >
                <RefreshCw aria-hidden="true" className="h-4 w-4" />
                Thử lại
              </button>
            </div>
          ) : (
            <div className="space-y-5">
              {savedSearches.length > 0 && (
                <section aria-labelledby="recent-searches-heading">
                  <h2
                    id="recent-searches-heading"
                    className="mb-2.5 flex items-center gap-1.5 px-1 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                  >
                    <History aria-hidden="true" className="h-3.5 w-3.5" />
                    Tìm kiếm gần đây
                  </h2>
                  <div className="flex flex-wrap gap-2">
                    {savedSearches.map((saved) => (
                      <div
                        key={saved.savedSearchId}
                        className="flex min-h-10 items-center overflow-hidden rounded-xl border border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/60"
                      >
                        <button
                          type="button"
                          onClick={() => handleSelectKeyword(saved.queryText)}
                          className="min-h-10 cursor-pointer px-3 text-xs font-medium text-slate-700 transition-colors hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-500 dark:text-slate-200 dark:hover:bg-slate-800"
                        >
                          {saved.queryText}
                        </button>
                        <button
                          type="button"
                          onClick={() => deleteSavedMutation.mutate(saved.savedSearchId)}
                          disabled={deleteSavedMutation.isPending}
                          className="inline-flex h-10 w-10 cursor-pointer items-center justify-center text-slate-400 transition-colors hover:bg-white hover:text-rose-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-500 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-slate-800 dark:hover:text-rose-400"
                          aria-label={`Xóa ${saved.queryText} khỏi lịch sử`}
                        >
                          <Trash2 aria-hidden="true" className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                </section>
              )}

              {trendingSearches.length > 0 && (
                <section aria-labelledby="trending-searches-heading">
                  <div className="mb-2.5 px-1">
                    <h2
                      id="trending-searches-heading"
                      className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                    >
                      <TrendingUp aria-hidden="true" className="h-3.5 w-3.5 text-amber-500" />
                      Thịnh hành trong SQB
                    </h2>
                    <p className="mt-1 text-[11px] text-slate-500 dark:text-slate-400">
                      Tổng hợp từ các lượt tìm kiếm đã xác nhận trong 7 ngày gần đây.
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {trendingSearches.map((trend, index) => (
                      <button
                        key={trend}
                        type="button"
                        onClick={() => handleSelectKeyword(trend)}
                        className="inline-flex min-h-10 cursor-pointer items-center gap-1.5 rounded-xl border border-indigo-100 bg-indigo-50/70 px-3 text-xs font-semibold text-indigo-700 transition-colors hover:bg-indigo-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:border-indigo-800/60 dark:bg-indigo-950/40 dark:text-indigo-300 dark:hover:bg-indigo-900/60"
                      >
                        <span className="font-bold text-amber-600 dark:text-amber-400">
                          #{index + 1}
                        </span>
                        {trend}
                      </button>
                    ))}
                  </div>
                </section>
              )}

              {savedSearches.length === 0 && trendingSearches.length === 0 && (
                <div className="py-10 text-center">
                  <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-400 dark:bg-slate-800">
                    <Search aria-hidden="true" className="h-6 w-6" />
                  </div>
                  <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Chưa có lịch sử tìm kiếm
                  </p>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                    Lịch sử chỉ được lưu sau khi bạn xác nhận một từ khóa.
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        {debouncedQuery && (
          <div className="flex items-center justify-between gap-3 border-t border-slate-100 bg-slate-50/70 p-3 dark:border-slate-800 dark:bg-slate-900/70">
            <span className="hidden text-xs text-slate-500 dark:text-slate-400 sm:inline">
              Nhấn{' '}
              <kbd className="rounded border bg-white px-1.5 py-0.5 font-mono text-[10px] dark:bg-slate-800">
                Enter
              </kbd>{' '}
              để xem đầy đủ
            </span>
            <button
              type="button"
              onClick={handleViewAllResults}
              className="ml-auto inline-flex min-h-10 cursor-pointer items-center gap-1.5 rounded-xl bg-indigo-600 px-4 text-xs font-bold text-white transition-colors hover:bg-indigo-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 active:scale-[0.98]"
            >
              Xem tất cả
              <ArrowRight aria-hidden="true" className="h-3.5 w-3.5" />
            </button>
          </div>
        )}
      </section>
    </div>
  );

  return createPortal(panel, document.body);
}
