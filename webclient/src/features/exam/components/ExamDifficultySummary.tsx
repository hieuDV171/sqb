import { examDifficulties, examPercentFormat } from '../examPresentation';
import type { DifficultyDistribution, Statistic } from '../types/exam.types';

export function ExamDifficultySummary({ statistic, requested }: { statistic: Statistic; requested?: DifficultyDistribution | null }) {
  return (
    <div className="space-y-2 rounded-xl bg-surface-subtle p-3">
      <p className="text-sm font-medium">{requested ? 'Ma trận độ khó' : 'Phân bố độ khó'}</p>
      {requested && <p className="text-xs text-muted-foreground">Tỷ lệ yêu cầu và số câu thực tế khi tạo đề</p>}
      <div aria-hidden="true" className="flex h-1.5 overflow-hidden rounded-full bg-muted">
        {examDifficulties.map(({ field, count, bar }) => (
          <span key={field} className={bar} style={{ flexGrow: statistic[count] ?? 0 }} />
        ))}
      </div>
      <ul className="flex flex-wrap gap-1.5">
        {examDifficulties.map(({ field, count, label, tone }) => (
          <li key={field} className={`rounded-md px-2 py-0.5 text-xs font-medium ${tone}`}>
            {label} · {requested && `${examPercentFormat.format(requested[field] ?? 0)} · `}{statistic[count] ?? 0} câu
          </li>
        ))}
      </ul>
    </div>
  );
}
