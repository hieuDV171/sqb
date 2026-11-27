import type { CursorResponse } from '../../../types/user.types';
import type { UserRole } from '../../../stores/useAuthStore';
import type { ActivityFeedItemDto } from '../types/feed.types';

export type ActivityFeedPage = CursorResponse<ActivityFeedItemDto>;

type FeedScope = {
  userId: number;
  role: UserRole | undefined;
  sessionMarker: string | null;
};

type FeedSnapshot = FeedScope & {
  requestedAt: number;
  page: ActivityFeedPage;
};

const snapshotKey = 'first-page';
const snapshotMaxAge = 30 * 60 * 1000;

function accessSnapshot(
  update: (store: IDBObjectStore, snapshot: FeedSnapshot | undefined) => FeedSnapshot | undefined,
): Promise<FeedSnapshot | undefined> {
  return new Promise((resolve) => {
    if (typeof indexedDB === 'undefined') return resolve(undefined);
    let opening: IDBOpenDBRequest;
    try { opening = indexedDB.open('sqb-activity-feed', 1); }
    catch { return resolve(undefined); }

    opening.onupgradeneeded = () => opening.result.createObjectStore('snapshots');
    opening.onerror = () => resolve(undefined);
    opening.onblocked = () => resolve(undefined);
    opening.onsuccess = () => {
      const db = opening.result;
      let result: FeedSnapshot | undefined;
      const transaction = db.transaction('snapshots', 'readwrite');
      transaction.oncomplete = () => { db.close(); resolve(result); };
      transaction.onabort = () => { db.close(); resolve(undefined); };
      const store = transaction.objectStore('snapshots');
      const reading = store.get(snapshotKey);
      reading.onsuccess = () => {
        result = update(store, reading.result as FeedSnapshot | undefined);
      };
    };
  });
}

export function readFeedSnapshot(scope: FeedScope, isCurrent: () => boolean) {
  return accessSnapshot((store, snapshot) => {
    if (!snapshot || !isCurrent()) return;
    if (snapshot.userId !== scope.userId || snapshot.role !== scope.role || snapshot.sessionMarker !== scope.sessionMarker ||
        Date.now() - snapshot.requestedAt >= snapshotMaxAge) {
      store.delete(snapshotKey);
      return;
    }
    return snapshot;
  });
}

export function saveFeedSnapshot(snapshot: FeedSnapshot, isCurrent: () => boolean) {
  return accessSnapshot((store, current) => {
    if (isCurrent() && (!current || current.sessionMarker !== snapshot.sessionMarker || current.requestedAt <= snapshot.requestedAt)) {
      store.put(snapshot, snapshotKey);
    }
    return undefined;
  });
}

export function clearFeedSnapshot(clearedAt = Date.now()) {
  return accessSnapshot((store, snapshot) => {
    if (snapshot && snapshot.requestedAt <= clearedAt) store.delete(snapshotKey);
    return undefined;
  });
}
