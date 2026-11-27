import { lazy, Suspense, useState } from 'react';
import { Play } from 'lucide-react';

const LecturePlayer = lazy(() => import('./LecturePlayer'));

export function LectureVideo({ src, poster, title }: { src: string; poster?: string; title: string }) {
  const [started, setStarted] = useState(false);

  if (started) {
    return (
      <Suspense fallback={
        <div role="status" className="grid aspect-video place-items-center rounded-xl bg-black text-sm text-white">
          Đang mở trình phát…
        </div>
      }>
        <LecturePlayer src={src} poster={poster} title={title} />
      </Suspense>
    );
  }

  return (
    <button type="button" onClick={() => setStarted(true)} aria-label={`Phát video: ${title}`}
      className="group relative grid aspect-video w-full place-items-center overflow-hidden rounded-xl bg-black text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">
      {poster && <img src={poster} alt="" loading="lazy" className="absolute inset-0 size-full object-contain opacity-70" />}
      <span className="relative flex items-center gap-2 rounded-full bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition-colors group-hover:bg-primary/85">
        <Play aria-hidden="true" className="size-4 fill-current" /> Phát video
      </span>
    </button>
  );
}
