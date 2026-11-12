import { useState, type FormEvent, type ReactNode } from 'react';
import { Bot, BrainCircuit, Database, History, RefreshCw, Send, Users } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import {
  useActiveGame6Session,
  useGame1Classes,
  useMyPredictions,
  usePredictGame1,
  usePredictGame2,
  usePredictGame4,
  useSubmitGame6,
} from '../hooks/useGamification';
import type { GamePredictionResponse, GameType } from '../types/gamification.types';

const tabs = [
  { id: 'game1', label: '1 · Nộp bài ngày mai', icon: Users },
  { id: 'game2', label: '2 · Duyệt câu hỏi', icon: Bot },
  { id: 'game4', label: '4 · Quy mô cuối kỳ', icon: Database },
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
  if (item.status === 'CANCELLED') return <Badge variant="outline">Đã hủy</Badge>;
  return item.isCorrect
    ? <Badge variant="success">Đoán trúng</Badge>
    : <Badge variant="destructive">Chưa chính xác</Badge>;
}

function HistoryRow({ item }: { item: GamePredictionResponse }) {
  return (
    <li className="grid gap-2 py-3 sm:grid-cols-[1fr_auto] sm:items-start">
      <div className="min-w-0 space-y-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-medium">{gameLabels[item.gameType] ?? item.gameType}</span>
          <PredictionStatus item={item} />
        </div>
        <p className="text-xs text-muted-foreground">
          Đặt lúc <time dateTime={item.createdAt}>{schoolDateTime.format(new Date(item.createdAt))}</time>
          {item.resolvedAt && <> · Chốt lúc <time dateTime={item.resolvedAt}>{schoolDateTime.format(new Date(item.resolvedAt))}</time></>}
        </p>
      </div>
      <div className="space-y-1 text-sm text-muted-foreground sm:text-right">
        {item.gameType === 'GAME_1_PARTICIPANTS' && item.targetDate && (
          <p className="font-medium text-foreground">
            Dự đoán cho ngày <time dateTime={item.targetDate}>{schoolDate.format(new Date(item.targetDate))}</time>
          </p>
        )}
        {item.targetId != null && <p className="text-xs">{item.gameType === 'GAME_1_PARTICIPANTS' ? 'Lớp' : 'Mục tiêu'} #{item.targetId}</p>}
      </div>
    </li>
  );
}

export function PredictionArenaTab() {
  const [activeGame, setActiveGame] = useState<(typeof tabs)[number]['id']>('game1');
  const classesQuery = useGame1Classes();
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [game1Count, setGame1Count] = useState('1');
  const predictGame1 = usePredictGame1();
  const game1Classes = classesQuery.data ?? [];
  const selectedClass = game1Classes.find((course) => course.courseClassId === selectedClassId && !course.alreadyPredicted);

  const [game2SessionId, setGame2SessionId] = useState('');
  const [game2LlmCount, setGame2LlmCount] = useState('0');
  const [game2HumanCount, setGame2HumanCount] = useState('0');
  const predictGame2 = usePredictGame2();

  const [game4SubjectId, setGame4SubjectId] = useState('');
  const [game4BankSize, setGame4BankSize] = useState('100');
  const predictGame4 = usePredictGame4();

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

  function submitGame2(event: FormEvent) {
    event.preventDefault();
    if (!game2SessionId || predictGame2.isPending) return;
    predictGame2.mutate({
      sessionId: Number(game2SessionId),
      predictedLlmCount: Number(game2LlmCount),
      predictedHumanCount: Number(game2HumanCount),
    });
  }

  function submitGame4(event: FormEvent) {
    event.preventDefault();
    if (!game4SubjectId || predictGame4.isPending) return;
    predictGame4.mutate({ subjectId: Number(game4SubjectId), predictedBankSize: Number(game4BankSize) });
  }

  return (
    <div className="space-y-5">
      <div role="group" aria-label="Trò chơi dự đoán" className="flex flex-wrap gap-2 rounded-2xl border border-border bg-card p-2">
        {tabs.map(({ id, label, icon: Icon }) => (
          <Button key={id} type="button" variant={activeGame === id ? 'secondary' : 'ghost'}
            aria-pressed={activeGame === id} className={cn('min-h-11', activeGame === id && 'bg-academic-soft text-academic', id === 'history' && 'sm:ml-auto')}
            onClick={() => setActiveGame(id)}>
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
        <GamePanel title="Số câu hỏi được duyệt"
          description="Dự đoán có bao nhiêu câu do AI và do người biên soạn trong phiên đề xuất của bạn sẽ được giảng viên duyệt.">
          <form onSubmit={submitGame2} className="max-w-xl space-y-4 rounded-xl border border-border bg-surface-subtle p-4">
            <NumberField id="game2-session" label="ID phiên đề xuất" min={1} value={game2SessionId} onChange={setGame2SessionId} />
            <div className="grid gap-3 sm:grid-cols-2">
              <NumberField id="game2-ai-count" label="Số câu do AI tạo" min={0} value={game2LlmCount} onChange={setGame2LlmCount} />
              <NumberField id="game2-human-count" label="Số câu do người biên soạn" min={0} value={game2HumanCount} onChange={setGame2HumanCount} />
            </div>
            <Button type="submit" disabled={predictGame2.isPending}><Send aria-hidden="true" />{predictGame2.isPending ? 'Đang gửi…' : 'Gửi dự đoán'}</Button>
          </form>
        </GamePanel>
      )}

      {activeGame === 'game4' && (
        <GamePanel title="Quy mô ngân hàng câu hỏi cuối kỳ"
          description="Dự đoán tổng số câu hỏi được duyệt vào ngân hàng đề của một môn khi học kỳ kết thúc.">
          <form onSubmit={submitGame4} className="max-w-xl space-y-4 rounded-xl border border-border bg-surface-subtle p-4">
            <NumberField id="game4-subject" label="ID môn học" min={1} value={game4SubjectId} onChange={setGame4SubjectId} />
            <NumberField id="game4-bank-size" label="Tổng số câu hỏi dự đoán" min={10} value={game4BankSize} onChange={setGame4BankSize} />
            <Button type="submit" disabled={predictGame4.isPending}><Send aria-hidden="true" />{predictGame4.isPending ? 'Đang gửi…' : 'Gửi dự đoán'}</Button>
          </form>
        </GamePanel>
      )}

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
