import type { CourseClassResponse } from '@/features/course-class/types/courseClass.types';
import { questionDifficultyLabels } from '@/lib/questionDifficulty';
import type { DifficultyDto, DifficultyDistribution, Statistic } from './types/exam.types';

export const examDifficulties = [
  { value: 'EASY', field: 'easy', count: 'easyCount', label: questionDifficultyLabels.EASY, tone: 'bg-success-soft text-success', bar: 'bg-success' },
  { value: 'MEDIUM', field: 'medium', count: 'mediumCount', label: questionDifficultyLabels.MEDIUM, tone: 'bg-warning-soft text-warning', bar: 'bg-warning' },
  { value: 'HARD', field: 'hard', count: 'hardCount', label: questionDifficultyLabels.HARD, tone: 'bg-destructive/10 text-destructive', bar: 'bg-destructive' },
  { value: 'UNCLASSIFIED', field: 'unclassified', count: 'unclassifiedCount', label: questionDifficultyLabels.UNCLASSIFIED, tone: 'bg-muted text-muted-foreground', bar: 'bg-muted-foreground' },
] as const satisfies ReadonlyArray<{
  value: DifficultyDto;
  field: keyof DifficultyDistribution;
  count: keyof Statistic;
  label: string;
  tone: string;
  bar: string;
}>;

export function getExamSubjects(classes: CourseClassResponse[] = []) {
  const subjects = new Map<number, { id: number; label: string }>();
  for (const { subjectId, subjectCode, subjectName } of classes) {
    if (!subjects.has(subjectId)) {
      subjects.set(subjectId, { id: subjectId, label: `[${subjectCode}] ${subjectName}` });
    }
  }
  return [...subjects.values()];
}
