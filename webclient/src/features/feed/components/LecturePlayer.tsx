import { I18nProvider } from '@videojs/react/i18n';
import { NeutralVideoSkin, Video, VideoPlayer } from '@videojs/react/video';
import '@videojs/react/video/neutral-skin.css';
import { useEffect, useRef } from 'react';
import './lecturePlayer.css';

const rates = [0.75, 1, 1.25, 1.5, 1.75, 2];

export default function LecturePlayer({ src, poster, title }: { src: string; poster?: string; title: string }) {
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    container.current?.focus();
  }, []);

  return (
    <div ref={container} tabIndex={-1} aria-label={title} role="group" className="lecture-player">
      <I18nProvider locale="vi">
        <VideoPlayer title={title} poster={poster} defaultPlaybackRates={rates}>
          <NeutralVideoSkin>
            <Video src={src} preload="metadata" playsInline autoPlay aria-label={title} />
          </NeutralVideoSkin>
        </VideoPlayer>
      </I18nProvider>
    </div>
  );
}
