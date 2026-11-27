import { FilterSelect } from '@/components/ui/filter-select';
import { questionDifficultyLabels, type QuestionDifficulty } from '@/lib/questionDifficulty';

const options = Object.entries(questionDifficultyLabels).map(([value, label]) => ({ value, label }));

interface QuestionDifficultySelectProps {
  label: string;
  value?: QuestionDifficulty | null;
  disabled?: boolean;
  onChange: (value: QuestionDifficulty) => void;
}

export function QuestionDifficultySelect({ label, value, disabled, onChange }: QuestionDifficultySelectProps) {
  return (
    <FilterSelect label={label} value={value ?? 'UNCLASSIFIED'} options={options} disabled={disabled}
      onChange={(next) => onChange(next as QuestionDifficulty)} />
  );
}
