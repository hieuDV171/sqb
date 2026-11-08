import React from 'react';
import { BookOpen, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface EmptySubmissionsProps {
  onProposeNew?: () => void;
  filtered?: boolean;
  hasNextPage?: boolean;
}

export const EmptySubmissions: React.FC<EmptySubmissionsProps> = ({
  onProposeNew,
  filtered = false,
  hasNextPage = false,
}) => {
  return (
    <div className="flex flex-col items-center rounded-2xl border border-border bg-card p-8 text-center sm:p-12">
      <div className="mb-4 grid size-12 place-items-center rounded-xl bg-academic-soft text-academic">
        <BookOpen aria-hidden="true" className="size-6" />
      </div>

      <h2 className="text-lg font-semibold">
        {filtered
          ? 'Không tìm thấy phiên nộp phù hợp'
          : 'Bạn chưa có phiên đề xuất câu hỏi nào'}
      </h2>

      <p className="mt-2 max-w-md text-sm text-muted-foreground">
        {hasNextPage ? 'Chưa có kết quả trong các phiên đã tải. Bạn có thể tải thêm để tiếp tục tìm.' : filtered
          ? 'Hãy thử thay đổi bộ lọc trạng thái hoặc môn học để tìm kiếm phiên đề xuất của bạn.'
          : 'Hãy đóng góp các câu hỏi trắc nghiệm chất lượng vào ngân hàng câu hỏi môn học của trường để nhận điểm thưởng và huy hiệu!'}
      </p>

      {!filtered && !hasNextPage && onProposeNew && (
        <Button
          onClick={onProposeNew}
          className="mt-5 min-h-11"
        >
          <Plus aria-hidden="true" />
          <span>Tạo phiên đề xuất mới</span>
        </Button>
      )}
    </div>
  );
};
