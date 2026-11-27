import { useQuery } from '@tanstack/react-query';
import { FilterSelect } from '@/components/ui/filter-select';
import { topicCatalog } from '../services/topicService';

interface QuestionTopicSelectProps {
  subjectId: number | undefined;
  value?: number | null;
  currentName?: string | null;
  disabled?: boolean;
  onChange: (value: number | null) => void;
}

export function QuestionTopicSelect({ subjectId, value, currentName, disabled, onChange }: QuestionTopicSelectProps) {
  const catalog = useQuery(topicCatalog(subjectId ?? 0));
  const topics = catalog.data ?? [];
  const options = [{
    value: '',
    label: subjectId && catalog.isLoading ? 'Đang tải chủ đề…' : 'Chưa có chủ đề phù hợp',
  }];
  if (value != null && !topics.some((topic) => topic.id === value)) {
    options.push({ value: String(value), label: currentName ?? 'Chủ đề đã chọn' });
  }
  options.push(...topics.map((topic) => ({ value: String(topic.id), label: topic.name })));

  return (
    <div className="space-y-1.5">
      <FilterSelect label="Chủ đề" value={value == null ? '' : String(value)} options={options}
        disabled={disabled || !subjectId || catalog.isPending || catalog.isError}
        onChange={(next) => onChange(next === '' ? null : Number(next))} />
      {catalog.isError && <p role="status" className="text-sm text-muted-foreground">
        Không tải được chủ đề.{' '}
        <button type="button" className="font-medium underline" onClick={() => void catalog.refetch()}>Thử lại</button>
      </p>}
    </div>
  );
}
