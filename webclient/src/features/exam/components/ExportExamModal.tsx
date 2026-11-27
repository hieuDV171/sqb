import { useState } from 'react';
import { Download, FileDown, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { FilterSelect } from '@/components/ui/filter-select';
import { useExportExam } from '@/features/exam';
import { ExamDialog } from './ExamDialog';

interface ExportExamModalProps {
  examId: number;
  examTitle?: string;
  onClose: () => void;
}

const formatOptions = [{ value: 'pdf', label: 'PDF' }, { value: 'excel', label: 'Excel' }];

export function ExportExamModal({ examId, examTitle, onClose }: ExportExamModalProps) {
  const [format, setFormat] = useState<'pdf' | 'excel'>('pdf');
  const [includeAnswerKey, setIncludeAnswerKey] = useState(true);
  const exportExam = useExportExam();
  const downloadUrl = exportExam.data?.data?.downloadUrl;

  return (
    <ExamDialog title="Xuất đề thi" description={examTitle || `Đề thi #${examId}`}
      icon={FileDown} className="max-w-md" busy={exportExam.isPending} onClose={onClose}>
      <form className="flex min-h-0 flex-col" onSubmit={(event) => {
        event.preventDefault();
        if (!exportExam.isPending) exportExam.mutate({ examId, params: { format, includeAnswerKey } });
      }}>
        <div className="min-h-0 space-y-3 overflow-y-auto p-4 sm:px-5">
          <fieldset disabled={exportExam.isPending} className="min-w-0 space-y-3">
            <FilterSelect label="Định dạng" value={format} options={formatOptions}
              disabled={exportExam.isPending} onChange={(value) => setFormat(value === 'excel' ? 'excel' : 'pdf')} />
            <label className="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-input px-3 py-2 text-sm">
              <input type="checkbox" checked={includeAnswerKey} onChange={(event) => setIncludeAnswerKey(event.target.checked)} className="size-4 shrink-0 accent-academic" />
              Kèm đáp án
            </label>
          </fieldset>
          {exportExam.isError && <p role="alert" className="text-sm text-destructive">{exportExam.error.message || 'Không thể xuất đề thi.'}</p>}
          {downloadUrl && (
            <p role="status" className="text-sm text-success">
              Tệp đã sẵn sàng.{' '}
              <a href={downloadUrl} download referrerPolicy="no-referrer" className="font-semibold underline underline-offset-2 focus-visible:outline-2 focus-visible:outline-ring">Tải lại</a>
            </p>
          )}
          {exportExam.isSuccess && !downloadUrl && <p role="alert" className="text-sm text-destructive">Không nhận được liên kết tải tệp.</p>}
        </div>
        <footer className="flex shrink-0 flex-wrap justify-end gap-2 border-t border-border p-4 sm:px-5">
          <Button type="button" variant="outline" disabled={exportExam.isPending} onClick={onClose}>Đóng</Button>
          <Button type="submit" disabled={exportExam.isPending}>
            {exportExam.isPending ? <Loader2 aria-hidden="true" className="motion-safe:animate-spin" /> : <Download aria-hidden="true" />}
            {exportExam.isPending ? 'Đang xuất…' : 'Xuất tệp'}
          </Button>
        </footer>
      </form>
    </ExamDialog>
  );
}
