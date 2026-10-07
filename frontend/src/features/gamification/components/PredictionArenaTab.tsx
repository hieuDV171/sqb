import { useState } from 'react';
import {
  Users,
  Bot,
  Database,
  BrainCircuit,
  History,
  CheckCircle,
  XCircle,
  Clock,
  Send,
} from 'lucide-react';
import {
  useGame1Classes,
  usePredictGame1,
  usePredictGame2,
  usePredictGame4,
  useActiveGame6Session,
  useSubmitGame6,
  useMyPredictions,
} from '../hooks/useGamification';

export function PredictionArenaTab() {
  const [activeGame, setActiveGame] = useState<'game1' | 'game2' | 'game4' | 'game6' | 'history'>('game1');

  // Game 1 State
  const { data: game1Classes = [], isLoading: isGame1Loading } = useGame1Classes();
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [game1Count, setGame1Count] = useState<number>(1);
  const predictGame1Mutation = usePredictGame1();

  // Game 2 State
  const [game2SessionId, setGame2SessionId] = useState<string>('');
  const [game2LlmCount, setGame2LlmCount] = useState<number>(0);
  const [game2HumanCount, setGame2HumanCount] = useState<number>(0);
  const predictGame2Mutation = usePredictGame2();

  // Game 4 State
  const [game4SubjectId, setGame4SubjectId] = useState<string>('');
  const [game4BankSize, setGame4BankSize] = useState<number>(100);
  const predictGame4Mutation = usePredictGame4();

  // Game 6 State
  const { data: activeGame6, isLoading: isGame6Loading } = useActiveGame6Session();
  const [selectedLlmQuestionIds, setSelectedLlmQuestionIds] = useState<number[]>([]);
  const submitGame6Mutation = useSubmitGame6();

  // History State
  const { data: historyData, isLoading: isHistoryLoading } = useMyPredictions();
  const predictionsList = historyData?.contents || [];

  const handlePredictGame1 = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClassId) return;
    predictGame1Mutation.mutate({
      courseClassId: selectedClassId,
      predictedCount: Number(game1Count),
    });
  };

  const handlePredictGame2 = (e: React.FormEvent) => {
    e.preventDefault();
    if (!game2SessionId) return;
    predictGame2Mutation.mutate({
      sessionId: Number(game2SessionId),
      predictedLlmCount: Number(game2LlmCount),
      predictedHumanCount: Number(game2HumanCount),
    });
  };

  const handlePredictGame4 = (e: React.FormEvent) => {
    e.preventDefault();
    if (!game4SubjectId) return;
    predictGame4Mutation.mutate({
      subjectId: Number(game4SubjectId),
      predictedBankSize: Number(game4BankSize),
    });
  };

  const handleToggleLlmQuestion = (questionId: number) => {
    if (selectedLlmQuestionIds.includes(questionId)) {
      setSelectedLlmQuestionIds(selectedLlmQuestionIds.filter((id) => id !== questionId));
    } else {
      setSelectedLlmQuestionIds([...selectedLlmQuestionIds, questionId]);
    }
  };

  const handleSubmitGame6 = () => {
    if (!activeGame6) return;
    submitGame6Mutation.mutate({
      minigameSessionId: activeGame6.sessionId,
      selectedLlmQuestionIds,
    });
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Sub-navigation for Minigames */}
      <div className="flex items-center gap-2 p-1.5 bg-white dark:bg-slate-800/80 rounded-2xl border border-slate-200/80 dark:border-slate-700 shadow-xs overflow-x-auto scrollbar-none">
        <button
          type="button"
          onClick={() => setActiveGame('game1')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeGame === 'game1'
              ? 'bg-indigo-600 text-white shadow-xs'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Game 1: Nộp Bài Ngày Mai</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveGame('game2')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeGame === 'game2'
              ? 'bg-indigo-600 text-white shadow-xs'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'
          }`}
        >
          <Bot className="w-4 h-4" />
          <span>Game 2: Tỷ Lệ Duyệt Đề</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveGame('game4')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeGame === 'game4'
              ? 'bg-indigo-600 text-white shadow-xs'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>Game 4: Quy Mô Cuối Kỳ</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveGame('game6')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeGame === 'game6'
              ? 'bg-indigo-600 text-white shadow-xs'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'
          }`}
        >
          <BrainCircuit className="w-4 h-4" />
          <span>Game 6: Phán Đoán AI</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveGame('history')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all ml-auto cursor-pointer ${
            activeGame === 'history'
              ? 'bg-indigo-600 text-white shadow-xs'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Lịch Sử Dự Đoán</span>
        </button>
      </div>

      {/* GAME 1: PARTICIPANTS PREDICTION */}
      {activeGame === 'game1' && (
        <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-6">
          <div className="space-y-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
              Minigame Hằng Ngày • Chốt cược 22:00
            </span>
            <h3 className="text-xl font-black text-slate-900 dark:text-white">
              Game 1: Dự Đoán Số Sinh Viên Nộp Bài Ngày Mai
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xl">
              Chọn lớp học phần bạn đang tham gia và dự đoán có bao nhiêu sinh viên sẽ đề xuất câu hỏi trong ngày mai. Đoán trúng nhận ngay xu thưởng lớn!
            </p>
          </div>

          {isGame1Loading ? (
            <div className="h-40 rounded-2xl bg-slate-100 dark:bg-slate-700/50 animate-pulse" />
          ) : game1Classes.length === 0 ? (
            <div className="p-8 text-center rounded-2xl bg-slate-50 dark:bg-slate-900/40 border border-slate-200 dark:border-slate-800 text-slate-500">
              <Users className="w-10 h-10 mx-auto mb-2 opacity-30" />
              <p className="font-semibold text-sm">Bạn chưa tham gia lớp học phần nào trong học kỳ này.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {game1Classes.map((cls) => {
                const isSelected = selectedClassId === cls.courseClassId;
                return (
                  <div
                    key={cls.courseClassId}
                    onClick={() => !cls.alreadyPredicted && setSelectedClassId(cls.courseClassId)}
                    className={`p-4 rounded-2xl border transition-all cursor-pointer ${
                      cls.alreadyPredicted
                        ? 'bg-slate-50 dark:bg-slate-900/40 border-slate-200 dark:border-slate-800 opacity-60 cursor-not-allowed'
                        : isSelected
                        ? 'bg-indigo-50/80 dark:bg-indigo-950/60 border-indigo-500 ring-2 ring-indigo-500/20 shadow-sm'
                        : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 hover:border-indigo-300'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                        {cls.classCode}
                      </span>
                      {cls.alreadyPredicted ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300 flex items-center gap-1">
                          <CheckCircle className="w-3 h-3" /> Đã đặt: {cls.predictedCount} SV
                        </span>
                      ) : (
                        <span className="text-[11px] text-slate-400">Kỳ {cls.semester}</span>
                      )}
                    </div>
                    <p className="font-bold text-sm text-slate-900 dark:text-white truncate">
                      {cls.subjectName}
                    </p>
                    <p className="text-xs text-slate-500 mt-1">GV: {cls.lecturerName}</p>
                  </div>
                );
              })}
            </div>
          )}

          {selectedClassId && (
            <form onSubmit={handlePredictGame1} className="pt-4 border-t border-slate-200 dark:border-slate-700 flex flex-col sm:flex-row items-center gap-3">
              <div className="flex-1 w-full">
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  Số lượng sinh viên bạn dự đoán:
                </label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={game1Count}
                  onChange={(e) => setGame1Count(Number(e.target.value))}
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm font-bold text-slate-800 dark:text-slate-100"
                />
              </div>
              <button
                type="submit"
                disabled={predictGame1Mutation.isPending}
                className="w-full sm:w-auto mt-auto px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
              >
                <Send className="w-4 h-4" />
                <span>{predictGame1Mutation.isPending ? 'Đang gửi...' : 'Xác Nhận Dự Đoán'}</span>
              </button>
            </form>
          )}
        </div>
      )}

      {/* GAME 2: APPROVED QUESTIONS PREDICTION */}
      {activeGame === 'game2' && (
        <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-6">
          <div className="space-y-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-purple-100 dark:bg-purple-900/60 text-purple-600 dark:text-purple-400">
              Dành Cho Tác Giả Phiên Đề Xuất
            </span>
            <h3 className="text-xl font-black text-slate-900 dark:text-white">
              Game 2: Dự Đoán Số Câu Hỏi Được Giảng Viên Duyệt
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xl">
              Khi bạn đề xuất một phiên câu hỏi, hãy dự đoán có bao nhiêu câu do AI (LLM) và Con người tạo ra sẽ được duyệt chính thức vào ngân hàng đề!
            </p>
          </div>

          <form onSubmit={handlePredictGame2} className="space-y-4 max-w-md">
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                Mã Phiên Đề Xuất (Session ID):
              </label>
              <input
                type="number"
                required
                placeholder="Ví dụ: 12"
                value={game2SessionId}
                onChange={(e) => setGame2SessionId(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm font-bold text-slate-800 dark:text-slate-100"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  Số câu AI được duyệt:
                </label>
                <input
                  type="number"
                  min="0"
                  value={game2LlmCount}
                  onChange={(e) => setGame2LlmCount(Number(e.target.value))}
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm font-bold text-slate-800 dark:text-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  Số câu Con Người được duyệt:
                </label>
                <input
                  type="number"
                  min="0"
                  value={game2HumanCount}
                  onChange={(e) => setGame2HumanCount(Number(e.target.value))}
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm font-bold text-slate-800 dark:text-slate-100"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={predictGame2Mutation.isPending}
              className="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
            >
              <Send className="w-4 h-4" />
              <span>{predictGame2Mutation.isPending ? 'Đang gửi...' : 'Gửi Dự Đoán Game 2'}</span>
            </button>
          </form>
        </div>
      )}

      {/* GAME 4: FINAL BANK SIZE */}
      {activeGame === 'game4' && (
        <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-6">
          <div className="space-y-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-emerald-100 dark:bg-emerald-900/60 text-emerald-600 dark:text-emerald-400">
              Giải Thưởng Cuối Học Kỳ • Top 3 Nhận Thưởng Lớn
            </span>
            <h3 className="text-xl font-black text-slate-900 dark:text-white">
              Game 4: Dự Đoán Quy Mô Ngân Hàng Câu Hỏi Môn Học
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xl">
              Dự đoán tổng số lượng câu hỏi sẽ được phê duyệt vào ngân hàng đề thi của môn học khi học kỳ khép lại.
            </p>
          </div>

          <form onSubmit={handlePredictGame4} className="space-y-4 max-w-md">
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                Mã Môn Học (Subject ID):
              </label>
              <input
                type="number"
                required
                placeholder="Ví dụ: 10"
                value={game4SubjectId}
                onChange={(e) => setGame4SubjectId(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm font-bold text-slate-800 dark:text-slate-100"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                Dự đoán tổng số câu hỏi khi kết thúc kỳ:
              </label>
              <input
                type="number"
                min="10"
                value={game4BankSize}
                onChange={(e) => setGame4BankSize(Number(e.target.value))}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm font-bold text-slate-800 dark:text-slate-100"
              />
            </div>

            <button
              type="submit"
              disabled={predictGame4Mutation.isPending}
              className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
            >
              <Send className="w-4 h-4" />
              <span>{predictGame4Mutation.isPending ? 'Đang gửi...' : 'Gửi Dự Đoán Game 4'}</span>
            </button>
          </form>
        </div>
      )}

      {/* GAME 6: AI VS HUMAN CHALLENGE */}
      {activeGame === 'game6' && (
        <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-6">
          <div className="space-y-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-pink-100 dark:bg-pink-900/60 text-pink-600 dark:text-pink-400">
              Thử Thách Định Kỳ Thứ 7 Hàng Tuần
            </span>
            <h3 className="text-xl font-black text-slate-900 dark:text-white">
              Game 6: Phán Đoán Câu Hỏi AI vs Con Người
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xl">
              Hệ thống đưa ra 7 câu hỏi trắc nghiệm trộn lẫn giữa người biên soạn và mô hình AI sinh ra. Hãy chọn những câu hỏi bạn tin rằng do AI tạo!
            </p>
          </div>

          {isGame6Loading ? (
            <div className="h-48 rounded-2xl bg-slate-100 dark:bg-slate-700/50 animate-pulse" />
          ) : !activeGame6 ? (
            <div className="p-8 text-center rounded-2xl bg-slate-50 dark:bg-slate-900/40 border border-slate-200 dark:border-slate-800 text-slate-500">
              <BrainCircuit className="w-12 h-12 mx-auto mb-2 opacity-30 text-pink-500" />
              <p className="font-semibold text-sm">Hiện tại chưa có phiên Game 6 nào đang mở.</p>
              <p className="text-xs text-slate-400 mt-1">Phiên thử thách mở định kỳ vào Thứ 7 hằng tuần. Hẹn gặp lại bạn!</p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between text-xs text-slate-500">
                <span>Tuần {activeGame6.weekNumber} / {activeGame6.year}</span>
                <span>Đã chọn: <strong className="text-indigo-600 dark:text-indigo-400">{selectedLlmQuestionIds.length}</strong> câu AI</span>
              </div>

              <div className="space-y-3">
                {activeGame6.questions.map((q, idx) => {
                  const isChecked = selectedLlmQuestionIds.includes(q.questionId);
                  return (
                    <div
                      key={q.questionId}
                      onClick={() => handleToggleLlmQuestion(q.questionId)}
                      className={`p-4 rounded-2xl border transition-all cursor-pointer ${
                        isChecked
                          ? 'bg-pink-50 dark:bg-pink-950/40 border-pink-400 ring-2 ring-pink-400/20'
                          : 'bg-slate-50/50 dark:bg-slate-900/50 border-slate-200 dark:border-slate-700 hover:border-pink-200'
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => {}}
                          className="mt-1 w-4 h-4 rounded-sm text-pink-600 focus:ring-pink-500"
                        />
                        <div className="space-y-1">
                          <p className="text-xs font-bold text-slate-800 dark:text-slate-200">
                            Câu {idx + 1}: {q.content}
                          </p>
                          {q.options && q.options.length > 0 && (
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 pt-1">
                              {q.options.map((opt) => (
                                <span key={opt.key} className="text-[11px] text-slate-500">
                                  <strong>{opt.key}.</strong> {opt.content}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              <button
                type="button"
                disabled={submitGame6Mutation.isPending || selectedLlmQuestionIds.length === 0}
                onClick={handleSubmitGame6}
                className="w-full py-3.5 rounded-2xl bg-pink-600 hover:bg-pink-500 text-white font-black text-sm flex items-center justify-center gap-2 shadow-lg shadow-pink-500/20 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer disabled:opacity-50"
              >
                <Send className="w-4 h-4" />
                <span>{submitGame6Mutation.isPending ? 'Đang gửi...' : 'Nộp Dự Đoán Game 6'}</span>
              </button>
            </div>
          )}
        </div>
      )}

      {/* MY PREDICTIONS HISTORY */}
      {activeGame === 'history' && (
        <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-4">
          <div className="space-y-1">
            <h3 className="text-xl font-black text-slate-900 dark:text-white">
              Lịch Sử Các Lượt Dự Đoán
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Theo dõi trạng thái và kết quả trúng/trượt từ các minigame bạn đã tham gia.
            </p>
          </div>

          {isHistoryLoading ? (
            <div className="h-40 rounded-2xl bg-slate-100 dark:bg-slate-700/50 animate-pulse" />
          ) : predictionsList.length === 0 ? (
            <div className="py-12 text-center text-slate-400">
              <History className="w-10 h-10 mx-auto mb-2 opacity-30" />
              <p className="font-semibold text-sm">Bạn chưa tham gia dự đoán minigame nào</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100 dark:divide-slate-700/60">
              {predictionsList.map((item) => {
                const isPending = item.status === 'PENDING';
                const isCorrect = item.isCorrect;

                return (
                  <div key={item.predictionId} className="py-3.5 flex items-center justify-between gap-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-800 dark:text-slate-200">
                          {item.gameType}
                        </span>
                        {isPending ? (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300 flex items-center gap-1">
                            <Clock className="w-3 h-3" /> Đang chờ
                          </span>
                        ) : isCorrect ? (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300 flex items-center gap-1">
                            <CheckCircle className="w-3 h-3" /> Đoán trúng 🎯
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300 flex items-center gap-1">
                            <XCircle className="w-3 h-3" /> Chưa chính xác
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-slate-400">
                        Ngày tạo: {new Date(item.createdAt).toLocaleString('vi-VN')}
                      </p>
                    </div>

                    <div className="text-right text-xs">
                      <p className="text-slate-500">Mục tiêu: #{item.targetId}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
