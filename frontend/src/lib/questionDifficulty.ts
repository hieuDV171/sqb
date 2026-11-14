export const questionDifficultyLabels = {
  EASY: 'Dễ',
  MEDIUM: 'Trung bình',
  HARD: 'Khó',
  UNCLASSIFIED: 'Chưa phân loại',
} as const;

export type QuestionDifficulty = keyof typeof questionDifficultyLabels;
