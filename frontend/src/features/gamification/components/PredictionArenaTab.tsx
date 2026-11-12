import { useState, type FormEvent, type ReactNode } from 'react';
import { Bot, BrainCircuit, ChevronDown, Database, History, RefreshCw, Send, ShieldAlert, Target, Users } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { StudySubjectSelect } from '@/features/session/components/StudySubjectSelect';
import { cn } from '@/lib/utils';
import {
  useActiveGame6Session,
  useGame1Classes,
  useMyPredictions,
  usePredictGame1,
  usePredictGame4,
  useSubmitGame6,
} from '../hooks/useGamification';
import { useGamificationSubjects } from '../hooks/useGamificationSubjects';
import type { Game3Result, GamePredictionResponse, GameType } from '../types/gamification.types';
import { ErrorHunterTab } from './ErrorHunterTab';

const tabs = [
  { id: 'game1', label: '1 · Nộp bài ngày mai', icon: Users },
  { id: 'game2', label: '2 · Duyệt câu hỏi', icon: Bot },
  { id: 'game3', label: '3 · Độ tự tin', icon: Target },
  { id: 'game4', label: '4 · Quy mô cuối kỳ', icon: Database },
  { id: 'game5', label: '5 · Thợ săn lỗi', icon: ShieldAlert },
  { id: 'game6', label: '6 · Phán đoán AI', icon: BrainCircuit },
  { id: 'history', label: 'Lịch sử', icon: History },
] as const;

const gameLabels: Record<GameType, string> = {
  GAME_1_PARTICIPANTS: 'Game 1 · Nộp bài ngày mai',
  GAME_2_APPROVED_QUESTIONS: 'Game 2 · Duyệt câu hỏi',
  GAME_3_AUTHOR_CONFIDENCE: 'Game 3 · Độ tự tin của tác giả',
  GAME_4_SUBJECT_BANK_SIZE: 'Game 4 · Quy mô cuối kỳ',
  GAME_5_REPORT_ERROR: 'Game 5 · Thợ săn lỗi',
  GAME_6_LLM_IDENTIFICATION: 'Game 6 · Phán đoán AI',
};

const schoolTimeZone = 'Asia/Ho_Chi_Minh';
const schoolDate = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeZone: schoolTimeZone });
const schoolDateTime = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: schoolTimeZone });
const ratingFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 4 });
const questionVerdicts = {
  MATCH: { label: 'Khớp', variant: 'success' },
  MISMATCH: { label: 'Lệch', variant: 'destructive' },
  INSUFFICIENT_RATINGS: { label: 'Chưa đủ đánh giá', variant: 'outline' },
  MISSING_CONFIDENCE: { label: 'Thiếu độ tự tin', variant: 'outline' },
} as const;

function GamePanel({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <section className="space-y-5 rounded-2xl border border-border bg-card p-4 sm:p-6">
      <div className="space-y-1">
        <h3 className="text-lg font-bold tracking-tight sm:text-xl">{title}</h3>
        <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground">{description}</p>
      </div>
      {children}
    </section>
  );
}

function NumberField({ id, label, value, onChange, min, max }: {
  id: string; label: string; value: string; onChange: (value: string) => void; min: number; max?: number;
}) {
  return (
    <div className="min-w-0 space-y-1.5">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} type="number" value={value} onChange={(event) => onChange(event.target.value)}
        min={min} max={max} required />
    </div>
  );
}

function PredictionStatus({ item }: { item: GamePredictionResponse }) {
  if (item.status === 'PENDING') return <Badge variant="warning">Đang chờ</Badge>;
  if (item.gameType === 'GAME_3_AUTHOR_CONFIDENCE' && item.status === 'RESOLVED'
      && item.actualData?.outcome === 'INSUFFICIENT_RATINGS') {
    return <Badge variant="outline">Chưa đủ đánh giá</Badge>;
  }
  if (item.status === 'CANCELLED') return <Badge variant="outline">Đã hủy</Badge>;
  return item.isCorrect
    ? <Badge variant="success">Đoán trúng</Badge>
    : <Badge variant="destructive">Chưa chính xác</Badge>;
}

