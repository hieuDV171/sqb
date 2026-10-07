import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  Search,
  User,
  FileText,
  HelpCircle,
  FolderKanban,
  BookOpen,
  ArrowRight,
  RefreshCw,
  SlidersHorizontal,
  GraduationCap,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import { useGlobalSearch, type SearchType, type SearchResultItemDto } from '@/features/search';

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const queryParam = searchParams.get('q') || '';
  const typeParam = (searchParams.get('type') as SearchType) || 'ALL';

  const [inputQuery, setInputQuery] = useState(queryParam);
  const [selectedType, setSelectedType] = useState<SearchType>(typeParam);
  const [cursor, setCursor] = useState<number | undefined>(undefined);

  // Sync state when URL params change
  useEffect(() => {
    setInputQuery(queryParam);
    setSelectedType(typeParam);
    setCursor(undefined);
  }, [queryParam, typeParam]);

  const {
    data: searchData,
    isLoading,
    isFetching,
    refetch,
  } = useGlobalSearch({
    query: queryParam,
    type: selectedType,
    after: cursor,
    limit: 15,
    enabled: !!queryParam.trim(),
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputQuery.trim()) {
      setSearchParams({ q: inputQuery.trim(), type: selectedType });
    }
  };

  const handleTabChange = (type: SearchType) => {
    setSelectedType(type);
    setCursor(undefined);
    setSearchParams({ q: queryParam, type });
  };

  const items: SearchResultItemDto[] = searchData?.items || [];
  const totalHits = searchData?.totalHits ?? items.length;

  const tabs: { key: SearchType; label: string; icon: typeof User }[] = [
    { key: 'ALL', label: 'Tất cả', icon: SlidersHorizontal },
    { key: 'POST', label: 'Bài viết', icon: FileText },
    { key: 'USER', label: 'Người dùng', icon: User },
    { key: 'QUESTION', label: 'Câu hỏi ôn tập', icon: HelpCircle },
    { key: 'SESSION', label: 'Phiên đề xuất', icon: FolderKanban },
    { key: 'SUBJECT', label: 'Môn học', icon: BookOpen },
  ];

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Header & Search Bar Box */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 sm:p-8 shadow-sm">
        <div className="max-w-2xl mx-auto text-center space-y-3 mb-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 text-xs font-semibold">
            <Search className="w-3.5 h-3.5" />
            Tra Cứu Toàn Cục SQB
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
            Tìm Kiếm Đa Thực Thể
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
            Tra cứu nhanh bài viết chia sẻ, sinh viên & giảng viên, ngân hàng câu hỏi và môn học trong trường.
          </p>
        </div>

        {/* Input Form */}
        <form onSubmit={handleSearchSubmit} className="max-w-2xl mx-auto">
          <div className="relative flex items-center shadow-md rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-500/20 transition-all">
            <Search className="w-5 h-5 text-slate-400 dark:text-slate-500 ml-4 shrink-0" />
            <input
              type="text"
              value={inputQuery}
              onChange={(e) => setInputQuery(e.target.value)}
              placeholder="Nhập từ khóa tìm kiếm..."
              className="w-full px-4 py-3.5 text-sm sm:text-base bg-transparent text-slate-900 dark:text-slate-100 placeholder:text-slate-400 outline-none"
            />
            <button
              type="submit"
              className="px-6 py-3.5 m-1 rounded-xl font-bold text-xs sm:text-sm text-white bg-indigo-600 hover:bg-indigo-700 transition-colors shrink-0 cursor-pointer"
            >
              Tìm kiếm
            </button>
          </div>
        </form>
      </div>

      {/* Tabs Toolbar */}
      {queryParam && (
        <div className="flex flex-wrap items-center justify-between gap-3 bg-white dark:bg-slate-900 p-2 sm:p-3 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto pb-1 sm:pb-0">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => handleTabChange(tab.key)}
                  className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-bold rounded-xl transition-all whitespace-nowrap cursor-pointer ${
                    selectedType === tab.key
                      ? 'bg-indigo-600 text-white shadow-xs shadow-indigo-500/20'
                      : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  {tab.label}
                </button>
              );
            })}
          </div>

          <div className="flex items-center gap-3 ml-auto">
            <span className="text-xs text-slate-500 dark:text-slate-400 hidden sm:inline">
              Tìm thấy: <strong>{totalHits}</strong> kết quả
            </span>
            <button
              type="button"
              onClick={() => refetch()}
              disabled={isFetching}
              className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              title="Làm mới"
            >
              <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      )}

      {/* Results Content */}
      {!queryParam.trim() ? (
        <EmptyState
          icon={Search}
          title="Bắt đầu tìm kiếm thông tin"
          description="Nhập từ khóa về môn học, bài viết bạn quan tâm hoặc tên bạn bè để tra cứu toàn bộ dữ liệu hệ thống."
        />
      ) : isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 animate-pulse flex items-start gap-4"
            >
              <div className="w-12 h-12 rounded-xl bg-slate-200 dark:bg-slate-800 shrink-0" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded w-1/4" />
                <div className="h-4 bg-slate-100 dark:bg-slate-800/60 rounded w-3/4" />
              </div>
            </div>
          ))}
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          icon={Search}
          title={`Không tìm thấy kết quả nào cho "${queryParam}"`}
          description="Hãy thử thay đổi loại bộ lọc danh mục hoặc sử dụng các từ khóa học phần khác."
        />
      ) : (
        <div className="space-y-3">
          {items.map((item, index) => (
            <div
              key={index}
              className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 hover:border-indigo-300 dark:hover:border-indigo-700/60 p-5 shadow-xs hover:shadow-md transition-all flex flex-col justify-between"
            >
              {/* Type Badge & Info */}
              {item.type === 'USER' && item.user && (
                <div className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    <div className="w-12 h-12 rounded-full overflow-hidden border border-slate-200 dark:border-slate-700 bg-slate-100 shrink-0">
                      {item.user.avatarUrl ? (
                        <img
                          src={item.user.avatarUrl}
                          alt={item.user.username}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center font-bold text-slate-500">
                          {item.user.username.charAt(0).toUpperCase()}
                        </div>
                      )}
                    </div>

                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-bold text-base text-slate-900 dark:text-slate-100">
                          {item.user.fullName || item.user.username}
                        </h3>
                        <Badge variant="role">
                          {item.user.role === 'LECTURER' ? 'Giảng viên' : 'Sinh viên'}
                        </Badge>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                        @{item.user.username} • {item.user.schoolFaculty || 'Đại học Bách Khoa Hà Nội'}
                      </p>
                    </div>
                  </div>

                  <Link
                    to={`/users/${item.user.userId}`}
                    className="px-4 py-2 text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 rounded-xl transition-colors inline-flex items-center gap-1"
                  >
                    Trang cá nhân <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              )}

              {item.type === 'POST' && item.post && (
                <div>
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                        Bài viết
                      </span>
                      <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                        {item.post.authorName}
                      </span>
                    </div>
                    {item.post.createdAt && (
                      <span className="text-xs text-slate-400">
                        {new Date(item.post.createdAt).toLocaleDateString('vi-VN')}
                      </span>
                    )}
                  </div>

                  <p className="text-sm text-slate-800 dark:text-slate-200 leading-relaxed line-clamp-3">
                    {item.post.content}
                  </p>

                  <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <div className="flex items-center gap-4 text-xs text-slate-500">
                      <span>{item.post.reactCount || 0} cảm xúc</span>
                      <span>{item.post.commentCount || 0} bình luận</span>
                    </div>
                    <Link
                      to={`/feed?postId=${item.post.postId}`}
                      className="text-xs font-bold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 inline-flex items-center gap-1"
                    >
                      Xem chi tiết bài viết <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              )}

              {item.type === 'QUESTION' && item.question && (
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300">
                      Câu hỏi trắc nghiệm
                    </span>
                    {item.question.subjectName && (
                      <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                        {item.question.subjectName}
                      </span>
                    )}
                    {item.question.difficulty && (
                      <Badge variant="outline" className="text-[10px]">
                        {item.question.difficulty}
                      </Badge>
                    )}
                  </div>

                  <p className="text-sm font-medium text-slate-900 dark:text-slate-100 line-clamp-3">
                    {item.question.content}
                  </p>

                  <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <span className="text-xs text-slate-500">
                      {item.question.totalAnswers ? `${item.question.totalAnswers} lượt làm` : 'Câu hỏi đã kiểm duyệt'}
                    </span>
                    <Link
                      to={`/sessions?questionId=${item.question.questionId}`}
                      className="text-xs font-bold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 inline-flex items-center gap-1"
                    >
                      Luyện tập câu này <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              )}

              {item.type === 'SESSION' && item.session && (
                <div>
                  <div className="flex items-center gap-2 mb-1.5">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-purple-50 text-purple-700 dark:bg-purple-900/40 dark:text-purple-300">
                      Phiên đề xuất
                    </span>
                    <span className="text-xs text-slate-500">
                      Môn: {item.session.subjectName || 'Đang cập nhật'}
                    </span>
                  </div>

                  <h3 className="font-bold text-base text-slate-900 dark:text-slate-100">
                    {item.session.title}
                  </h3>

                  <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <span className="text-xs text-slate-500">
                      {item.session.questionCount || 0} câu hỏi trong phiên
                    </span>
                    <Link
                      to={`/sessions/${item.session.sessionId}`}
                      className="text-xs font-bold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 inline-flex items-center gap-1"
                    >
                      Mở phiên đề xuất <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              )}

              {item.type === 'SUBJECT' && item.subject && (
                <div className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center">
                      <GraduationCap className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800 dark:bg-blue-900/60 dark:text-blue-300">
                          {item.subject.code}
                        </span>
                        <h3 className="font-bold text-sm sm:text-base text-slate-900 dark:text-slate-100">
                          {item.subject.name}
                        </h3>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                        {item.subject.department || 'Bộ môn phụ trách'} • {item.subject.creditCount || 3} tín chỉ
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      setInputQuery(item.subject?.name || '');
                      setSelectedType('QUESTION');
                      setSearchParams({ q: item.subject?.name || '', type: 'QUESTION' });
                    }}
                    className="px-4 py-2 text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 rounded-xl transition-colors inline-flex items-center gap-1 cursor-pointer"
                  >
                    Xem câu hỏi môn này <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Pagination Load More */}
      {searchData?.pagination?.hasNext && (
        <div className="flex justify-center pt-4">
          <button
            type="button"
            onClick={() => {
              if (items.length > 0) setCursor(items.length);
            }}
            disabled={isFetching}
            className="px-6 py-2.5 rounded-xl font-semibold text-xs border border-slate-200 dark:border-slate-700 hover:bg-white dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition-colors flex items-center gap-2 cursor-pointer"
          >
            {isFetching ? 'Đang tải thêm...' : 'Tải thêm kết quả'}
          </button>
        </div>
      )}
    </div>
  );
}
