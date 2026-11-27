export function ChatPresenceDot({ decorative = false }: { decorative?: boolean }) {
  return (
    <span
      aria-hidden={decorative || undefined}
      title={decorative ? undefined : 'Đang hoạt động'}
      className="absolute right-0 bottom-0 size-3 rounded-full bg-success ring-2 ring-card"
    >
      {!decorative && <span className="sr-only">Đang hoạt động</span>}
    </span>
  );
}
