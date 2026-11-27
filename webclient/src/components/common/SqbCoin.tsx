interface SqbCoinProps {
  className?: string;
  size?: number;
}

/**
 * Biểu tượng Đồng xu SQB Coin chuẩn Vector SVG
 * Đảm bảo hiển thị sắc nét, đẹp mắt và đồng nhất trên mọi hệ điều hành (kể cả Windows không có font emoji U+1FA99)
 */
export function SqbCoin({ className = 'w-4 h-4 inline-block', size }: SqbCoinProps) {
  const style = size ? { width: size, height: size } : undefined;

  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`shrink-0 select-none align-middle ${className}`}
      style={style}
    >
      <defs>
        <radialGradient
          id="sqb-coin-gradient"
          cx="0.35"
          cy="0.3"
          r="0.75"
          fx="0.35"
          fy="0.3"
        >
          <stop offset="0%" stopColor="#FDE047" />
          <stop offset="45%" stopColor="#EAB308" />
          <stop offset="90%" stopColor="#CA8A04" />
          <stop offset="100%" stopColor="#A16207" />
        </radialGradient>
        <linearGradient id="sqb-coin-rim" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#FEF08A" />
          <stop offset="100%" stopColor="#854D0E" />
        </linearGradient>
      </defs>

      {/* Vành ngoài */}
      <circle cx="12" cy="12" r="10" fill="url(#sqb-coin-gradient)" stroke="url(#sqb-coin-rim)" strokeWidth="1.2" />

      {/* Vành viền trong nét đứt/chấm nổi */}
      <circle cx="12" cy="12" r="7.5" fill="none" stroke="#FEF9C3" strokeWidth="0.8" strokeDasharray="1.2 1" opacity="0.8" />

      {/* Biểu tượng logo SQB (chữ S cách điệu học thuật) */}
      <path
        d="M14.5 8.5C14.5 7.4 13.4 6.5 12 6.5C10.6 6.5 9.5 7.4 9.5 8.5C9.5 11 14.5 10.2 14.5 13C14.5 14.4 13.4 15.5 12 15.5C10.3 15.5 9.2 14.3 9.2 13M12 5V6.5M12 15.5V17"
        stroke="#713F12"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Vệt sáng lấp lánh (Gloss/Shine) */}
      <ellipse cx="9" cy="8" rx="2.5" ry="1.2" fill="#FFFFFF" opacity="0.35" transform="rotate(-30 9 8)" />
    </svg>
  );
}

export default SqbCoin;
