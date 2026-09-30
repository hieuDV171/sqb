import { useState, useEffect } from 'react';
import {
  BookOpen,
  Plus,
  Trash2,
  Bot,
  User,
  CheckCircle2,
  Send,
  X,
  Check,
  Gamepad2,
  ImageIcon,
  Loader2,
  RotateCcw,
  HelpCircle,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { mediaService } from '@/services/mediaService';
import { toast } from '@/stores/useToastStore';
import { useEnrolledSubjects } from '../hooks/useEnrolledSubjects';
import { useSessionMutations } from '../hooks/useSessionMutations';
import type {
  QuestionProposeDto,
  ProposeSessionResponse,
  ProposeSessionRequest,
} from '../types/session.types';

interface ProposeSessionFormProps {
  onSuccessNavigate?: () => void;
  onViewMySubmissions?: () => void;
}

export const ProposeSessionForm: React.FC<ProposeSessionFormProps> = ({
  onSuccessNavigate,
  onViewMySubmissions,
}) => {
  const { data: subjects = [], isLoading: isLoadingSubjects } = useEnrolledSubjects();
  const { proposeMutation, predictGame2Mutation } = useSessionMutations();

  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(null);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');

  const [questions, setQuestions] = useState<QuestionProposeDto[]>([
    createEmptyQuestion(1),
  ]);

  const [uploadingTarget, setUploadingTarget] = useState<string | null>(null);
  const [deleteIndex, setDeleteIndex] = useState<number | null>(null);

  // Game 2 Modal state
  const [isGame2ModalOpen, setIsGame2ModalOpen] = useState(false);
  const [predictedLlmCount, setPredictedLlmCount] = useState<number>(0);
  const [predictedHumanCount, setPredictedHumanCount] = useState<number>(0);

  // Success Response modal
  const [successResponse, setSuccessResponse] = useState<{
    session: ProposeSessionResponse;
    game2Predicted?: boolean;
    predLlm?: number | null;
    predHuman?: number | null;
  } | null>(null);

  // Draft auto-save state
  const DRAFT_KEY = 'sqb_propose_session_draft_v2';
  const [draftInfo, setDraftInfo] = useState<{ updatedAt: string } | null>(null);
  const [zoomedImage, setZoomedImage] = useState<string | null>(null);

  const getOptionKey = (idx: number) => String.fromCharCode(65 + idx);

  function createEmptyQuestion(_index: number): QuestionProposeDto {
    return {
      content: '',
      mediaUrls: [],
      options: [
        { key: 'A', text: '', isCorrect: true, mediaUrl: '' },
        { key: 'B', text: '', isCorrect: false, mediaUrl: '' },
        { key: 'C', text: '', isCorrect: false, mediaUrl: '' },
        { key: 'D', text: '', isCorrect: false, mediaUrl: '' },
      ],
      explanation: '',
      llmGenerated: false,
      confidence: 3.5,
    };
  }

  // Auto-select first subject if not set
  useEffect(() => {
    if (subjects.length > 0 && selectedSubjectId === null) {
      setSelectedSubjectId(subjects[0].subjectId);
    }
  }, [subjects, selectedSubjectId]);

  // Check existing draft
  useEffect(() => {
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (
          parsed &&
          (parsed.title || (parsed.questions && parsed.questions.length > 1) || parsed.questions?.[0]?.content)
        ) {
          setDraftInfo({
            updatedAt: new Date(parsed.updatedAt || Date.now()).toLocaleString('vi-VN'),
          });
        }
      }
    } catch {
      // Ignore JSON parse errors
    }
  }, []);

  // Save draft periodically
  useEffect(() => {
    const hasData =
      title.trim() ||
      content.trim() ||
      questions.some((q) => q.content.trim() || (q.mediaUrls && q.mediaUrls.length > 0));

    if (!hasData) return;

    const timer = setTimeout(() => {
      try {
        const draft = {
          selectedSubjectId,
          title,
          content,
          sourceUrl,
          questions,
          updatedAt: new Date().toISOString(),
        };
        localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
      } catch (e) {
        console.warn('Lỗi ghi bản nháp', e);
      }
    }, 1000);

    return () => clearTimeout(timer);
  }, [selectedSubjectId, title, content, sourceUrl, questions]);

  const handleRestoreDraft = () => {
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      if (parsed.selectedSubjectId) setSelectedSubjectId(parsed.selectedSubjectId);
      if (parsed.title) setTitle(parsed.title);
      if (parsed.content) setContent(parsed.content);
      if (parsed.sourceUrl) setSourceUrl(parsed.sourceUrl);
      if (parsed.questions && Array.isArray(parsed.questions) && parsed.questions.length > 0) {
        setQuestions(parsed.questions);
      }
      setDraftInfo(null);
      toast.success('Đã khôi phục dữ liệu bản nháp phiên thành công!');
    } catch {
      toast.error('Không thể khôi phục bản nháp');
    }
  };

  const handleDiscardDraft = () => {
    localStorage.removeItem(DRAFT_KEY);
    setDraftInfo(null);
    toast.info('Đã hủy bỏ bản nháp cũ');
  };

  const llmQuestionsCount = questions.filter((q) => q.llmGenerated).length;
  const humanQuestionsCount = questions.length - llmQuestionsCount;

  const handleAddQuestion = () => {
    setQuestions([...questions, createEmptyQuestion(questions.length + 1)]);
    toast.info(`Đã thêm câu hỏi #${questions.length + 1}`);
  };

  const handleConfirmDelete = () => {
    if (deleteIndex === null) return;
    if (questions.length <= 1) {
      toast.warning('Phiên đề xuất phải chứa ít nhất 1 câu hỏi!');
      setDeleteIndex(null);
      return;
    }
    const updated = questions.filter((_, idx) => idx !== deleteIndex);
    setQuestions(updated);
    toast.success(`Đã xóa câu hỏi #${deleteIndex + 1}`);
    setDeleteIndex(null);
  };

  const handleUpdateQuestion = (index: number, field: keyof QuestionProposeDto, value: any) => {
    const updated = [...questions];
    updated[index] = { ...updated[index], [field]: value };
    setQuestions(updated);
  };

  const handleUploadQuestionImage = async (qIndex: number, file: File) => {
    if (!file.type.startsWith('image/')) {
      toast.error('Chỉ hỗ trợ tệp định dạng hình ảnh (PNG, JPG, WEBP)');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      toast.error('Dung lượng tệp tối đa là 10MB');
      return;
    }

    const targetKey = `q-${qIndex}`;
    setUploadingTarget(targetKey);
    try {
      const res = await mediaService.uploadViaPresign(file, 'QUESTION');
      if (res.publicUrl) {
        const updated = [...questions];
        const current = updated[qIndex].mediaUrls || [];
        updated[qIndex].mediaUrls = [...current, res.publicUrl];
        setQuestions(updated);
        toast.success(`Đã tải ảnh lên thành công!`);
      }
    } catch (err: any) {
      toast.error(err?.message || 'Lỗi khi tải ảnh câu hỏi lên');
    } finally {
      setUploadingTarget(null);
    }
  };

  const handleUploadOptionImage = async (qIndex: number, optKey: string, file: File) => {
    if (!file.type.startsWith('image/')) {
      toast.error('Chỉ hỗ trợ tải lên hình ảnh');
      return;
    }
    const targetKey = `opt-${qIndex}-${optKey}`;
    setUploadingTarget(targetKey);
    try {
      const res = await mediaService.uploadViaPresign(file, 'QUESTION');
      if (res.publicUrl) {
        const updated = [...questions];
        const opts = [...updated[qIndex].options];
        const optIdx = opts.findIndex((o) => o.key === optKey);
        if (optIdx !== -1) {
          opts[optIdx] = { ...opts[optIdx], mediaUrl: res.publicUrl };
          updated[qIndex].options = opts;
          setQuestions(updated);
          toast.success(`Đã gắn ảnh vào đáp án ${optKey}`);
        }
      }
    } catch (err: any) {
      toast.error(err?.message || 'Lỗi khi tải ảnh đáp án');
    } finally {
      setUploadingTarget(null);
    }
  };

  const handleAddOption = (qIndex: number) => {
    const updated = [...questions];
    const currentOpts = updated[qIndex].options;
    const nextKey = getOptionKey(currentOpts.length);
    updated[qIndex].options = [
      ...currentOpts,
      { key: nextKey, text: '', isCorrect: false, mediaUrl: '' },
    ];
    setQuestions(updated);
    toast.info(`Đã thêm đáp án ${nextKey} vào câu hỏi #${qIndex + 1}`);
  };

  const handleRemoveOption = (qIndex: number, optIndex: number) => {
    const updated = [...questions];
    const currentOpts = updated[qIndex].options;
    if (currentOpts.length <= 2) {
      toast.warning('Mỗi câu hỏi trắc nghiệm phải có ít nhất 2 đáp án lựa chọn');
      return;
    }
    const removedKey = currentOpts[optIndex].key;
    const filtered = currentOpts.filter((_, i) => i !== optIndex);
    updated[qIndex].options = filtered.map((opt, i) => ({
      ...opt,
      key: getOptionKey(i),
    }));
    setQuestions(updated);
    toast.info(`Đã xóa đáp án ${removedKey} khỏi câu hỏi #${qIndex + 1}`);
  };

  const handleRemoveOptionImage = (qIndex: number, optKey: string) => {
    const updated = [...questions];
    const opts = updated[qIndex].options.map((opt) =>
      opt.key === optKey ? { ...opt, mediaUrl: undefined } : opt
    );
    updated[qIndex].options = opts;
    setQuestions(updated);
    toast.info(`Đã gỡ ảnh đính kèm của đáp án ${optKey}`);
  };

  const handleToggleCorrectOption = (qIndex: number, optKey: string) => {
    const updated = [...questions];
    const opts = updated[qIndex].options.map((opt) =>
      opt.key === optKey ? { ...opt, isCorrect: !opt.isCorrect } : opt
    );
    updated[qIndex].options = opts;
    setQuestions(updated);
  };

  const handleUpdateOptionText = (qIndex: number, optKey: string, text: string) => {
    const updated = [...questions];
    const opts = updated[qIndex].options.map((opt) =>
      opt.key === optKey ? { ...opt, text } : opt
    );
    updated[qIndex].options = opts;
    setQuestions(updated);
  };

  const validateForm = (): boolean => {
    if (!selectedSubjectId) {
      toast.error('Vui lòng chọn môn học bạn đang theo học');
      return false;
    }
    if (!title.trim()) {
      toast.error('Vui lòng nhập tiêu đề cho phiên đề xuất');
      return false;
    }
    if (questions.length === 0) {
      toast.error('Phiên đề xuất phải chứa ít nhất 1 câu hỏi');
      return false;
    }

    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.content.trim()) {
        toast.error(`Câu hỏi số #${i + 1} chưa có nội dung đề bài`);
        return false;
      }
      if (q.options.length < 2) {
        toast.error(`Câu hỏi số #${i + 1} phải có ít nhất 2 đáp án lựa chọn`);
        return false;
      }
      for (const opt of q.options) {
        if (!opt.text.trim()) {
          toast.error(`Đáp án ${opt.key} của câu hỏi #${i + 1} chưa được nhập nội dung`);
          return false;
        }
      }
      const hasCorrect = q.options.some((o) => o.isCorrect);
      if (!hasCorrect) {
        toast.error(`Vui lòng chọn ít nhất 1 đáp án đúng cho câu hỏi #${i + 1}`);
        return false;
      }
    }
    return true;
  };

  const handleSubmitSession = async (withGame2 = false) => {
    if (!validateForm()) return;

    const payload: ProposeSessionRequest = {
      title: title.trim(),
      content: content.trim() || undefined,
      sourceUrl: sourceUrl.trim() || undefined,
      subjectId: selectedSubjectId!,
      questions: questions.map((q) => ({
        content: q.content.trim(),
        mediaUrls: q.mediaUrls && q.mediaUrls.length > 0 ? q.mediaUrls : undefined,
        options: q.options.map((opt) => ({
          key: opt.key,
          text: opt.text.trim(),
          isCorrect: opt.isCorrect,
          mediaUrl: opt.mediaUrl?.trim() || undefined,
        })),
        explanation: q.explanation?.trim() || undefined,
        llmGenerated: q.llmGenerated,
        confidence: q.confidence,
      })),
    };

    try {
      const sessionRes = await proposeMutation.mutateAsync(payload);

      let game2Result = false;
      if (withGame2 && sessionRes.sessionId) {
        try {
          await predictGame2Mutation.mutateAsync({
            sessionId: sessionRes.sessionId,
            predictedLlmCount,
            predictedHumanCount,
          });
          game2Result = true;
        } catch (e) {
          console.warn('Lỗi đặt cược game 2', e);
        }
      }

      localStorage.removeItem(DRAFT_KEY);

      setSuccessResponse({
        session: sessionRes,
        game2Predicted: game2Result,
        predLlm: withGame2 ? predictedLlmCount : null,
        predHuman: withGame2 ? predictedHumanCount : null,
      });

      setIsGame2ModalOpen(false);
    } catch {
      // Handled by mutation
    }
  };

  const selectedSubject = subjects.find((s) => s.subjectId === selectedSubjectId);

  return (
    <div className="space-y-6">
      {/* Draft Notification Banner */}
      {draftInfo && (
        <div className="bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/80 rounded-2xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xs">
          <div className="flex items-center gap-3">
            <RotateCcw className="w-5 h-5 text-amber-600 shrink-0" />
            <div className="text-xs">
              <span className="font-bold text-amber-900 dark:text-amber-200 block">
                Tìm thấy bản nháp phiên lưu từ phiên trước
              </span>
              <span className="text-amber-700 dark:text-amber-400">
                Lưu lúc: {draftInfo.updatedAt}
              </span>
            </div>
          </div>
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              type="button"
              onClick={handleRestoreDraft}
              className="flex-1 sm:flex-initial px-3.5 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
            >
              Khôi phục nháp
            </button>
            <button
              type="button"
              onClick={handleDiscardDraft}
              className="flex-1 sm:flex-initial px-3 py-1.5 rounded-xl border border-amber-300 dark:border-amber-800 text-amber-800 dark:text-amber-300 hover:bg-amber-100 dark:hover:bg-amber-900/40 text-xs font-medium transition-colors cursor-pointer"
            >
              Bỏ qua
            </button>
          </div>
        </div>
      )}

      {/* Main Info Card */}
      <div className="bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-3xl p-5 sm:p-6 shadow-xs backdrop-blur-md space-y-5">
        <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-4">
          <div>
            <h3 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <BookOpen className="w-5 h-5 text-indigo-600" />
              <span>Thông tin phiên đề xuất</span>
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Đóng góp các câu hỏi trắc nghiệm theo môn học của học kỳ hiện tại
            </p>
          </div>

          <div className="hidden sm:flex items-center gap-2 text-xs">
            <span className="px-2.5 py-1 rounded-xl bg-purple-50 dark:bg-purple-950/40 text-purple-700 dark:text-purple-300 font-semibold border border-purple-200/60 dark:border-purple-800/60">
              AI: {llmQuestionsCount}
            </span>
            <span className="px-2.5 py-1 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 font-semibold border border-indigo-200/60 dark:border-indigo-800/60">
              Tự soạn: {humanQuestionsCount}
            </span>
          </div>
        </div>

        {/* Inputs */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
          {/* Subject Dropdown */}
          <div className="md:col-span-5 space-y-1.5">
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
              Môn học tham gia *
            </label>
            <select
              value={selectedSubjectId || ''}
              onChange={(e) => setSelectedSubjectId(Number(e.target.value))}
              disabled={isLoadingSubjects}
              className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 cursor-pointer"
            >
              {isLoadingSubjects ? (
                <option value="">Đang tải danh sách môn học...</option>
              ) : subjects.length === 0 ? (
                <option value="">Chưa có môn học được phân bổ</option>
              ) : (
                subjects.map((sub) => (
                  <option key={sub.subjectId} value={sub.subjectId}>
                    [{sub.code}] {sub.name}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Session Title */}
          <div className="md:col-span-7 space-y-1.5">
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
              Tiêu đề phiên câu hỏi *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: Trắc nghiệm Chương 3: Định tuyến mạng IP và BGP..."
              className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          {/* Session Content / Note */}
          <div className="md:col-span-8 space-y-1.5">
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
              Mô tả nội dung / Ghi chú gửi Giảng viên
            </label>
            <input
              type="text"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="VD: Tổng hợp các dạng bài tập thực hành trắc nghiệm có sơ đồ mạng..."
              className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          {/* Source URL */}
          <div className="md:col-span-4 space-y-1.5">
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
              Nguồn tài liệu tham khảo (URL)
            </label>
            <input
              type="text"
              value={sourceUrl}
              onChange={(e) => setSourceUrl(e.target.value)}
              placeholder="https://..."
              className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>
        </div>
      </div>

      {/* Questions Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <HelpCircle className="w-5 h-5 text-indigo-600" />
            <span>Danh sách câu hỏi trắc nghiệm ({questions.length})</span>
          </h3>

          <button
            type="button"
            onClick={handleAddQuestion}
            className="flex items-center gap-1.5 px-4 py-2 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 text-xs sm:text-sm font-semibold transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm câu hỏi</span>
          </button>
        </div>

        {/* Questions List */}
        <div className="space-y-5">
          {questions.map((q, qIndex) => (
            <div
              key={qIndex}
              className="bg-white/90 dark:bg-slate-900/90 border border-slate-200/80 dark:border-slate-800/80 rounded-3xl p-5 sm:p-6 shadow-xs backdrop-blur-xs space-y-4"
            >
              {/* Question Card Header */}
              <div className="flex items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 pb-3 flex-wrap">
                <div className="flex items-center gap-2">
                  <span className="w-7 h-7 rounded-xl bg-indigo-600 text-white font-bold text-xs flex items-center justify-center shadow-xs">
                    {qIndex + 1}
                  </span>
                  <span className="text-sm font-bold text-slate-800 dark:text-slate-200">
                    Câu hỏi #{qIndex + 1}
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  {/* LLM vs Human toggle */}
                  <button
                    type="button"
                    onClick={() =>
                      handleUpdateQuestion(qIndex, 'llmGenerated', !q.llmGenerated)
                    }
                    className={cn(
                      'flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-semibold border transition-all cursor-pointer',
                      q.llmGenerated
                        ? 'bg-purple-50 dark:bg-purple-950/40 text-purple-700 dark:text-purple-300 border-purple-200 dark:border-purple-800'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700'
                    )}
                  >
                    {q.llmGenerated ? (
                      <>
                        <Bot className="w-3.5 h-3.5 text-purple-600" />
                        <span>AI sinh ra</span>
                      </>
                    ) : (
                      <>
                        <User className="w-3.5 h-3.5 text-slate-600 dark:text-slate-300" />
                        <span>Tự biên soạn</span>
                      </>
                    )}
                  </button>

                  {/* Delete question button */}
                  {questions.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setDeleteIndex(qIndex)}
                      className="p-1.5 rounded-xl text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>

              {/* Question Content */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Nội dung đề bài *
                </label>
                <textarea
                  value={q.content}
                  onChange={(e) => handleUpdateQuestion(qIndex, 'content', e.target.value)}
                  rows={3}
                  placeholder="Nhập nội dung câu hỏi trắc nghiệm..."
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>

              {/* Question Media Attachments */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-600 dark:text-slate-400 flex items-center gap-1.5">
                    <ImageIcon className="w-3.5 h-3.5" />
                    <span>Hình ảnh minh họa đề bài</span>
                  </span>
                  <label className="cursor-pointer text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1">
                    <Plus className="w-3 h-3" />
                    <span>Thêm ảnh</span>
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) handleUploadQuestionImage(qIndex, file);
                        e.target.value = '';
                      }}
                    />
                  </label>
                </div>

                {uploadingTarget === `q-${qIndex}` && (
                  <div className="flex items-center gap-2 p-2 bg-indigo-50 dark:bg-indigo-950/40 rounded-xl text-xs text-indigo-600">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang tải ảnh lên MinIO S3...</span>
                  </div>
                )}

                {q.mediaUrls && q.mediaUrls.length > 0 && (
                  <div className="flex gap-2 flex-wrap pt-1">
                    {q.mediaUrls.map((url, imgIdx) => (
                      <div key={imgIdx} className="relative group">
                        <img
                          src={url}
                          alt="Ảnh đề bài"
                          className="w-20 h-20 object-cover rounded-xl border border-slate-200 dark:border-slate-700"
                        />
                        <button
                          type="button"
                          onClick={() => {
                            const updated = [...questions];
                            updated[qIndex].mediaUrls = updated[qIndex].mediaUrls?.filter(
                              (_, i) => i !== imgIdx
                            );
                            setQuestions(updated);
                          }}
                          className="absolute -top-1.5 -right-1.5 w-5 h-5 bg-rose-600 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-xs cursor-pointer"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Options */}
              <div className="space-y-2.5 pt-2">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                    Các đáp án lựa chọn (Click nút chữ cái để bật/tắt đáp án ĐÚNG - hỗ trợ nhiều đáp án đúng):
                  </label>
                  <button
                    type="button"
                    onClick={() => handleAddOption(qIndex)}
                    className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 text-xs font-semibold transition-colors cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Thêm đáp án ({getOptionKey(q.options.length)})</span>
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {q.options.map((opt, optIdx) => (
                    <div
                      key={opt.key}
                      className={cn(
                        'flex flex-col gap-2 p-2.5 rounded-2xl border transition-all',
                        opt.isCorrect
                          ? 'bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-400 dark:border-emerald-800'
                          : 'bg-slate-50 dark:bg-slate-800/40 border-slate-200 dark:border-slate-700'
                      )}
                    >
                      <div className="flex items-center gap-2">
                        {/* Toggle correct answer */}
                        <button
                          type="button"
                          onClick={() => handleToggleCorrectOption(qIndex, opt.key)}
                          title="Nhấn để chọn / bỏ chọn đáp án ĐÚNG"
                          className={cn(
                            'w-7 h-7 rounded-xl font-bold text-xs flex items-center justify-center shrink-0 cursor-pointer transition-all',
                            opt.isCorrect
                              ? 'bg-emerald-600 text-white shadow-xs'
                              : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-300'
                          )}
                        >
                          {opt.isCorrect ? <Check className="w-4 h-4" /> : opt.key}
                        </button>

                        {/* Option text */}
                        <input
                          type="text"
                          value={opt.text}
                          onChange={(e) =>
                            handleUpdateOptionText(qIndex, opt.key, e.target.value)
                          }
                          placeholder={`Nội dung đáp án ${opt.key}...`}
                          className="flex-1 bg-transparent border-none text-xs sm:text-sm text-slate-900 dark:text-white focus:outline-none"
                        />

                        {/* Upload image button */}
                        <label
                          title={`Tải ảnh cho đáp án ${opt.key}`}
                          className="cursor-pointer p-1.5 rounded-lg text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-slate-700 shrink-0 transition-colors"
                        >
                          {uploadingTarget === `opt-${qIndex}-${opt.key}` ? (
                            <Loader2 className="w-4 h-4 animate-spin text-indigo-600" />
                          ) : (
                            <ImageIcon className="w-4 h-4" />
                          )}
                          <input
                            type="file"
                            accept="image/*"
                            className="hidden"
                            onChange={(e) => {
                              const file = e.target.files?.[0];
                              if (file) handleUploadOptionImage(qIndex, opt.key, file);
                              e.target.value = '';
                            }}
                          />
                        </label>

                        {/* Remove option button (if > 2 options) */}
                        {q.options.length > 2 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveOption(qIndex, optIdx)}
                            title={`Xóa đáp án ${opt.key}`}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-slate-700 shrink-0 transition-colors cursor-pointer"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>

                      {/* Option Image Preview thumbnail if present */}
                      {opt.mediaUrl && (
                        <div className="flex items-center gap-2 pl-9 pt-0.5">
                          <div className="relative group/optimg">
                            <img
                              src={opt.mediaUrl}
                              alt={`Ảnh đáp án ${opt.key}`}
                              onClick={() => setZoomedImage(opt.mediaUrl!)}
                              className="w-14 h-14 object-cover rounded-xl border border-slate-200 dark:border-slate-700 cursor-zoom-in shadow-xs hover:opacity-90 transition-opacity"
                            />
                            <button
                              type="button"
                              onClick={() => handleRemoveOptionImage(qIndex, opt.key)}
                              title="Gỡ ảnh đáp án"
                              className="absolute -top-1.5 -right-1.5 w-5 h-5 bg-rose-600 text-white rounded-full flex items-center justify-center opacity-90 group-hover/optimg:opacity-100 shadow-xs cursor-pointer"
                            >
                              <X className="w-3 h-3" />
                            </button>
                          </div>
                          <span className="text-[11px] text-slate-400">
                            Đã đính kèm ảnh (Click để phóng to)
                          </span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Explanation */}
              <div className="space-y-1.5 pt-1">
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300">
                  Giải thích đáp án chi tiết (nếu có)
                </label>
                <input
                  type="text"
                  value={q.explanation || ''}
                  onChange={(e) =>
                    handleUpdateQuestion(qIndex, 'explanation', e.target.value)
                  }
                  placeholder="Giải thích vì sao đáp án được chọn là chính xác..."
                  className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Submit Footer Bar */}
      <div className="bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-3xl p-5 shadow-xs backdrop-blur-md flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="text-xs text-slate-500">
          Tổng cộng <strong className="text-indigo-600">{questions.length}</strong> câu hỏi
          đang sẵn sàng gửi duyệt.
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          {/* Direct Submit */}
          <button
            type="button"
            onClick={() => handleSubmitSession(false)}
            disabled={proposeMutation.isPending}
            className="flex-1 sm:flex-initial flex items-center justify-center gap-2 px-5 py-2.5 rounded-2xl border border-indigo-600 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 text-xs sm:text-sm font-semibold transition-colors cursor-pointer disabled:opacity-50"
          >
            {proposeMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
            <span>Nộp phiên trực tiếp</span>
          </button>

          {/* Submit with Game 2 Prediction */}
          <button
            type="button"
            onClick={() => {
              if (validateForm()) {
                setIsGame2ModalOpen(true);
              }
            }}
            disabled={proposeMutation.isPending}
            className="flex-1 sm:flex-initial flex items-center justify-center gap-2 px-5 py-2.5 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold shadow-md shadow-indigo-200 dark:shadow-none transition-all cursor-pointer disabled:opacity-50"
          >
            <Gamepad2 className="w-4 h-4 text-amber-300" />
            <span>Nộp & Chơi Game 2</span>
          </button>
        </div>
      </div>

      {/* Delete Question Modal */}
      {deleteIndex !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 max-w-sm w-full space-y-4 shadow-2xl">
            <h4 className="text-base font-bold text-slate-900 dark:text-white">
              Xóa câu hỏi #{deleteIndex + 1}?
            </h4>
            <p className="text-xs text-slate-600 dark:text-slate-400">
              Bạn có chắc chắn muốn xóa câu hỏi này khỏi danh sách đề xuất?
            </p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setDeleteIndex(null)}
                className="px-3.5 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-semibold"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                className="px-4 py-1.5 rounded-xl bg-rose-600 text-white text-xs font-semibold"
              >
                Xóa
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Game 2 Prediction Modal */}
      {isGame2ModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-10 h-10 rounded-2xl bg-amber-100 dark:bg-amber-950/60 text-amber-600 flex items-center justify-center">
                  <Gamepad2 className="w-6 h-6" />
                </div>
                <div>
                  <h4 className="text-base font-bold text-slate-900 dark:text-white">
                    Mini-game 2: Dự đoán kết quả duyệt
                  </h4>
                  <p className="text-[11px] text-slate-500">
                    Dự đoán chính xác để nhận thêm điểm thưởng và danh hiệu
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsGame2ModalOpen(false)}
                className="p-1 rounded-lg text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 bg-slate-50 dark:bg-slate-800/40 p-4 rounded-2xl border border-slate-100 dark:border-slate-800 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Số câu do AI (LLM) được duyệt (Max: {llmQuestionsCount})
                </label>
                <input
                  type="number"
                  min={0}
                  max={llmQuestionsCount}
                  value={predictedLlmCount}
                  onChange={(e) => setPredictedLlmCount(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Số câu tự biên soạn được duyệt (Max: {humanQuestionsCount})
                </label>
                <input
                  type="number"
                  min={0}
                  max={humanQuestionsCount}
                  value={predictedHumanCount}
                  onChange={(e) => setPredictedHumanCount(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsGame2ModalOpen(false)}
                className="px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-semibold"
              >
                Bỏ qua dự đoán
              </button>
              <button
                type="button"
                onClick={() => handleSubmitSession(true)}
                disabled={proposeMutation.isPending}
                className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-xs cursor-pointer"
              >
                Xác nhận nộp & Đặt cược
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Success Modal */}
      {successResponse && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl text-center space-y-4">
            <div className="w-16 h-16 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-9 h-9" />
            </div>

            <div>
              <h3 className="text-xl font-bold text-slate-900 dark:text-slate-100">
                Đề Xuất Phiên Thành Công!
              </h3>
              <p className="text-xs text-slate-500 mt-1">
                Mã phiên:{' '}
                <strong className="font-mono text-indigo-600 dark:text-indigo-400">
                  {successResponse.session.sessionCode}
                </strong>
              </p>
            </div>

            <div className="bg-slate-50 dark:bg-slate-800/50 p-4 rounded-2xl text-xs space-y-2 text-left border border-slate-100 dark:border-slate-800">
              <div className="flex justify-between">
                <span className="text-slate-500">Môn học:</span>
                <span className="font-bold text-slate-800 dark:text-slate-200">
                  {selectedSubject?.name} ({selectedSubject?.code})
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Tổng số câu:</span>
                <span className="font-bold text-slate-800 dark:text-slate-200">
                  {successResponse.session.questionCount} câu
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Trạng thái:</span>
                <span className="font-bold text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-2 py-0.5 rounded">
                  {successResponse.session.status} (Chờ duyệt)
                </span>
              </div>
              {successResponse.game2Predicted && (
                <div className="pt-2 border-t border-slate-200 dark:border-slate-700 flex justify-between text-indigo-600 dark:text-indigo-400 font-semibold">
                  <span>Dự đoán Game 2:</span>
                  <span>
                    LLM: {successResponse.predLlm ?? 0} | Tự soạn: {successResponse.predHuman ?? 0}
                  </span>
                </div>
              )}
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  setSuccessResponse(null);
                  setTitle('');
                  setContent('');
                  setSourceUrl('');
                  setQuestions([createEmptyQuestion(1)]);
                }}
                className="flex-1 py-2.5 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
              >
                Tạo phiên khác
              </button>

              <button
                type="button"
                onClick={() => {
                  setSuccessResponse(null);
                  if (onViewMySubmissions) {
                    onViewMySubmissions();
                  } else if (onSuccessNavigate) {
                    onSuccessNavigate();
                  }
                }}
                className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl text-xs sm:text-sm font-bold shadow-md cursor-pointer"
              >
                Xem danh sách phiên
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Lightbox zoomed image */}
      {zoomedImage && (
        <div
          onClick={() => setZoomedImage(null)}
          className="fixed inset-0 z-60 bg-black/80 flex items-center justify-center p-4 cursor-zoom-out animate-in fade-in"
        >
          <img
            src={zoomedImage}
            alt="Zoomed"
            className="max-w-full max-h-full object-contain rounded-2xl shadow-2xl"
          />
        </div>
      )}
    </div>
  );
};