function Game3ResultDetails({ result }: { result: Game3Result }) {
  return (
    <details className="group rounded-xl border border-border bg-surface-subtle sm:col-span-2">
      <summary className="flex min-h-11 cursor-pointer list-none flex-wrap items-center gap-x-3 gap-y-1 rounded-xl px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-ring [&::-webkit-details-marker]:hidden">
        <ChevronDown aria-hidden="true" className="size-4 shrink-0 group-open:rotate-180" />
        Chi tiết lần xét
        <span className="text-muted-foreground">{result.matchedQuestions} câu khớp · {result.nonMatchingQuestions} câu lệch</span>
      </summary>
      {result.questionEvaluations?.length ? (
        <ul className="divide-y divide-border border-t border-border">
          {result.questionEvaluations.map((row) => {
            const verdict = questionVerdicts[row.verdict];
            return (
              <li key={row.questionId} className="grid gap-2 px-3 py-3 sm:grid-cols-[minmax(0,2fr)_minmax(0,3fr)] sm:items-center">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium">Câu {row.displayOrder ?? `#${row.questionId}`}</span>
                    <Badge variant={verdict.variant}>{verdict.label}</Badge>
                  </div>
                  {row.questionCode && <p className="mt-1 break-all font-mono text-xs text-muted-foreground">{row.questionCode}</p>}
                </div>
                <dl className="grid grid-cols-3 gap-2 text-sm tabular-nums">
                  <div>
                    <dt className="text-xs text-muted-foreground">Độ tự tin</dt>
                    <dd>{row.declaredConfidence == null ? '—' : ratingFormat.format(row.declaredConfidence)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Điểm TB</dt>
                    <dd title={row.averageRating?.toString()}>{row.averageRating == null ? '—' : ratingFormat.format(row.averageRating)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Người đánh giá</dt>
                    <dd>{row.ratingCount}</dd>
                  </div>
                </dl>
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="border-t border-border px-3 py-3 text-sm text-muted-foreground">Lượt này chỉ lưu số liệu tổng hợp.</p>
      )}
    </details>
  );
}

function HistoryRow({ item }: { item: GamePredictionResponse }) {
  const isGame3 = item.gameType === 'GAME_3_AUTHOR_CONFIDENCE';
  return (
    <li className="grid gap-2 py-3 sm:grid-cols-[1fr_auto] sm:items-start">
      <div className="min-w-0 space-y-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-medium">{gameLabels[item.gameType] ?? item.gameType}</span>
          <PredictionStatus item={item} />
        </div>
        <p className="text-xs text-muted-foreground">
          {isGame3 ? 'Phiên chốt lúc' : 'Đặt lúc'} <time dateTime={item.createdAt}>{schoolDateTime.format(new Date(item.createdAt))}</time>
          {item.resolvedAt && <> · {isGame3 ? 'Xét lúc' : 'Chốt lúc'} <time dateTime={item.resolvedAt}>{schoolDateTime.format(new Date(item.resolvedAt))}</time></>}
        </p>
      </div>
      <div className="space-y-1 text-sm text-muted-foreground sm:text-right">
        {item.gameType === 'GAME_1_PARTICIPANTS' && item.targetDate && (
          <p className="font-medium text-foreground">
            Dự đoán cho ngày <time dateTime={item.targetDate}>{schoolDate.format(new Date(item.targetDate))}</time>
          </p>
        )}
        {isGame3 && item.status === 'PENDING' && item.targetDate && (
          <p className="font-medium text-foreground">
            Mốc xét tiếp theo <time dateTime={item.targetDate}>{schoolDate.format(new Date(item.targetDate))}</time>
          </p>
        )}
        {item.targetId != null && <p className="text-xs">{item.gameType === 'GAME_1_PARTICIPANTS' ? 'Lớp' : isGame3 ? 'Phiên' : 'Mục tiêu'} #{item.targetId}</p>}
      </div>
      {isGame3 && item.status === 'RESOLVED' && item.actualData && <Game3ResultDetails result={item.actualData} />}
    </li>
  );
}

export function PredictionArenaTab() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeGame = tabs.find(({ id }) => id === searchParams.get('game'))?.id ?? 'game1';
  const classesQuery = useGame1Classes();
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [game1Count, setGame1Count] = useState('1');
  const predictGame1 = usePredictGame1();
  const game1Classes = classesQuery.data ?? [];
  const selectedClass = game1Classes.find((course) => course.courseClassId === selectedClassId && !course.alreadyPredicted);

  const [game4SubjectId, setGame4SubjectId] = useState<number>();
  const [game4BankSize, setGame4BankSize] = useState('100');
  const predictGame4 = usePredictGame4();
  const subjectsQuery = useGamificationSubjects(activeGame === 'game4');
  const subjects = subjectsQuery.data ?? [];
  const game4Subject = subjects.find((subject) => subject.subjectId === game4SubjectId);

  const game6Query = useActiveGame6Session();
  const game6 = game6Query.data;
  const [game6Choices, setGame6Choices] = useState<{ sessionId: number; ids: number[] }>();
  const selectedLlmQuestionIds = game6Choices?.sessionId === game6?.sessionId ? game6Choices?.ids ?? [] : [];
  const submitGame6 = useSubmitGame6();

  const historyQuery = useMyPredictions();
  const predictions = historyQuery.data?.contents ?? [];

  function submitGame1(event: FormEvent) {
    event.preventDefault();
    if (!selectedClass || predictGame1.isPending) return;
    predictGame1.mutate({ courseClassId: selectedClass.courseClassId, predictedCount: Number(game1Count) }, {
      onSuccess: () => setSelectedClassId(null),
    });
  }

  function submitGame4(event: FormEvent) {
    event.preventDefault();
    if (!game4Subject || predictGame4.isPending) return;
    predictGame4.mutate({ subjectId: game4Subject.subjectId, predictedBankSize: Number(game4BankSize) });
  }

  return (
    <div className="space-y-5">
      <div role="group" aria-label="Trò chơi dự đoán" className="flex flex-wrap gap-2 rounded-2xl border border-border bg-card p-2">
        {tabs.map(({ id, label, icon: Icon }) => (
          <Button key={id} type="button" variant={activeGame === id ? 'secondary' : 'ghost'}
            aria-pressed={activeGame === id} className={cn('min-h-11', activeGame === id && 'bg-academic-soft text-academic', id === 'history' && 'sm:ml-auto')}
            onClick={() => {
              if (id === activeGame) return;
              const next = new URLSearchParams(searchParams);
              next.set('game', id);
              setSearchParams(next);
            }}>
            <Icon aria-hidden="true" /> {label}
          </Button>
        ))}
      </div>

      {activeGame === 'game1' && (
        <GamePanel title="Số sinh viên nộp bài ngày mai"
          description="Dự đoán số sinh viên sẽ đề xuất câu hỏi trong một lớp học phần. Đặt trước 22:00. Kết quả được chốt từ 00:05 sau ngày dự đoán, theo giờ Việt Nam.">
          {classesQuery.isLoading ? <Skeleton className="h-32 rounded-xl" aria-label="Đang tải lớp học phần" />
            : classesQuery.isError ? <ErrorState title="Không thể tải lớp học phần" onRetry={() => void classesQuery.refetch()} />
            : game1Classes.length === 0 ? <EmptyState icon={Users} title="Bạn chưa tham gia lớp học phần nào trong học kỳ này" />
            : (
              <div className="grid gap-3 md:grid-cols-2">
                {game1Classes.map((course) => (
                  <button key={course.courseClassId} type="button" disabled={course.alreadyPredicted || predictGame1.isPending}
                    aria-pressed={selectedClassId === course.courseClassId} onClick={() => setSelectedClassId(course.courseClassId)}
                    className={cn('min-w-0 space-y-2 rounded-xl border border-border bg-surface-subtle p-4 text-left focus-visible:outline-2 focus-visible:outline-ring enabled:cursor-pointer enabled:hover:border-academic/60',
                      selectedClassId === course.courseClassId && 'border-academic bg-academic-soft')}>
                    <span className="flex flex-wrap items-center justify-between gap-2">
                      <span className="font-mono text-sm font-semibold text-academic">{course.classCode}</span>
                      {course.alreadyPredicted ? (
                        <span className="rounded-full bg-success-soft px-2.5 py-1 text-xs font-medium text-success">Đã đặt · {course.predictedCount} sinh viên</span>
                      ) : <span className="text-xs text-muted-foreground">Kỳ {course.semester}</span>}
                    </span>
                    <span className="block wrap-anywhere font-semibold">{course.subjectName}</span>
                    {course.lecturerName && <span className="block text-sm text-muted-foreground">GV {course.lecturerName}</span>}
                  </button>
                ))}
              </div>
            )}
          {selectedClass && (
            <form onSubmit={submitGame1} className="grid gap-3 border-t border-border pt-4 sm:grid-cols-[minmax(0,20rem)_auto] sm:items-end sm:justify-start">
              <NumberField id="game1-count" label="Số sinh viên dự đoán" min={1} max={100} value={game1Count} onChange={setGame1Count} />
              <Button type="submit" disabled={predictGame1.isPending}><Send aria-hidden="true" />{predictGame1.isPending ? 'Đang gửi…' : 'Gửi dự đoán'}</Button>
            </form>
          )}
        </GamePanel>
      )}

      {activeGame === 'game2' && (
        <GamePanel title="Thể lệ Game 2 · Số câu hỏi được duyệt"
          description="Bạn có thể tham gia Game 2 ngay khi gửi phiên đề xuất câu hỏi của mình.">
          <ol className="max-w-2xl list-decimal space-y-2 rounded-xl border border-border bg-surface-subtle py-4 pr-4 pl-9 text-sm">
            <li>Khi đề xuất phiên, nhập hai số dự đoán: số câu do AI tạo và số câu do người biên soạn sẽ được duyệt.</li>
            <li>Khi giảng viên chốt phiên, hệ thống đối chiếu từng số với kết quả thực tế.</li>
            <li>Bạn nhận <strong className="text-academic">0,5 điểm công khai</strong> khi cả hai số đều khớp.</li>
          </ol>
          <Button render={<Link to="/sessions/propose" />} nativeButton={false}>Đề xuất câu hỏi</Button>
        </GamePanel>
      )}

      {activeGame === 'game3' && (
        <GamePanel title="Thể lệ Game 3 · Độ tự tin của tác giả"
          description="Khi chốt phiên có ít nhất 10 câu hỏi, bạn tự động tham gia Game 3. Hệ thống xét 10 câu đầu theo thứ tự đề xuất, gồm cả câu bị từ chối.">
          <ol className="max-w-2xl list-decimal space-y-2 rounded-xl border border-border bg-surface-subtle py-4 pr-4 pl-9 text-sm">
            <li>Hệ thống xét lần đầu sau 14 ngày kể từ khi chốt phiên. Mỗi câu cần ít nhất 3 người khác tác giả đánh giá.</li>
            <li>Điểm đánh giá trung bình và độ tự tin được so trên thang 0–4. Chênh tối đa 0,5 điểm được tính là khớp.</li>
            <li>Ít nhất 5 câu khớp được thưởng <strong className="text-academic">0,25 điểm công khai</strong>. Nếu 6 câu trở lên lệch quá 0,5 điểm, lượt dự đoán kết thúc với kết quả chưa chính xác.</li>
            <li>Nếu chưa thể kết luận, hệ thống xét lại ở mốc 28 ngày hoặc lúc chốt sổ học kỳ, tùy mốc nào đến trước. Nếu vẫn chưa thể kết luận, lịch sử sẽ ghi “Chưa đủ đánh giá”.</li>
          </ol>
          <p className="max-w-2xl text-sm text-muted-foreground">Nếu học kỳ chốt sổ trước mốc 14 ngày, hệ thống xét kết quả ngay khi chốt sổ.</p>
          <p className="max-w-2xl text-sm text-muted-foreground">Kết quả thắng hoặc thua được chốt ngay khi đủ điều kiện. Bạn vẫn có thể sửa đánh giá sau đó. Lịch sử hiển thị số liệu tại lần xét đã chốt.</p>
        </GamePanel>
      )}

      {activeGame === 'game4' && (
        <GamePanel title="Quy mô ngân hàng câu hỏi cuối kỳ"
          description="Dự đoán tổng số câu hỏi được duyệt vào ngân hàng đề của một môn khi học kỳ kết thúc.">
          {subjectsQuery.isLoading ? <Skeleton className="h-28 max-w-xl rounded-xl" aria-label="Đang tải môn học" />
            : subjectsQuery.isError ? <ErrorState title="Không thể tải danh sách môn học" onRetry={() => void subjectsQuery.refetch()} />
            : subjects.length === 0 ? <EmptyState icon={Database} title="Chưa có môn học để dự đoán" />
            : (
              <form onSubmit={submitGame4} className="max-w-xl space-y-4 rounded-xl border border-border bg-surface-subtle p-4">
                <StudySubjectSelect subjects={subjects} value={game4Subject?.subjectId} onChange={setGame4SubjectId}
                  label="Môn học" emptyLabel="Chọn một môn học…" required />
                <NumberField id="game4-bank-size" label="Tổng số câu hỏi dự đoán" min={0} value={game4BankSize} onChange={setGame4BankSize} />
                <Button type="submit" disabled={!game4Subject || predictGame4.isPending}>
                  <Send aria-hidden="true" />{predictGame4.isPending ? 'Đang gửi…' : 'Gửi dự đoán'}
                </Button>
              </form>
            )}
        </GamePanel>
      )}

      {activeGame === 'game5' && <ErrorHunterTab />}

      {activeGame === 'game6' && (
        <GamePanel title="Phán đoán câu hỏi do AI tạo"
          description="Phiên thử thách mở vào thứ Bảy. Chọn những câu hỏi bạn cho rằng do AI tạo.">
          {game6Query.isLoading ? <Skeleton className="h-40 rounded-xl" aria-label="Đang tải thử thách" />
            : game6Query.isError ? <ErrorState title="Không thể tải thử thách" onRetry={() => void game6Query.refetch()} />
            : !game6 ? <EmptyState icon={BrainCircuit} title="Chưa có phiên thử thách đang mở" />
            : (
              <div className="space-y-4">
                <div className="flex flex-wrap justify-between gap-2 text-sm text-muted-foreground">
                  <span>Tuần {game6.weekNumber} / {game6.year}</span>
                  <span>Đã chọn {selectedLlmQuestionIds.length} câu</span>
                </div>
                <div className="space-y-3">
                  {game6.questions.map((question, index) => {
                    const checked = selectedLlmQuestionIds.includes(question.questionId);
                    return (
                      <label key={question.questionId} className={cn('flex cursor-pointer items-start gap-3 rounded-xl border border-border bg-surface-subtle p-4',
                        checked && 'border-academic bg-academic-soft')}>
                        <input type="checkbox" checked={checked} disabled={submitGame6.isPending} className="mt-1 size-4 shrink-0 accent-academic"
                          onChange={() => setGame6Choices({ sessionId: game6.sessionId, ids: checked
                            ? selectedLlmQuestionIds.filter((id) => id !== question.questionId)
                            : [...selectedLlmQuestionIds, question.questionId] })} />
                        <span className="min-w-0 space-y-2">
                          <span className="block text-sm font-semibold">Câu {index + 1}. {question.content}</span>
                          <span className="grid gap-1 text-sm text-muted-foreground sm:grid-cols-2">
                            {question.options?.map((option) => <span key={option.key}><strong>{option.key}.</strong> {option.content}</span>)}
                          </span>
                        </span>
                      </label>
                    );
                  })}
                </div>
                <Button type="button" disabled={submitGame6.isPending || selectedLlmQuestionIds.length === 0}
                  onClick={() => submitGame6.mutate({ minigameSessionId: game6.sessionId, selectedLlmQuestionIds })}>
                  <Send aria-hidden="true" />{submitGame6.isPending ? 'Đang gửi…' : 'Gửi dự đoán'}
                </Button>
              </div>
            )}
        </GamePanel>
      )}

      {activeGame === 'history' && (
        <GamePanel title="Lịch sử dự đoán" description="Ngày đặt và ngày mục tiêu của Game 1 được hiển thị theo giờ Việt Nam.">
          <Button type="button" variant="outline" disabled={historyQuery.isFetching} onClick={() => void historyQuery.refetch()}>
            <RefreshCw aria-hidden="true" className={cn(historyQuery.isFetching && 'motion-safe:animate-spin')} /> Làm mới
          </Button>
          {historyQuery.isLoading ? <Skeleton className="h-40 rounded-xl" aria-label="Đang tải lịch sử dự đoán" />
            : historyQuery.isError ? <ErrorState title="Không thể tải lịch sử dự đoán" onRetry={() => void historyQuery.refetch()} />
            : predictions.length === 0 ? <EmptyState icon={History} title="Bạn chưa có lượt dự đoán nào" />
            : <ul className="divide-y divide-border">{predictions.map((item) => <HistoryRow key={item.predictionId} item={item} />)}</ul>}
        </GamePanel>
      )}
    </div>
  );
}
