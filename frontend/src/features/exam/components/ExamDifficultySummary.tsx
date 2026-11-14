import { examDifficulties } from '../examPresentation';
import type { Statistic } from '../types/exam.types';

export function ExamDifficultySummary({ statistic }: { statistic: Statistic }) {
  return (
    <div className="space-y-2 rounded-xl bg-surface-subtle p-3">
      <p className="text-sm font-medium">Phân bố độ khó</p>
      <div aria-hidden="true" className="flex h-1.5 overflow-hidden rounded-full bg-muted">
        {examDifficulties.map(({ field, count, bar }) => (
          <span key={field} className={bar} style={{ flexGrow: statistic[count] ?? 0 }} />
        ))}
      </div>
      <ul className="flex flex-wrap gap-1.5">
        {examDifficulties.map(({ field, count, label, tone }) => (
          <li key={field} className={`rounded-md px-2 py-0.5 text-xs font-medium ${tone}`}>
            {label} · {statistic[count] ?? 0}
          </li>
        ))}
      </ul>
    </div>
  );
}
