import React, { useState, useRef, useEffect, useMemo } from "react";
import { ChevronDown, Check, Search, X, User } from "lucide-react";

export interface CustomSelectOption {
  value: string;
  label: string;
  subLabel?: string;
  badge?: string;
}

interface CustomSelectProps {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  options: CustomSelectOption[];
  placeholder?: string;
  emptyLabel?: string;
  disabled?: boolean;
  searchable?: boolean;
  className?: string;
}

export function CustomSelect({
  id,
  value,
  onChange,
  options,
  placeholder = "Chọn một mục...",
  emptyLabel = "-- Bỏ trống (Không chỉ định) --",
  disabled = false,
  searchable = true,
  className = "",
}: CustomSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Đóng dropdown khi nhấp chuột ra ngoài
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      // Focus vào ô tìm kiếm khi mở
      setTimeout(() => {
        searchInputRef.current?.focus();
      }, 50);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  // Tìm option đã chọn
  const selectedOption = useMemo(
    () => options.find((opt) => opt.value === value),
    [options, value]
  );

  // Lọc danh sách theo query
  const filteredOptions = useMemo(() => {
    if (!searchQuery.trim()) return options;
    const q = searchQuery.toLowerCase();
    return options.filter(
      (opt) =>
        opt.label.toLowerCase().includes(q) ||
        (opt.subLabel && opt.subLabel.toLowerCase().includes(q)) ||
        (opt.badge && opt.badge.toLowerCase().includes(q))
    );
  }, [options, searchQuery]);

  const handleSelect = (val: string) => {
    onChange(val);
    setIsOpen(false);
    setSearchQuery("");
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    onChange("");
    setIsOpen(false);
    setSearchQuery("");
  };

  return (
    <div ref={containerRef} className={`relative w-full ${className}`}>
      {/* Trigger Button */}
      <button
        type="button"
        id={id}
        disabled={disabled}
        onClick={() => !disabled && setIsOpen(!isOpen)}
        className={`w-full min-h-[42px] px-3.5 py-2 flex items-center justify-between gap-2 text-xs rounded-xl border transition-all text-left cursor-pointer ${
          disabled ? "opacity-50 cursor-not-allowed" : ""
        } ${
          isOpen
            ? "border-indigo-500 ring-2 ring-indigo-500/20 bg-white dark:bg-slate-900"
            : "border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-slate-300 dark:hover:border-slate-700"
        }`}
      >
        <div className="flex-1 min-w-0 flex items-center gap-2">
          {selectedOption ? (
            <div className="flex items-center gap-2 truncate">
              <span className="font-semibold text-slate-900 dark:text-slate-100 truncate">
                {selectedOption.label}
              </span>
              {selectedOption.badge && (
                <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 shrink-0">
                  {selectedOption.badge}
                </span>
              )}
              {selectedOption.subLabel && (
                <span className="text-slate-500 dark:text-slate-400 text-[11px] truncate hidden sm:inline">
                  | {selectedOption.subLabel}
                </span>
              )}
            </div>
          ) : (
            <span className="text-slate-400 dark:text-slate-500 truncate">
              {placeholder}
            </span>
          )}
        </div>

        <div className="flex items-center gap-1.5 shrink-0 text-slate-400">
          {value && !disabled && (
            <span
              role="button"
              onClick={handleClear}
              title="Xóa lựa chọn"
              className="p-1 hover:text-slate-600 dark:hover:text-slate-200 rounded-md transition-colors"
            >
              <X className="w-3.5 h-3.5" />
            </span>
          )}
          <ChevronDown
            className={`w-4 h-4 transition-transform duration-200 ${
              isOpen ? "rotate-180 text-indigo-600 dark:text-indigo-400" : ""
            }`}
          />
        </div>
      </button>

      {/* Dropdown Menu Popover */}
      {isOpen && (
        <div className="absolute left-0 right-0 top-full mt-1.5 z-50 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
          {/* Search Input */}
          {searchable && options.length > 3 && (
            <div className="p-2 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
              <div className="relative flex items-center">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 pointer-events-none" />
                <input
                  ref={searchInputRef}
                  type="text"
                  placeholder="Tìm theo tên, mã cán bộ, khoa viện..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full h-8 pl-8 pr-3 text-xs bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery("")}
                    className="absolute right-2 text-slate-400 hover:text-slate-600 p-0.5 rounded cursor-pointer"
                  >
                    <X className="w-3 h-3" />
                  </button>
                )}
              </div>
            </div>
          )}

          {/* Option List */}
          <div className="max-h-60 overflow-y-auto p-1.5 space-y-0.5">
            {/* Clear / Empty option */}
            <button
              type="button"
              onClick={() => handleSelect("")}
              className={`w-full px-3 py-2 text-left rounded-xl text-xs flex items-center justify-between transition-colors cursor-pointer ${
                !value
                  ? "bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 font-medium"
                  : "text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
              }`}
            >
              <span className="italic">{emptyLabel}</span>
              {!value && <Check className="w-4 h-4 shrink-0" />}
            </button>

            {filteredOptions.length === 0 ? (
              <div className="py-4 text-center text-xs text-slate-400 dark:text-slate-500">
                Không tìm thấy kết quả phù hợp
              </div>
            ) : (
              filteredOptions.map((opt) => {
                const isSelected = opt.value === value;
                return (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => handleSelect(opt.value)}
                    className={`w-full px-3 py-2.5 text-left rounded-xl text-xs flex items-start justify-between gap-2 transition-colors cursor-pointer ${
                      isSelected
                        ? "bg-indigo-50 dark:bg-indigo-950/40 text-indigo-900 dark:text-indigo-200 font-semibold"
                        : "text-slate-800 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800/70"
                    }`}
                  >
                    <div className="flex items-start gap-2.5 min-w-0">
                      <div className="w-6 h-6 rounded-full bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center shrink-0 mt-0.5">
                        <User className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className="font-semibold">{opt.label}</span>
                          {opt.badge && (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                              {opt.badge}
                            </span>
                          )}
                        </div>
                        {opt.subLabel && (
                          <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 truncate">
                            {opt.subLabel}
                          </p>
                        )}
                      </div>
                    </div>

                    {isSelected && (
                      <Check className="w-4 h-4 text-indigo-600 dark:text-indigo-400 shrink-0 mt-1" />
                    )}
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}
