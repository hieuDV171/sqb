import { useId } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Label } from '@/components/ui/label';
import { topicCatalog } from '../services/topicService';

interface QuestionTopicSelectProps {
  subjectId: number | undefined;
  value?: number | null;
  currentName?: string | null;
  disabled?: boolean;
  onChange: (value: number | null) => void;
}

export function QuestionTopicSelect({ subjectId, value, currentName, disabled, onChange }: QuestionTopicSelectProps) {
  const id = useId();
  const catalog = useQuery(topicCatalog(subjectId ?? 0));
  const options = catalog.data ?? [];
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>Chủ đề</Label>
      <select id={id} value={value ?? ''} disabled={disabled || !subjectId || catalog.isPending || catalog.isError}
        onChange={(event) => onChange(event.target.value ? Number(event.target.value) : null)}
        className="min-h-11 w-full rounded-xl border border-input bg-background px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-ring disabled:opacity-60">
        <option value="">{subjectId && catalog.isLoading ? 'Đang tải chủ đề…' : 'Chưa có chủ đề phù hợp'}</option>
        {value != null && !options.some((topic) => topic.id === value) && (
          <option value={value}>{currentName ?? 'Chủ đề đã chọn'}</option>
        )}
        {options.map((topic) => <option key={topic.id} value={topic.id}>{topic.name}</option>)}
      </select>
      {catalog.isError && <p role="status" className="text-sm text-muted-foreground">
        Không tải được chủ đề.{' '}
        <button type="button" className="font-medium underline" onClick={() => void catalog.refetch()}>Thử lại</button>
      </p>}
    </div>
  );
}
