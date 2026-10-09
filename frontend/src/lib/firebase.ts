import { initializeApp, getApps, getApp } from 'firebase/app';
import {
  getMessaging,
  register,
  onRegistered,
  unregister,
  onUnregistered,
  onMessage,
  isSupported,
  type Messaging,
} from 'firebase/messaging';
import { deviceService } from '@/services/deviceService';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
};

// Singleton Firebase App instance
export const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);

let messagingInstance: Messaging | null = null;
let isFidLifecycleBound = false;

const FID_STORAGE_KEY = 'sqb_fid';

/**
 * Lấy instance Messaging một cách an toàn (tránh crash trên các trình duyệt không hỗ trợ Push API)
 */
export async function getMessagingSafe(): Promise<Messaging | null> {
  if (typeof window === 'undefined') return null;

  const supported = await isSupported().catch(() => false);
  if (!supported) {
    console.warn('[FID] Trình duyệt hiện tại không hỗ trợ Web Push Notification.');
    return null;
  }

  if (!messagingInstance) {
    messagingInstance = getMessaging(app);
  }
  return messagingInstance;
}

/**
 * Gắn các callback vòng đời FID (onRegistered, onUnregistered) theo chuẩn mới Firebase v11
 */
function bindFidLifecycleListeners(messaging: Messaging) {
  if (isFidLifecycleBound) return;
  isFidLifecycleBound = true;

  // 1. Lắng nghe khi FID được sinh hoặc làm mới
  onRegistered(messaging, async (fid: string) => {
    console.log('[FID] onRegistered callback nhận FID thành công:', fid.substring(0, 15) + '...');
    if (typeof window !== 'undefined') {
      localStorage.setItem(FID_STORAGE_KEY, fid);
    }

    // Tự động đồng bộ FID lên server nếu người dùng đã đăng nhập
    try {
      const { useAuthStore } = await import('@/stores/useAuthStore');
      if (useAuthStore.getState().isAuthenticated) {
        await deviceService.syncFid(fid);
        console.log('[FID] Đã tự động đồng bộ FID mới lên server thành công.');
      }
    } catch (err: any) {
      console.warn('[FID] Không thể tự động đồng bộ FID lên server:', err?.message);
    }
  });

  // 2. Lắng nghe khi app instance bị hủy đăng ký khỏi FCM
  onUnregistered(messaging, async (unregisteredFid: string) => {
    console.warn('[FID] onUnregistered callback kích hoạt cho FID:', unregisteredFid?.substring(0, 15) + '...');
    if (unregisteredFid) {
      await deviceService.unregisterFid(unregisteredFid).catch((err) => {
        console.warn('[FID] Gửi yêu cầu deactive FID lên backend thất bại:', err?.message);
      });
    }
    if (typeof window !== 'undefined') {
      localStorage.removeItem(FID_STORAGE_KEY);
    }
  });
}

/**
 * Xin quyền thông báo trình duyệt và đăng ký FID theo chuẩn mới register() & onRegistered()
 */
export async function requestFid(): Promise<string | null> {
  try {
    if (typeof window === 'undefined' || !('Notification' in window) || !('serviceWorker' in navigator)) {
      console.warn('[FID] Trình duyệt không hỗ trợ Service Worker hoặc Notification.');
      return null;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      console.info('[FID] Người dùng từ chối quyền thông báo:', permission);
      await unregisterCurrentFid();
      return null;
    }

    const messaging = await getMessagingSafe();
    if (!messaging) return null;

    // Gắn listener onRegistered trước khi gọi register()
    bindFidLifecycleListeners(messaging);

    // Đăng ký Service Worker
    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js', {
      scope: '/',
    });

    const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;

    // Chuẩn mới: Dùng register() thay thế hoàn toàn getToken()
    const fidPromise = new Promise<string>((resolve, reject) => {
      const timeout = setTimeout(() => {
        // Fallback đọc từ localStorage nếu listener đã kịp ghi
        const savedFid = localStorage.getItem(FID_STORAGE_KEY);
        if (savedFid) {
          resolve(savedFid);
        } else {
          reject(new Error('Hết thời gian chờ nhận FID từ onRegistered'));
        }
      }, 7000);

      const unsubscribe = onRegistered(messaging, (fid: string) => {
        clearTimeout(timeout);
        unsubscribe();
        resolve(fid);
      });
    });

    await register(messaging, {
      serviceWorkerRegistration: registration,
      ...(vapidKey ? { vapidKey } : {}),
    });

    const fid = await fidPromise;
    console.log('[FID] Hoàn tất đăng ký FID mới:', fid.substring(0, 15) + '...');
    return fid;
  } catch (error) {
    console.error('[FID] Lỗi khi đăng ký FID:', error);
    // Nếu có FID lưu từ trước trong localStorage, trả về để không gián đoạn luồng login
    return typeof window !== 'undefined' ? localStorage.getItem(FID_STORAGE_KEY) : null;
  }
}

/**
 * Hủy đăng ký FID hiện tại bằng chuẩn unregister()
 */
export async function unregisterCurrentFid(): Promise<void> {
  try {
    const messaging = await getMessagingSafe();
    if (messaging) {
      await unregister(messaging);
    } else {
      const currentFid = typeof window !== 'undefined' ? localStorage.getItem(FID_STORAGE_KEY) : null;
      if (currentFid) {
        await deviceService.unregisterFid(currentFid).catch(() => {});
        localStorage.removeItem(FID_STORAGE_KEY);
      }
    }
    console.info('[FID] Đã hủy đăng ký FID thành công.');
  } catch (error) {
    console.warn('[FID] Lỗi khi gọi unregister FID:', error);
  }
}

/**
 * Lắng nghe thông báo đẩy khi người dùng đang mở tab (Foreground Notification)
 */
export async function onForegroundMessage(callback: (payload: unknown) => void) {
  const messaging = await getMessagingSafe();
  if (!messaging) return () => {};

  return onMessage(messaging, (payload) => {
    console.log('[FID] Nhận thông báo Foreground:', payload);
    callback(payload);
  });
}

/**
 * Chủ động đồng bộ FID đã lưu lên server nếu người dùng đã xác thực
 */
export async function syncCurrentFidIfAuthenticated(): Promise<void> {
  if (typeof window === 'undefined') return;
  const fid = localStorage.getItem(FID_STORAGE_KEY);
  if (!fid) return;

  try {
    const { useAuthStore } = await import('@/stores/useAuthStore');
    if (useAuthStore.getState().isAuthenticated) {
      await deviceService.syncFid(fid);
      console.log('[FID] syncCurrentFidIfAuthenticated thành công cho FID:', fid.substring(0, 15) + '...');
    }
  } catch (error: any) {
    console.warn('[FID] syncCurrentFidIfAuthenticated thất bại:', error?.message);
  }
}
