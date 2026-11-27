import { Component, type ReactNode } from 'react';
import { useLocation, useSearchParams, Link } from 'react-router-dom';
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
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { UserAvatar } from '@/components/ui/user-avatar';
import { cn } from '@/lib/utils';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import {
  useInfiniteGlobalSearch,
  useRecordSearch,
  type SearchType,
  type SearchResultItemDto,
  type UserSearchResultDto,
} from '@/features/search';

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

const resultMetaClass = 'flex flex-wrap items-center gap-x-3 gap-y-2 text-sm text-muted-foreground';
const resultFooterClass = 'mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-border pt-3 text-sm text-muted-foreground';
const resultTitleClass = 'break-words text-lg font-semibold text-foreground sm:text-xl';
const resultActionClass = 'min-h-11 text-primary';

function SearchPageContent() {
  const [searchParams, setSearchParams] = useSearchParams();

  const queryParam = searchParams.get('q') || '';
  const selectedType = (searchParams.get('type') as SearchType) || 'ALL';

  const {
    data: searchData,
    isLoading,
    isFetching,
    isLoadingError,
    isFetchingNextPage,
    isFetchNextPageError,
    hasNextPage,
    fetchNextPage,
    refetch,
  } = useInfiniteGlobalSearch(queryParam, selectedType);
  const recordSearchMutation = useRecordSearch();

  const items: SearchResultItemDto[] = searchData?.pages.flatMap((page) => page?.items ?? []) ?? [];
  const totalHits = searchData?.pages[0]?.totalHits ?? items.length;
  const { sentinel, supportsAutoLoad } = useInfiniteScroll(
    hasNextPage && !isFetching && !isFetchNextPageError && items.length > 0,
    fetchNextPage,
    searchData?.pages.length ?? 0,
  );

  const handleTabChange = (type: SearchType) => {
    setSearchParams({ q: queryParam, type });
  };

  const recordCurrentQuery = () => {
    const confirmedQuery = queryParam.trim();
    if (confirmedQuery.length >= 2) {
      recordSearchMutation.mutate(confirmedQuery);
    }
  };

  const tabs: { key: SearchType; label: string; icon: typeof User }[] = [
    { key: 'ALL', label: 'Tất cả', icon: SlidersHorizontal },
    { key: 'POST', label: 'Bài viết', icon: FileText },
    { key: 'USER', label: 'Người dùng', icon: User },
    { key: 'QUESTION', label: 'Câu hỏi ôn tập', icon: HelpCircle },
    { key: 'SESSION', label: 'Phiên đề xuất', icon: FolderKanban },
    { key: 'SUBJECT', label: 'Môn học', icon: BookOpen },
  ];

  return (
    <div className="mx-auto max-w-5xl space-y-5 sm:space-y-6">
      <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Kết quả tìm kiếm</h1>

      {/* Tabs Toolbar */}
      {queryParam && (
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-border bg-card p-3">
          <div role="group" aria-label="Loại kết quả tìm kiếm" className="flex flex-wrap items-center gap-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <Button
                  key={tab.key}
                  type="button"
                  variant="ghost"
                  aria-pressed={selectedType === tab.key}
                  onClick={() => handleTabChange(tab.key)}
                  className={cn('min-h-11 px-3', selectedType === tab.key
                    ? 'bg-academic-soft text-academic hover:bg-academic-soft hover:text-academic'
                    : 'text-muted-foreground')}
                >
                  <Icon aria-hidden="true" />
                  {tab.label}
                </Button>
              );
            })}
          </div>

          <div className="ml-auto flex items-center gap-2">
            {!isLoading && !isLoadingError && (
              <span role="status" className="text-sm text-muted-foreground">
                <strong className="text-foreground">{totalHits}</strong> kết quả
              </span>
            )}
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => void refetch()}
              disabled={isFetching}
              className="size-11 text-muted-foreground"
              title="Làm mới"
              aria-label="Làm mới kết quả tìm kiếm"
            >
              <RefreshCw aria-hidden="true" className={cn(isFetching && 'motion-safe:animate-spin')} />
            </Button>
          </div>
        </div>
      )}

      {/* Results Content */}
      {!queryParam.trim() ? (
        <EmptyState
          icon={Search}
          title="Bắt đầu tìm kiếm thông tin"
          description="Nhập từ khóa vào ô tìm kiếm ở thanh trên cùng."
        />
      ) : isLoading ? (
        <div className="space-y-4" aria-busy="true" aria-label="Đang tải kết quả tìm kiếm">
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="space-y-3 rounded-2xl border border-border bg-card p-5"
            >
              <Skeleton className="h-5 w-32" />
              <Skeleton className="h-6 w-2/3" />
              <Skeleton className="h-4 w-1/3" />
            </div>
          ))}
        </div>
      ) : isLoadingError ? (
        <ErrorState
          title="Không thể tải kết quả tìm kiếm"
          message="Máy chủ tìm kiếm hiện chưa phản hồi. Vui lòng thử lại."
          onRetry={() => refetch()}
        />
      ) : items.length === 0 ? (
        <EmptyState
          icon={Search}
          title={hasNextPage ? 'Trang kết quả này chưa có mục phù hợp' : `Không tìm thấy kết quả nào cho "${queryParam}"`}
          description={hasNextPage ? 'Bạn có thể tải thêm để xem các kết quả tiếp theo.' : 'Hãy thử thay đổi loại bộ lọc danh mục hoặc sử dụng các từ khóa học phần khác.'}
        />
      ) : (
        <div className="space-y-4">
          {items.map((item, index) => (
            <article
              key={index}
              className="rounded-2xl border border-border bg-card p-4 sm:p-5"
            >
              {/* Type Badge & Info */}
              {item.type === 'USER' && item.user && (
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <UserAvatar name={getUserDisplayName(item.user)} avatarUrl={item.user.avatarUrl} className="size-12" decorative />
                    <div className="min-w-0">
                      <div className={resultMetaClass}>
                        <h2 className={resultTitleClass}>
                          {getUserDisplayName(item.user)}
                        </h2>
                        <Badge variant="role">
                          {item.user.role === 'LECTURER' ? 'Giảng viên' : 'Sinh viên'}
                        </Badge>
                      </div>
                      <p className="mt-1 break-words text-sm text-muted-foreground">
                        {getUserIdentifier(item.user) || 'Thành viên SQB'} • {item.user.schoolFaculty || item.user.major || 'Đại học Bách Khoa Hà Nội'}
                      </p>
                    </div>
                  </div>

                  <Button variant="ghost" nativeButton={false}
                    render={<Link to={`/users/${item.user.userId}`} />}
                    onClick={recordCurrentQuery}
                    className={resultActionClass}
                  >
                    Trang cá nhân <ArrowRight aria-hidden="true" />
                  </Button>
                </div>
              )}

              {item.type === 'POST' && item.post && (
                <div>
                  <div className={cn(resultMetaClass, 'mb-3 justify-between')}>
                    <div className={resultMetaClass}>
                      <Badge variant="outline">Bài viết</Badge>
                      <span className="font-semibold text-foreground">
                        {item.post.authorName}
                      </span>
                    </div>
                    {item.post.createdAt && (
                      <time dateTime={item.post.createdAt}>
                        {new Date(item.post.createdAt).toLocaleDateString('vi-VN')}
                      </time>
                    )}
                  </div>

                  <p className="line-clamp-3 break-words text-base leading-relaxed text-foreground">
                    {item.post.content}
                  </p>

                  <div className={resultFooterClass}>
                    <div className={resultMetaClass}>
                      {item.post.reactCount != null && <span>{item.post.reactCount} cảm xúc</span>}
                      {item.post.commentCount != null && <span>{item.post.commentCount} bình luận</span>}
                    </div>
                    <Button variant="ghost" nativeButton={false}
                      render={<Link to={`/posts/${item.post.postId}`} />}
                      onClick={recordCurrentQuery}
                      className={resultActionClass}
                    >
                      Xem chi tiết bài viết <ArrowRight aria-hidden="true" />
                    </Button>
                  </div>
                </div>
              )}

              {item.type === 'QUESTION' && item.question && (
                <div>
                  <div className={cn(resultMetaClass, 'mb-3')}>
                    <Badge className="bg-academic-soft text-academic hover:bg-academic-soft">Câu hỏi trắc nghiệm</Badge>
                    {item.question.subjectName && (
                      <span>{item.question.subjectName}</span>
                    )}
                    {item.question.difficulty && (
                      <Badge variant="outline">{item.question.difficulty}</Badge>
                    )}
                  </div>

                  <p className="line-clamp-3 break-words text-base font-medium leading-relaxed text-foreground">
                    {item.question.content}
                  </p>

                  <div className={resultFooterClass}>
                    <span>
                      {item.question.ratingCount ? `${item.question.ratingCount} lượt đánh giá` : 'Chưa có đánh giá'}
                    </span>
                    {item.question.sessionId && (
                      <Button variant="ghost" nativeButton={false}
                        render={<Link to={`/sessions/${item.question.sessionId}/practice?questionId=${item.question.questionId}`} />}
                        onClick={recordCurrentQuery}
                        className={resultActionClass}
                      >
                        Luyện tập câu này <ArrowRight aria-hidden="true" />
                      </Button>
                    )}
                  </div>
                </div>
              )}

              {item.type === 'SESSION' && item.session && (
                <div>
                  <div className={cn(resultMetaClass, 'mb-3')}>
                    <Badge className="bg-academic-soft text-academic hover:bg-academic-soft">Phiên đề xuất</Badge>
                    {item.session.subjectName && <span>{item.session.subjectName}</span>}
                    {item.session.status === 'REVIEWING' && <Badge variant="info">Đang xem xét</Badge>}
                    {item.session.status === 'PENDING' && <Badge variant="warning">Chờ duyệt</Badge>}
                  </div>

                  <h2 className={resultTitleClass}>
                    {item.session.title}
                  </h2>
                  {item.session.sessionCode && (
                    <p className="mt-1 wrap-break-word font-mono text-sm text-muted-foreground">#{item.session.sessionCode}</p>
                  )}

                  <div className={resultFooterClass}>
                    <span>{item.session.questionCount ?? 0} câu hỏi trong phiên</span>
                    <Button variant="ghost" nativeButton={false}
                      render={<Link to={item.session.sessionCode && item.session.status !== 'RESOLVED'
                        ? `/sessions/${item.session.sessionId}`
                        : `/sessions/${item.session.sessionId}/practice`} />}
                      onClick={recordCurrentQuery}
                      className={resultActionClass}
                    >
                      {item.session.sessionCode && item.session.status !== 'RESOLVED' ? 'Xem chi tiết phiên' : 'Luyện tập phiên này'}
                      <ArrowRight aria-hidden="true" />
                    </Button>
                  </div>
                </div>
              )}

              {item.type === 'SUBJECT' && item.subject && (
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic">
                      <GraduationCap aria-hidden="true" className="size-5" />
                    </div>
                    <div className="min-w-0">
                      <div className={resultMetaClass}>
                        <Badge variant="outline">{item.subject.subjectCode}</Badge>
                        <h2 className={resultTitleClass}>
                          {item.subject.subjectName}
                        </h2>
                      </div>
                      <p className="mt-1 text-sm text-muted-foreground">
                        Môn học trong ngân hàng câu hỏi SQB
                      </p>
                    </div>
                  </div>

                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => {
                      const subjectName = item.subject?.subjectName || '';
                      recordCurrentQuery();
                      setSearchParams({ q: subjectName, type: 'QUESTION' });
                    }}
                    className={resultActionClass}
                  >
                    Xem câu hỏi môn này <ArrowRight aria-hidden="true" />
                  </Button>
                </div>
              )}
            </article>
          ))}
        </div>
      )}

      {hasNextPage && (
        <div className="space-y-2 text-center">
          <div ref={sentinel} aria-hidden="true" className="h-px" />
          {isFetchNextPageError && <p role="status" className="text-sm text-destructive">Không tải được trang kết quả tiếp theo.</p>}
          {isFetchingNextPage ? (
            <p role="status" className="inline-flex items-center gap-2 text-sm text-muted-foreground">
              <RefreshCw aria-hidden="true" className="size-4 motion-safe:animate-spin" /> Đang tải thêm kết quả...
            </p>
          ) : (!supportsAutoLoad || isFetchNextPageError || items.length === 0) && (
            <Button variant="outline" onClick={() => void fetchNextPage({ cancelRefetch: false })} disabled={isFetching} className="min-h-11">
              {isFetchNextPageError ? 'Thử tải lại' : 'Tải thêm kết quả'}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

class SearchRenderBoundary extends Component<
  { children: ReactNode },
  { hasError: boolean }
> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <ErrorState
          title="Trang tìm kiếm gặp sự cố"
          message="Dữ liệu kết quả chưa đúng định dạng hoặc giao diện không thể hiển thị. Hãy thử tải lại trang tìm kiếm."
          onRetry={() => this.setState({ hasError: false })}
        />
      );
    }

    return this.props.children;
  }
}

export function SearchPage() {
  const location = useLocation();

  return (
    <SearchRenderBoundary key={`${location.pathname}${location.search}`}>
      <SearchPageContent />
    </SearchRenderBoundary>
  );
}
