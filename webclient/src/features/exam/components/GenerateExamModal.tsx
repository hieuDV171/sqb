import { useId, useState, type SubmitEvent } from 'react';
import { FilePlus2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { FilterSelect } from '@/components/ui/filter-select';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { examDifficulties, getExamSubjects } from '../examPresentation';
import { useExamBankSummary, useGenerateExam, useLecturerClasses } from '../hooks/useExam';
import { ExamDialog } from './ExamDialog';
import { maxQuestionCount, QuestionCountInput } from './QuestionCountInput';

interface GenerateExamModalProps {
  initialClassId?: number;
  initialSubjectId?: number;
  onClose: () => void;
  onSuccess: (examId: number) => void;
}

const defaultPercentages = { easy: '30', medium: '50', hard: '20', unclassified: '0' };
const untaggedTopicKey = 'untagged';
const invalidPercentage = (value: string) =>
  value === '' || !Number.isInteger(Number(value)) || Number(value) < 0 || Number(value) > 100;

export function GenerateExamModal({ initialClassId, initialSubjectId, onClose, onSuccess }: GenerateExamModalProps) {
  const id = useId();
  const classes = useLecturerClasses();
  const generate = useGenerateExam();
  const [subjectId, setSubjectId] = useState<number>();
  const [classIds, setClassIds] = useState<number[]>();
  const [title, setTitle] = useState('');
  const [questionCount, setQuestionCount] = useState('40');
  const [percentages, setPercentages] = useState(defaultPercentages);
  const [useTopicMatrix, setUseTopicMatrix] = useState(false);
  const [topicPercentages, setTopicPercentages] = useState<Record<string, string>>({});
  const [shuffleOptions, setShuffleOptions] = useState(false);
  const [includeAnswerKey, setIncludeAnswerKey] = useState(false);

  const subjects = getExamSubjects(classes.data);
  const initialClass = classes.data?.find((courseClass) => courseClass.courseClassId === initialClassId);
  const selectedId = subjectId ?? initialClass?.subjectId ?? initialSubjectId;
  const selectedSubject = subjects.find((subject) => subject.id === selectedId);
  const bankQuery = useExamBankSummary(selectedSubject?.id);
  const bank = bankQuery.data;
  const availableClasses = classes.data?.filter((courseClass) => courseClass.subjectId === selectedSubject?.id) ?? [];
  const selectedClasses = availableClasses.filter((courseClass) =>
    classIds ? classIds.includes(courseClass.courseClassId) : courseClass.courseClassId === initialClass?.courseClassId,
  );
  const total = Object.values(percentages).reduce((sum, value) => sum + Number(value), 0);
  const validMatrix = total === 100 && !Object.values(percentages).some(invalidPercentage);
  const weightedTopics = bank?.topics.map((topic) => {
    const key = topic.topicId?.toString() ?? untaggedTopicKey;
    return { ...topic, key, percentage: topicPercentages[key] ?? '0' };
  }) ?? [];
  const topicTotal = weightedTopics.reduce((sum, { percentage }) => sum + Number(percentage), 0);
  const validTopics = !useTopicMatrix || (topicTotal === 100 && !weightedTopics.some(({ percentage }) => invalidPercentage(percentage)));
  const count = Number(questionCount);
  const validCount = Number.isInteger(count) && count >= 1 && count <= maxQuestionCount;
  const invalidCount = questionCount !== '' && !validCount;
  const bankTooSmall = bank != null && validCount && count > bank.totalQuestions;
  const canGenerate = !!bank && selectedClasses.length > 0 && validCount && !bankTooSmall && validMatrix && validTopics;

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedSubject || !canGenerate || generate.isPending) return;
    generate.mutate({
      subjectId: selectedSubject.id,
      title: title.trim() || undefined,
      questionCount: count,
      difficultyDistribution: {
        easy: Number(percentages.easy) / 100,
        medium: Number(percentages.medium) / 100,
        hard: Number(percentages.hard) / 100,
        unclassified: Number(percentages.unclassified) / 100,
      },
      topicWeights: useTopicMatrix ? Object.fromEntries(weightedTopics.filter(({ percentage }) => Number(percentage) > 0)
        .map(({ key, percentage }) => [key, Number(percentage) / 100])) : undefined,
      shuffleOptions,
      includeAnswerKey,
      classIds: selectedClasses.map((courseClass) => courseClass.courseClassId),
    }, { onSuccess: (response) => onSuccess(response.data.examId) });
  }

  return (
    <ExamDialog title="Tạo đề thi" description="Chọn môn, lớp học phần và ma trận cho đề thi."
      icon={FilePlus2} busy={generate.isPending} onClose={onClose}>
      <form onSubmit={submit} className="flex min-h-0 min-w-0 flex-col">
        <div className="min-h-0 overflow-y-auto">
          <fieldset disabled={generate.isPending} className="min-w-0 space-y-5 p-4 sm:p-5">
            {classes.isPending ? (
              <Skeleton role="status" aria-label="Đang tải môn học" className="h-16 rounded-xl" />
            ) : !classes.data ? (
              <div className="flex flex-wrap items-center gap-2" role="status">
                <p className="text-sm text-destructive">Không thể tải môn học phụ trách.</p>
                <Button type="button" variant="outline" onClick={() => void classes.refetch()}>Thử lại</Button>
              </div>
            ) : subjects.length === 0 ? (
              <p className="rounded-xl bg-muted p-3 text-sm text-muted-foreground">Chưa có lớp học phần trong học kỳ hiện tại.</p>
            ) : (
              <div className="space-y-3">
                {initialClassId !== undefined && !initialClass && (
                  <p role="status" className="text-sm text-warning">Lớp đã chọn không có trong học kỳ hiện tại. Hãy chọn lớp khác.</p>
                )}
                <FilterSelect label="Môn học" required value={selectedSubject ? String(selectedSubject.id) : ''}
                  disabled={generate.isPending} options={subjects.map((subject) => ({ value: String(subject.id), label: subject.label }))}
                  onChange={(value) => {
                    setSubjectId(Number(value));
                    setClassIds([]);
                    setUseTopicMatrix(false);
                    setTopicPercentages({});
                    generate.reset();
                  }} />
                {selectedSubject && (bankQuery.isPending ? (
                  <Skeleton role="status" aria-label="Đang tải ngân hàng câu hỏi" className="h-14 rounded-xl" />
                ) : !bank ? (
                  <div className="flex flex-wrap items-center gap-2" role="status">
                    <p className="text-sm text-destructive">Không thể tải số liệu ngân hàng câu hỏi.</p>
                    <Button type="button" variant="outline" onClick={() => void bankQuery.refetch()}>Thử lại</Button>
                  </div>
                ) : (
                  <div className="rounded-xl bg-surface-subtle p-3 text-sm" role="status">
                    {bank.totalQuestions === 0 ? 'Ngân hàng chưa có câu hỏi nào dùng được để tạo đề.'
                      : <>Ngân hàng câu hỏi · <strong>{bank.totalQuestions} câu đã duyệt</strong></>}
                  </div>
                ))}
                {selectedSubject && (
                  <fieldset className="min-w-0 space-y-2">
                    <legend className="text-sm font-medium">Lớp học phần ({selectedClasses.length}/{availableClasses.length})</legend>
                    <div className="flex flex-wrap items-center gap-2">
                      {availableClasses.map((courseClass) => {
                        const checked = selectedClasses.includes(courseClass);
                        return (
                          <label key={courseClass.courseClassId} className="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-input px-3 text-sm font-medium has-checked:border-academic has-checked:bg-academic-soft has-checked:text-academic">
                            <input type="checkbox" checked={checked} className="size-4 shrink-0 accent-academic"
                              onChange={() => {
                                const ids = selectedClasses.map((item) => item.courseClassId);
                                setClassIds(checked ? ids.filter((classId) => classId !== courseClass.courseClassId) : [...ids, courseClass.courseClassId]);
                              }} />
                            {courseClass.classCode}
                          </label>
                        );
                      })}
                      {availableClasses.length > 1 && (
                        <Button type="button" variant="ghost" onClick={() => setClassIds(
                          selectedClasses.length === availableClasses.length ? [] : availableClasses.map((item) => item.courseClassId),
                        )}>
                          {selectedClasses.length === availableClasses.length ? 'Bỏ chọn tất cả' : 'Chọn tất cả'}
                        </Button>
                      )}
                    </div>
                  </fieldset>
                )}
              </div>
            )}

            <div className="grid gap-3 border-t border-border pt-4 sm:grid-cols-[minmax(0,1fr)_7rem]">
              <div className="space-y-1.5">
                <Label htmlFor={`${id}-title`}>Tiêu đề (không bắt buộc)</Label>
                <Input id={`${id}-title`} className="h-11 md:h-11" value={title} onChange={(event) => setTitle(event.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor={`${id}-count`}>Số câu hỏi</Label>
                <QuestionCountInput id={`${id}-count`} value={questionCount} onChange={setQuestionCount}
                  invalid={invalidCount || bankTooSmall}
                  describedBy={bankTooSmall ? `${id}-count-range ${id}-count-error` : `${id}-count-range`} />
                <p id={`${id}-count-range`} className={cn('text-sm', invalidCount ? 'text-destructive' : 'text-muted-foreground')}>
                  {invalidCount ? `Nhập số nguyên từ 1 đến ${maxQuestionCount}.` : `Từ 1 đến ${maxQuestionCount} câu`}
                </p>
              </div>
            </div>
            {bankTooSmall && <p id={`${id}-count-error`} role="status" className="text-sm text-destructive">
              Ngân hàng chỉ có {bank.totalQuestions}/{count} câu. Hãy giảm số câu của đề.
            </p>}

            <fieldset aria-describedby={`${id}-matrix-help ${id}-matrix-total`} className="min-w-0 space-y-2 border-t border-border pt-3">
              <legend className="pr-2 text-sm font-semibold">Ma trận độ khó</legend>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {examDifficulties.map(({ field, count: countField, label, tone }) => (
                  <div key={field} className={cn('min-w-0 space-y-2 rounded-xl p-2.5', tone)}>
                    <Label htmlFor={`${id}-${field}`} className="leading-tight">{label}</Label>
                    {bank && <p className="text-xs">Có {bank.statistic[countField] ?? 0} câu</p>}
                    <div className="relative text-foreground">
                      <Input id={`${id}-${field}`} type="number" min={0} max={100} step={1} required
                        aria-label={`Tỷ lệ ${label.toLowerCase()} (%)`}
                        className="h-11 bg-background pr-7" value={percentages[field]}
                        onChange={(event) => setPercentages((previous) => ({ ...previous, [field]: event.target.value }))} />
                      <span aria-hidden="true" className="pointer-events-none absolute inset-y-0 right-2 grid place-items-center text-sm text-muted-foreground">%</span>
                    </div>
                  </div>
                ))}
              </div>
              <p id={`${id}-matrix-total`} role="status" className={cn('text-sm font-medium', validMatrix ? 'text-success' : 'text-destructive')}>
                Tổng {total}%{!validMatrix && ' · Nhập số nguyên từ 0 đến 100 cho mỗi nhóm, tổng phải bằng 100%.'}
              </p>
              <p id={`${id}-matrix-help`} className="text-sm text-muted-foreground">
                Hệ thống làm tròn tỷ lệ ra số câu nguyên. Đề chỉ được tạo khi kho đáp ứng đủ ma trận.
              </p>
            </fieldset>

            {bank && (bank.topics.length > 0 || useTopicMatrix) && (
              <fieldset className="min-w-0 space-y-2 border-t border-border pt-2">
                <legend className="pr-2 text-sm font-semibold">Ma trận chủ đề</legend>
                <label className="flex min-h-11 cursor-pointer items-center gap-2 text-sm">
                  <input type="checkbox" checked={useTopicMatrix} onChange={(event) => setUseTopicMatrix(event.target.checked)}
                    className="size-4 shrink-0 accent-academic" />
                  Phân bổ theo chủ đề
                </label>
                {useTopicMatrix && (
                  <>
                    <ul className="space-y-2">
                      {weightedTopics.map((topic) => (
                        <li key={topic.key} className="space-y-3 rounded-xl border border-border bg-surface-subtle p-3">
                          <div className="grid grid-cols-[minmax(0,1fr)_5rem] items-center gap-3">
                            <div className="min-w-0 space-y-1.5">
                              <Label htmlFor={`${id}-topic-${topic.key}`} className="wrap-break-word font-semibold leading-tight">{topic.topicName}</Label>
                              <p className="w-fit rounded-md bg-muted px-2 py-0.5 text-sm font-semibold tabular-nums">
                                {topic.totalQuestions} câu trong kho
                              </p>
                            </div>
                            <div className="relative">
                              <Input id={`${id}-topic-${topic.key}`} type="number" min={0} max={100} step={1} required
                                aria-label={`Tỷ lệ chủ đề ${topic.topicName} (%)`} className="h-11 bg-background pr-7"
                                value={topic.percentage}
                                onChange={(event) => setTopicPercentages((previous) => ({ ...previous, [topic.key]: event.target.value }))} />
                              <span aria-hidden="true" className="pointer-events-none absolute inset-y-0 right-2 grid place-items-center text-sm text-muted-foreground">%</span>
                            </div>
                          </div>
                          <dl className="grid grid-cols-2 gap-1.5 sm:grid-cols-4">
                            {examDifficulties.map(({ count: countField, field, label, tone }) => (
                              <div key={field} className={cn('flex min-w-0 flex-col justify-between gap-1 rounded-lg px-2 py-1.5', tone)}>
                                <dt className="text-xs leading-tight">{label}</dt>
                                <dd className="text-base font-semibold tabular-nums">{topic.statistic[countField] ?? 0}</dd>
                              </div>
                            ))}
                          </dl>
                        </li>
                      ))}
                    </ul>
                    <p role="status" className={cn('text-sm font-medium', validTopics ? 'text-success' : 'text-destructive')}>
                      Tổng {topicTotal}%{!validTopics && ' · Tỷ lệ các chủ đề phải cộng đủ 100%.'}
                    </p>
                  </>
                )}
              </fieldset>
            )}
            {bank?.topics.length === 0 && <p className="text-sm text-muted-foreground">Môn học chưa có chủ đề đang hoạt động để lập ma trận chủ đề.</p>}

            <div className="flex flex-wrap gap-x-4 border-t border-border pt-2">
              <label className="flex min-h-11 cursor-pointer items-center gap-2 text-sm">
                <input type="checkbox" checked={shuffleOptions} onChange={(event) => setShuffleOptions(event.target.checked)} className="size-4 shrink-0 accent-academic" />
                Đảo thứ tự phương án
              </label>
              <label className="flex min-h-11 cursor-pointer items-center gap-2 text-sm">
                <input type="checkbox" checked={includeAnswerKey} onChange={(event) => setIncludeAnswerKey(event.target.checked)} className="size-4 shrink-0 accent-academic" />
                Kèm bảng đáp án
              </label>
            </div>
          </fieldset>
        </div>
        <footer className="shrink-0 space-y-2 border-t border-border p-4 sm:px-5">
          {generate.isError && <p role="alert" className="text-sm text-destructive">{generate.error.message || 'Không thể tạo đề thi. Vui lòng thử lại.'}</p>}
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" disabled={generate.isPending} onClick={onClose}>Hủy</Button>
            <Button type="submit" disabled={!canGenerate} pending={{ active: generate.isPending, label: 'Đang tạo đề…' }}>
              <FilePlus2 aria-hidden="true" /> Tạo đề thi
            </Button>
          </div>
        </footer>
      </form>
    </ExamDialog>
  );
}
