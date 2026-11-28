import { Form, ValidatedField } from '@/components/ui/form';
import React, {useState} from 'react';
import {Bot, Check, HelpCircle, ImageIcon, Loader2, Plus, Save, Trash2, User, X,} from 'lucide-react';
import {cn} from '@/lib/utils';
import { getActionErrorMessage } from '@/lib/errorMessage';
import { ActionError } from '@/components/ui/action-error';
import { useSessionFormError } from '../hooks/useSessionFormError';
import {mediaService} from '@/services/mediaService';
import {toast} from '@/stores/useToastStore';
import type {MySubmissionDetailResponse, QuestionUpdateDto, UpdateSubmissionSessionRequest,} from '@/features/session';
import { QuestionDifficultySelect } from './QuestionDifficultySelect';
import { QuestionTopicSelect } from './QuestionTopicSelect';

interface EditSubmissionModalProps {
  isOpen: boolean;
  onClose: () => void;
  detail: MySubmissionDetailResponse | null;
  onSave: (sessionId: number, data: UpdateSubmissionSessionRequest) => Promise<void>;
  isSaving: boolean;
}

export function EditSubmissionModal({ isOpen, detail, ...props }: EditSubmissionModalProps) {
  if (!isOpen || !detail) return null;

  return <EditSubmissionForm key={detail.sessionId} detail={detail} {...props} />;
}

type EditSubmissionFormProps = Omit<EditSubmissionModalProps, 'isOpen' | 'detail'> & {
  detail: MySubmissionDetailResponse;
};

