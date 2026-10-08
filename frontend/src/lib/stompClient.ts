import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import humps from 'humps';
import { useEffect, useState, useRef } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';

export type StompConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

type StatusListener = (status: StompConnectionStatus) => void;

class StompManager {
  private client: Client | null = null;
  private status: StompConnectionStatus = 'DISCONNECTED';
  private listeners: Set<StatusListener> = new Set();
  private socketUrl: string;

  constructor() {
    const rawBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
    // Backend Spring WebSocket endpoint registered at /ws under context-path /api/v1
    this.socketUrl = `${rawBase.replace(/\/$/, '')}/ws`;
  }

  public getStatus(): StompConnectionStatus {
    return this.status;
  }

  private setStatus(newStatus: StompConnectionStatus) {
    if (this.status !== newStatus) {
      this.status = newStatus;
      this.listeners.forEach((listener) => {
        try {
          listener(newStatus);
        } catch (err) {
          console.error('[StompManager] Error in status listener:', err);
        }
      });
    }
  }

  public subscribeStatus(listener: StatusListener): () => void {
    this.listeners.add(listener);
    listener(this.status);
    return () => {
      this.listeners.delete(listener);
    };
  }

  public connect(): void {
    const token = useAuthStore.getState().accessToken;

    if (!token) {
      console.warn('[StompManager] Cannot connect: No access token available');
      this.setStatus('DISCONNECTED');
      return;
    }

    if (this.client && this.status === 'CONNECTED') {
      return;
    }

    // Nếu client cũ đang chạy nhưng chưa connected (có thể đang kẹt reconnect hoặc lỗi), dọn dẹp trước
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (e) {
        console.error('[StompManager] Error deactivating stale client:', e);
      }
      this.client = null;
    }

    this.setStatus('CONNECTING');

    this.client = new Client({
      webSocketFactory: () => new SockJS(this.socketUrl),
      beforeConnect: () => {
        const latestToken = useAuthStore.getState().accessToken;
        if (this.client && latestToken) {
          this.client.connectHeaders = {
            Authorization: `Bearer ${latestToken}`,
          };
        }
      },
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (msg: string) => {
        console.log('[STOMP Debug]:', msg);
      },
      onConnect: () => {
        console.log('[StompManager] Connected to STOMP broker at', this.socketUrl);
        this.setStatus('CONNECTED');
      },
      onDisconnect: () => {
        console.log('[StompManager] Disconnected from STOMP broker');
        this.setStatus('DISCONNECTED');
      },
      onStompError: (frame) => {
        const errorMsg = frame.headers['message'] || '';
        console.error('[StompManager] Broker error:', errorMsg, frame.body);
        this.setStatus('ERROR');

        // Nếu là lỗi xác thực (Token hết hạn / không hợp lệ), ngắt kết nối ngay để tránh reconnect loop vô tận
        if (
          errorMsg.toLowerCase().includes('unauthorized') ||
          errorMsg.toLowerCase().includes('jwt') ||
          errorMsg.toLowerCase().includes('expired')
        ) {
          console.warn('[StompManager] Token authentication failed. Stopping reconnect loop.');
          this.disconnect();
        }
      },
      onWebSocketClose: () => {
        if (this.status !== 'DISCONNECTED') {
          this.setStatus('DISCONNECTED');
        }
      },
    });

    this.client.activate();
  }

  public disconnect(): void {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (err) {
        console.error('[StompManager] Error deactivating client:', err);
      }
      this.client = null;
    }
    this.setStatus('DISCONNECTED');
  }

  public subscribe<T = unknown>(
    destination: string,
    callback: (data: T, message: IMessage) => void
  ): StompSubscription | null {
    if (!this.client || !this.client.connected) {
      console.warn(`[StompManager] Cannot subscribe to ${destination}: client not connected`);
      return null;
    }

    return this.client.subscribe(destination, (message: IMessage) => {
      try {
        let parsedData: any = null;
        if (message.body) {
          try {
            const rawJson = JSON.parse(message.body);
            parsedData = humps.camelizeKeys(rawJson);
          } catch {
            parsedData = message.body;
          }
        }
        callback(parsedData as T, message);
      } catch (err) {
        console.error(`[StompManager] Error handling message on ${destination}:`, err);
      }
    });
  }

  public send(destination: string, body: any = {}, headers: Record<string, string> = {}): boolean {
    if (!this.client || !this.client.connected) {
      console.warn(`[StompManager] Cannot send message to ${destination}: client not connected`);
      return false;
    }

    const payload = typeof body === 'string' ? body : JSON.stringify(humps.decamelizeKeys(body));
    this.client.publish({
      destination,
      body: payload,
      headers,
    });
    return true;
  }
}

export const stompClient = new StompManager();

// Tự động ngắt kết nối WebSocket STOMP khi người dùng đăng xuất (isAuthenticated = false)
useAuthStore.subscribe((state) => {
  if (!state.isAuthenticated && stompClient.getStatus() !== 'DISCONNECTED') {
    stompClient.disconnect();
  }
});

/**
 * React Hook theo dõi trạng thái kết nối STOMP
 */
export function useStompStatus(): StompConnectionStatus {
  const [status, setStatus] = useState<StompConnectionStatus>(stompClient.getStatus());

  useEffect(() => {
    return stompClient.subscribeStatus(setStatus);
  }, []);

  return status;
}

/**
 * React Hook tự động đăng ký và hủy đăng ký nhận tin nhắn STOMP
 * @param destination Đường dẫn đích (/topic/..., /user/queue/...)
 * @param onMessage Callback nhận dữ liệu (tự động camelizeKeys)
 * @param enabled Bật/tắt việc lắng nghe
 */
export function useStompSubscription<T = unknown>(
  destination: string | null | undefined,
  onMessage: (data: T, message: IMessage) => void,
  enabled: boolean = true
): void {
  const status = useStompStatus();
  const callbackRef = useRef(onMessage);
  callbackRef.current = onMessage;

  useEffect(() => {
    if (!enabled || !destination || status !== 'CONNECTED') {
      return;
    }

    const sub = stompClient.subscribe<T>(destination, (data, rawMsg) => {
      callbackRef.current(data, rawMsg);
    });

    return () => {
      if (sub) {
        try {
          sub.unsubscribe();
        } catch (e) {
          console.error('[useStompSubscription] Error unsubscribing:', e);
        }
      }
    };
  }, [destination, enabled, status]);
}
