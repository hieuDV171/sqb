import React, { useState, useEffect } from 'react';
import { useChatStore } from '../stores/useChatStore';
import { useUnlockHiddenChat, useSetHiddenPin } from '../hooks/useChat';
import { Lock, KeyRound, X, Loader2, Delete } from 'lucide-react';
import { toast } from '@/stores/useToastStore';

interface HiddenChatPinModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialMode?: 'UNLOCK' | 'SET_PIN';
}

export function HiddenChatPinModal({
  isOpen,
  onClose,
  initialMode = 'UNLOCK',
}: HiddenChatPinModalProps) {
  const { unlockPin } = useChatStore();
  const [mode, setMode] = useState<'UNLOCK' | 'SET_PIN'>(initialMode);
  const [pin, setPin] = useState('');
  const [oldPin, setOldPin] = useState('');

  const unlockMutation = useUnlockHiddenChat();
  const setPinMutation = useSetHiddenPin();

  useEffect(() => {
    if (isOpen) {
      setMode(initialMode);
      setPin('');
      setOldPin('');
    }
  }, [isOpen, initialMode]);

  if (!isOpen) return null;

  const handleKeyPress = (num: string) => {
    if (pin.length < 6) {
      const nextPin = pin + num;
      setPin(nextPin);

      // Khi đủ 6 chữ số ở chế độ UNLOCK, tự động submit
      if (mode === 'UNLOCK' && nextPin.length === 6) {
        submitUnlock(nextPin);
      }
    }
  };

  const handleBackspace = () => {
    setPin((prev) => prev.slice(0, -1));
  };

  const submitUnlock = (pinToSubmit: string) => {
    unlockMutation.mutate(
      { pin: pinToSubmit },
      {
        onSuccess: () => {
          unlockPin();
          toast.success('Mở khóa kho trò chuyện ẩn thành công!');
          onClose();
        },
        onError: () => {
          setPin('');
        },
      }
    );
  };

  const submitSetPin = (e: React.FormEvent) => {
    e.preventDefault();
    if (pin.length !== 6) {
      toast.warning('Mã PIN phải có đúng 6 chữ số');
      return;
    }

    setPinMutation.mutate(
      {
        pin,
        oldPin: oldPin.trim() ? oldPin.trim() : undefined,
      },
      {
        onSuccess: () => {
          unlockPin();
          onClose();
        },
      }
    );
  };

  const isLoading = unlockMutation.isPending || setPinMutation.isPending;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in">
      <div
        className="w-full max-w-sm bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl p-6 relative animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Icon & Header */}
        <div className="flex flex-col items-center text-center mb-6">
          <div className="w-14 h-14 rounded-2xl bg-linear-to-tr from-amber-500 to-yellow-500 flex items-center justify-center text-white shadow-lg shadow-amber-500/25 mb-3">
            {mode === 'UNLOCK' ? <Lock className="w-7 h-7" /> : <KeyRound className="w-7 h-7" />}
          </div>

          <h3 className="text-xl font-bold text-slate-900 dark:text-slate-100">
            {mode === 'UNLOCK' ? 'Mở khóa kho bí mật' : 'Thiết lập mã PIN kho ẩn'}
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            {mode === 'UNLOCK'
              ? 'Nhập mã PIN 6 chữ số để xem các cuộc hội thoại ẩn'
              : 'Mã PIN gồm 6 số dùng để bảo vệ quyền riêng tư của bạn'}
          </p>
        </div>

        {/* PIN Indicators (6 Dots) */}
        <div className="flex items-center justify-center gap-3 mb-6">
          {[0, 1, 2, 3, 4, 5].map((index) => {
            const isFilled = index < pin.length;
            return (
              <div
                key={index}
                className={`w-4 h-4 rounded-full transition-all duration-200 ${
                  isFilled
                    ? 'bg-amber-500 scale-110 shadow-xs shadow-amber-500/50'
                    : 'bg-slate-200 dark:bg-slate-800'
                }`}
              />
            );
          })}
        </div>

        {/* Optional old PIN input if SET_PIN */}
        {mode === 'SET_PIN' && (
          <form onSubmit={submitSetPin} className="space-y-4 mb-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                Mã PIN cũ (nếu bạn đã từng cài đặt trước đó)
              </label>
              <input
                type="password"
                maxLength={6}
                value={oldPin}
                onChange={(e) => setOldPin(e.target.value.replace(/\D/g, ''))}
                placeholder="Nhập 6 số PIN cũ"
                className="w-full px-3 py-2 text-sm bg-slate-100 dark:bg-slate-800 rounded-xl outline-none focus:ring-2 focus:ring-amber-500 text-slate-900 dark:text-slate-100 font-mono tracking-widest text-center"
              />
            </div>
            <button
              type="submit"
              disabled={isLoading || pin.length !== 6}
              className="w-full py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-semibold text-sm shadow-md transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isLoading && <Loader2 className="w-4 h-4 animate-spin" />}
              Lưu mã PIN mới
            </button>
          </form>
        )}

        {/* Numeric Keypad (for UNLOCK mode) */}
        {mode === 'UNLOCK' && (
          <>
            <div className="grid grid-cols-3 gap-3 max-w-xs mx-auto mb-4 select-none">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
                <button
                  key={digit}
                  type="button"
                  onClick={() => handleKeyPress(digit)}
                  disabled={isLoading}
                  className="h-12 rounded-2xl bg-slate-100 dark:bg-slate-800/80 hover:bg-slate-200 dark:hover:bg-slate-700/80 text-lg font-bold text-slate-800 dark:text-slate-100 transition-colors cursor-pointer active:scale-95 disabled:opacity-50"
                >
                  {digit}
                </button>
              ))}

              <button
                type="button"
                onClick={() => setMode('SET_PIN')}
                className="h-12 rounded-2xl text-[11px] font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40 transition-colors cursor-pointer flex items-center justify-center"
              >
                Đổi PIN
              </button>

              <button
                type="button"
                onClick={() => handleKeyPress('0')}
                disabled={isLoading}
                className="h-12 rounded-2xl bg-slate-100 dark:bg-slate-800/80 hover:bg-slate-200 dark:hover:bg-slate-700/80 text-lg font-bold text-slate-800 dark:text-slate-100 transition-colors cursor-pointer active:scale-95 disabled:opacity-50"
              >
                0
              </button>

              <button
                type="button"
                onClick={handleBackspace}
                disabled={isLoading}
                className="h-12 rounded-2xl bg-slate-100 dark:bg-slate-800/80 hover:bg-slate-200 dark:hover:bg-slate-700/80 flex items-center justify-center text-slate-600 dark:text-slate-300 transition-colors cursor-pointer active:scale-95 disabled:opacity-50"
              >
                <Delete className="w-5 h-5" />
              </button>
            </div>

            {/* Mode Toggle Footer */}
            <div className="text-center pt-2 border-t border-slate-100 dark:border-slate-800">
              <button
                type="button"
                onClick={() => setMode('SET_PIN')}
                className="text-xs text-slate-500 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer"
              >
                Chưa cài đặt mã PIN? Tạo mã PIN ngay
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