function EditSubmissionForm({
  onClose,
  detail,
  onSave,
  isSaving,
}: EditSubmissionFormProps) {
  const formError = useSessionFormError();
  const [submitError, setSubmitError] = useState<string>();
  const [title, setTitle] = useState(detail.title || '');
  const [content, setContent] = useState(detail.content || '');
  const [questions, setQuestions] = useState<QuestionUpdateDto[]>(() =>
    (detail.questions || []).map((q) => ({
      questionId: q.questionId,
      topicId: q.topicId,
      content: q.content,
      mediaUrls: q.imageUrls || [],
      options: (q.options || []).map((opt) => ({
        key: opt.key,
        text: opt.text,
        isCorrect: Boolean(opt.isCorrect),
        mediaUrl: opt.mediaUrl,
      })),
      explanation: q.explanation || '',
      difficulty: q.difficulty ?? undefined,
      source: q.source || 'HOMO_SAPIENS',
      confidence: q.confidenceScore ?? 3.5,
    })),
  );
  const [uploadingTarget, setUploadingTarget] = useState<string | null>(null);
  const [zoomedImage, setZoomedImage] = useState<string | null>(null);

  const getOptionKey = (idx: number) => String.fromCharCode(65 + idx);

  const handleAddQuestion = () => {
    formError.clear('edit-questions');
    const newQ: QuestionUpdateDto = {
      content: '',
      mediaUrls: [],
      options: [
        { key: 'A', text: '', isCorrect: true },
        { key: 'B', text: '', isCorrect: false },
        { key: 'C', text: '', isCorrect: false },
        { key: 'D', text: '', isCorrect: false },
      ],
      explanation: '',
      source: 'HOMO_SAPIENS',
      confidence: 3.5,
    };
    setQuestions([...questions, newQ]);
  };

  const handleRemoveQuestion = (idx: number) => {
    formError.clear();
    if (questions.length <= 1) {
      formError.show('edit-questions', 'Phiên đề xuất phải chứa ít nhất 1 câu hỏi.');
      return;
    }
    setQuestions(questions.filter((_, i) => i !== idx));
  };

  const handleUpdateQuestion = <Field extends keyof QuestionUpdateDto>(
    idx: number,
    field: Field,
    value: QuestionUpdateDto[Field],
  ) => {
    if (field === 'content') formError.clear(`edit-question-${idx}`);
    if (field === 'confidence') formError.clear(`edit-confidence-${idx}`);
    const updated = [...questions];
    updated[idx] = { ...updated[idx], [field]: value };
    setQuestions(updated);
  };

  const handleAddOption = (qIdx: number) => {
    formError.clear(`edit-options-${qIdx}`);
    const updated = [...questions];
    const currentOpts = updated[qIdx].options;
    const nextKey = getOptionKey(currentOpts.length);
    updated[qIdx].options = [
      ...currentOpts,
      { key: nextKey, text: '', isCorrect: false },
    ];
    setQuestions(updated);
    toast.info(`Đã thêm đáp án ${nextKey} vào câu hỏi #${qIdx + 1}`);
  };

  const handleRemoveOption = (qIdx: number, optIdx: number) => {
    const updated = [...questions];
    const currentOpts = updated[qIdx].options;
    if (currentOpts.length <= 2) {
      formError.show(`edit-options-${qIdx}`, 'Phải giữ ít nhất 2 đáp án lựa chọn.');
      return;
    }
    formError.clear();
    const removedKey = currentOpts[optIdx].key;
    const filtered = currentOpts.filter((_, i) => i !== optIdx);
    updated[qIdx].options = filtered.map((opt, i) => ({
      ...opt,
      key: getOptionKey(i),
    }));
    setQuestions(updated);
    toast.info(`Đã xóa đáp án ${removedKey} khỏi câu hỏi #${qIdx + 1}`);
  };

  const handleUploadOptionImage = async (qIdx: number, optKey: string, file: File) => {
    const targetKey = `opt-${qIdx}-${optKey}`;
    formError.clear(targetKey);
    if (!file.type.startsWith('image/')) {
      formError.show(targetKey, 'Chỉ hỗ trợ tệp hình ảnh.');
      return;
    }
    setUploadingTarget(targetKey);
    try {
      const res = await mediaService.uploadViaPresign(file, 'QUESTION');
      if (res.publicUrl) {
        const updated = [...questions];
        const opts = [...updated[qIdx].options];
        const optIdx = opts.findIndex((o) => o.key === optKey);
        if (optIdx !== -1) {
          opts[optIdx] = { ...opts[optIdx], mediaUrl: res.publicUrl };
          updated[qIdx].options = opts;
          setQuestions(updated);
          toast.success(`Đã gắn ảnh vào đáp án ${optKey}`);
        }
      }
    } catch (err) {
      formError.show(targetKey, getActionErrorMessage(err, 'Không thể tải ảnh đáp án lên.'));
    } finally {
      setUploadingTarget(null);
    }
  };

  const handleRemoveOptionImage = (qIdx: number, optKey: string) => {
    const updated = [...questions];
    updated[qIdx].options = updated[qIdx].options.map((opt) =>
        opt.key === optKey ? {...opt, mediaUrl: undefined} : opt
    );
    setQuestions(updated);
    toast.info(`Đã gỡ ảnh khỏi đáp án ${optKey}`);
  };

  const handleToggleCorrectOption = (qIdx: number, optKey: string) => {
    formError.clear(`edit-options-${qIdx}`);
    const updated = [...questions];
    const q = { ...updated[qIdx] };
    q.options = q.options.map((opt) =>
      opt.key === optKey ? { ...opt, isCorrect: !opt.isCorrect } : opt
    );
    updated[qIdx] = q;
    setQuestions(updated);
  };

  const handleUpdateOptionText = (
    qIdx: number,
    optKey: string,
    text: string
  ) => {
    formError.clear(`edit-option-${qIdx}-${optKey}`);
    const updated = [...questions];
    const q = { ...updated[qIdx] };
    q.options = q.options.map((opt) =>
      opt.key === optKey ? { ...opt, text } : opt
    );
    updated[qIdx] = q;
    setQuestions(updated);
  };

  const handleSubmit = async (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    formError.clear();
    setSubmitError(undefined);

    if (!title.trim()) {
      formError.show('edit-title', 'Vui lòng nhập tiêu đề phiên đề xuất.', true);
      return;
    }

    if (questions.length < 3) {
      formError.show('edit-questions', 'Mỗi phiên đề xuất cần ít nhất 3 câu hỏi.', true);
      return;
    }

    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.content.trim()) {
        formError.show(`edit-question-${i}`, 'Nhập nội dung đề bài.', true);
        return;
      }
      if (q.options.length < 2) {
        formError.show(`edit-options-${i}`, 'Câu hỏi phải có ít nhất 2 đáp án lựa chọn.', true);
        return;
      }
      const emptyOption = q.options.find((o) => !o.text.trim());
      if (emptyOption) {
        formError.show(`edit-option-${i}-${emptyOption.key}`, `Nhập nội dung đáp án ${emptyOption.key}.`, true);
        return;
      }
      const hasCorrect = q.options.some((o) => o.isCorrect);
      if (!hasCorrect) {
        formError.show(`edit-options-${i}`, 'Chọn ít nhất 1 đáp án đúng.', true);
        return;
      }
      if (q.confidence == null || !Number.isFinite(q.confidence)
        || q.confidence < 0 || q.confidence > 4) {
        formError.show(`edit-confidence-${i}`, 'Độ tự tin phải nằm trong khoảng 0.00 đến 4.00.', true);
        return;
      }
    }

    try {
      await onSave(detail.sessionId, {
        title: title.trim(),
        content: content.trim() || undefined,
        questions: questions.map((question) => ({ ...question, clearTopic: question.topicId === null })),
      });
      onClose();
    } catch (error) {
      setSubmitError(getActionErrorMessage(error, 'Không thể lưu thay đổi. Hãy thử lại.'));
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl max-w-4xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">
              Chỉnh sửa phiên đề xuất ({detail.sessionCode})
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Chỉ có thể chỉnh sửa khi phiên đang ở trạng thái Chờ duyệt (PENDING)
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <Form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-6 scrollbar-thin">
          {/* Title & Description */}
          <div className="space-y-4 bg-slate-50/70 dark:bg-slate-800/40 p-4 sm:p-5 rounded-2xl border border-slate-100 dark:border-slate-800">
            <div>
              <label htmlFor="edit-title" className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                Tiêu đề phiên đề xuất *
              </label>
              <input
                type="text"
                id="edit-title"
                {...formError.inputProps('edit-title')}
                value={title}
                onChange={(e) => {
                  formError.clear('edit-title');
                  setTitle(e.target.value);
                }}
                placeholder="Nhập tiêu đề phiên..."
                className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
              <ActionError id="edit-title-error" message={formError.messageAt('edit-title')} />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                Mô tả / Ghi chú cho Giảng viên
              </label>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={2}
                placeholder="Ghi chú thêm về nội dung đề xuất..."
                className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          {/* Question List Editor */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <HelpCircle className="w-4 h-4 text-indigo-600" />
                <span>Danh sách câu hỏi ({questions.length}, tối thiểu 3)</span>
              </h4>
              <button
                type="button"
                onClick={handleAddQuestion}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 text-xs font-semibold transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Thêm câu hỏi</span>
              </button>
            </div>

            <div id="edit-questions" tabIndex={-1} className="space-y-5">
              <ActionError message={formError.messageAt('edit-questions')} />
              {questions.map((q, qIdx) => (
                <div
                  key={qIdx}
                  className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs space-y-4"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold px-2.5 py-0.5 rounded-lg bg-indigo-100/70 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300">
                      Câu hỏi #{qIdx + 1}
                    </span>

                    <div className="flex items-center gap-2">
                      {/* AI vs Human source toggle */}
                      <button
                        type="button"
                        onClick={() =>
                          handleUpdateQuestion(
                            qIdx,
                            'source',
                            q.source === 'LLM' ? 'HOMO_SAPIENS' : 'LLM'
                          )
                        }
                        className={cn(
                          'flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium border cursor-pointer transition-colors',
                          q.source === 'LLM'
                            ? 'bg-purple-50 dark:bg-purple-950/40 text-purple-700 dark:text-purple-300 border-purple-200'
                            : 'bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700'
                        )}
                      >
                        {q.source === 'LLM' ? (
                          <>
                            <Bot className="w-3.5 h-3.5" />
                            <span>AI tạo</span>
                          </>
                        ) : (
                          <>
                            <User className="w-3.5 h-3.5" />
                            <span>Tự soạn</span>
                          </>
                        )}
                      </button>

                      {questions.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveQuestion(qIdx)}
                          className="p-1.5 rounded-lg text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors cursor-pointer"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Content input */}
                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      Nội dung câu hỏi *
                    </label>
                    <textarea
                      id={`edit-question-${qIdx}`}
                      {...formError.inputProps(`edit-question-${qIdx}`)}
                      value={q.content}
                      onChange={(e) =>
                        handleUpdateQuestion(qIdx, 'content', e.target.value)
                      }
                      rows={3}
                      placeholder="Nhập nội dung đề bài..."
                      className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-xl text-xs sm:text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                    />
                    <ActionError id={`edit-question-${qIdx}-error`} message={formError.messageAt(`edit-question-${qIdx}`)} />
                  </div>

                  <div className="max-w-xs space-y-3">
                    <QuestionTopicSelect subjectId={detail.subjectId} value={q.topicId}
                      currentName={detail.questions.find((question) => question.questionId === q.questionId)?.topicName}
                      disabled={isSaving} onChange={(value) => handleUpdateQuestion(qIdx, 'topicId', value)} />
                    <QuestionDifficultySelect label="Độ khó đề xuất" value={q.difficulty} disabled={isSaving}
                      onChange={(value) => handleUpdateQuestion(qIdx, 'difficulty', value)} />
                  </div>

                  {/* Options */}
                  <div id={`edit-options-${qIdx}`} tabIndex={-1} className="space-y-2.5">
                    <div className="flex items-center justify-between flex-wrap gap-2">
                      <label className="block text-xs font-medium text-slate-700 dark:text-slate-300">
                        Các đáp án lựa chọn (Click nút chữ cái để bật/tắt đáp án ĐÚNG - hỗ trợ nhiều đáp án đúng):
                      </label>
                      <button
                        type="button"
                        onClick={() => handleAddOption(qIdx)}
                        className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 text-xs font-semibold transition-colors cursor-pointer"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        <span>Thêm đáp án ({getOptionKey(q.options.length)})</span>
                      </button>
                    </div>

                    <ActionError message={formError.messageAt(`edit-options-${qIdx}`)} />
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                      {q.options.map((opt, optIdx) => (
                        <div
                          key={opt.key}
                          className={cn(
                            'flex flex-col gap-2 p-2.5 rounded-2xl border transition-all',
                            opt.isCorrect
                              ? 'bg-emerald-50/60 dark:bg-emerald-950/30 border-emerald-400 dark:border-emerald-800'
                              : 'bg-slate-50 dark:bg-slate-800/40 border-slate-200 dark:border-slate-700'
                          )}
                        >
                          <div className="flex items-center gap-2">
                            {/* Toggle correct */}
                            <button
                              type="button"
                              onClick={() => handleToggleCorrectOption(qIdx, opt.key)}
                              title="Nhấn để chọn / bỏ chọn đáp án ĐÚNG"
                              className={cn(
                                'w-6 h-6 rounded-lg font-bold text-xs flex items-center justify-center shrink-0 cursor-pointer transition-colors',
                                opt.isCorrect
                                  ? 'bg-emerald-600 text-white shadow-xs'
                                  : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-300'
                              )}
                            >
                              {opt.isCorrect ? (
                                <Check className="w-3.5 h-3.5" />
                              ) : (
                                opt.key
                              )}
                            </button>

                            {/* Option text */}
                            <input
                              type="text"
                              id={`edit-option-${qIdx}-${opt.key}`}
                              {...formError.inputProps(`edit-option-${qIdx}-${opt.key}`)}
                              value={opt.text}
                              onChange={(e) =>
                                handleUpdateOptionText(
                                  qIdx,
                                  opt.key,
                                  e.target.value
                                )
                              }
                              placeholder={`Đáp án ${opt.key}...`}
                              className="flex-1 bg-transparent border-none text-xs sm:text-sm text-slate-900 dark:text-white focus:outline-none"
                            />

                            {/* Upload option image */}
                            <label
                              title={`Tải ảnh cho đáp án ${opt.key}`}
                              className="cursor-pointer p-1.5 rounded-lg text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-slate-700 shrink-0 transition-colors"
                            >
                              {uploadingTarget === `opt-${qIdx}-${opt.key}` ? (
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
                                  if (file) handleUploadOptionImage(qIdx, opt.key, file);
                                  e.target.value = '';
                                }}
                              />
                            </label>

                            {/* Remove option button (if > 2 options) */}
                            {q.options.length > 2 && (
                              <button
                                type="button"
                                onClick={() => handleRemoveOption(qIdx, optIdx)}
                                title={`Xóa đáp án ${opt.key}`}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-slate-700 shrink-0 transition-colors cursor-pointer"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            )}
                          </div>

                          {/* Option Image Preview thumbnail if present */}
                          <ActionError id={`edit-option-${qIdx}-${opt.key}-error`} message={formError.messageAt(`edit-option-${qIdx}-${opt.key}`)} />
                          <ActionError message={formError.messageAt(`opt-${qIdx}-${opt.key}`)} />
                          {opt.mediaUrl && (
                            <div className="flex items-center gap-2 pl-8 pt-0.5">
                              <div className="relative group/optimg">
                                <img
                                  src={opt.mediaUrl}
                                  alt={`Ảnh đáp án ${opt.key}`}
                                  onClick={() => setZoomedImage(opt.mediaUrl!)}
                                  className="w-14 h-14 object-cover rounded-xl border border-slate-200 dark:border-slate-700 cursor-zoom-in shadow-xs hover:opacity-90 transition-opacity"
                                />
                                <button
                                  type="button"
                                  onClick={() => handleRemoveOptionImage(qIdx, opt.key)}
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

                  <div className="grid grid-cols-1 gap-4 md:grid-cols-12">
                    <div className="md:col-span-4">
                      <div className="mb-1 flex items-center gap-1.5">
                        <label htmlFor={`edit-confidence-${qIdx}`} className="text-xs font-bold text-slate-700 dark:text-slate-300">
                          Độ tự tin *
                        </label>
                        <span className="group relative inline-flex">
                          <button
                            type="button"
                            aria-label="Giải thích về điểm độ tự tin"
                            className="inline-flex h-6 w-6 items-center justify-center rounded-full text-indigo-600 hover:bg-indigo-50 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 dark:text-indigo-400 dark:hover:bg-indigo-950/60"
                          >
                            <HelpCircle className="h-4 w-4" />
                          </button>
                          <span role="tooltip" className="pointer-events-none absolute bottom-full left-1/2 z-30 mb-2 hidden w-64 -translate-x-1/2 rounded-xl border border-slate-200 bg-white p-3 text-[11px] font-normal leading-relaxed text-slate-600 shadow-xl group-hover:block group-focus-within:block dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
                            Điểm tự chấm 0.00–4.00 sẽ được so với điểm trung bình của người làm câu hỏi: 0 sai hoàn toàn, 1 LLM bịa, 2 chưa nắm chắc, 3 cơ bản, 4 câu hỏi hay.
                          </span>
                        </span>
                      </div>
                      <ValidatedField>
                        <input
                          id={`edit-confidence-${qIdx}`}
                          {...formError.inputProps(`edit-confidence-${qIdx}`)}
                          type="number"
                          min="0"
                          max="4"
                          step="0.01"
                          required
                          value={q.confidence ?? ''}
                          onChange={(e) => handleUpdateQuestion(qIdx, 'confidence', e.target.value === '' ? undefined : Number(e.target.value))}
                          onBlur={(e) => {
                            if (e.target.value === '') return;
                            const normalized = Math.min(4, Math.max(0, Number(e.target.value)));
                            handleUpdateQuestion(qIdx, 'confidence', Math.round(normalized * 100) / 100);
                          }}
                          className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800/60 dark:text-white"
                        />
                      </ValidatedField>
                      <ActionError id={`edit-confidence-${qIdx}-error`} message={formError.messageAt(`edit-confidence-${qIdx}`)} />
                    </div>

                    {/* Explanation */}
                    <div className="md:col-span-8">
                      <label htmlFor={`edit-explanation-${qIdx}`} className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                        Giải thích đáp án chi tiết
                      </label>
                      <input
                        id={`edit-explanation-${qIdx}`}
                        type="text"
                        value={q.explanation || ''}
                        onChange={(e) =>
                          handleUpdateQuestion(qIdx, 'explanation', e.target.value)
                        }
                        placeholder="Giải thích vì sao đáp án này đúng..."
                        className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <ActionError message={submitError} />
          {/* Action buttons */}
          <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs sm:text-sm font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="flex items-center gap-2 px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold shadow-xs transition-colors cursor-pointer disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              <span>{isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}</span>
            </button>
          </div>
        </Form>
      </div>

      {/* Lightbox zoomed image */}
      {zoomedImage && (
        <div
          onClick={() => setZoomedImage(null)}
          className="fixed inset-0 z-70 bg-black/80 flex items-center justify-center p-4 cursor-zoom-out animate-in fade-in"
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
}
