import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import humps from 'humps';
import { useEffect, useState, useRef } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { API_BASE_URL, isAccessTokenExpiring, refreshAccessToken } from '@/api/accessSession';

export type StompConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

type StatusListener = (status: StompConnectionStatus) => void;
const RECONNECT_DELAY = 5000;

class StompManager {
  private client: Client | null = null;
  private status: StompConnectionStatus = 'DISCONNECTED';
  private listeners: Set<StatusListener> = new Set();
  private socketUrl = `${API_BASE_URL.replace(/\/$/, '')}/ws`;

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
    if (useAuthStore.getState().tokenType !== 'ACCESS' || this.client?.active) return;

    let connectToken: string | undefined;
    let rejectedToken: string | undefined;
    const client = new Client({
      webSocketFactory: () => new SockJS(this.socketUrl),
      beforeConnect: async () => {
        connectToken = await this.prepareConnectToken(client, rejectedToken);
        if (connectToken) client.connectHeaders = { Authorization: `Bearer ${connectToken}` };
      },
      reconnectDelay: RECONNECT_DELAY,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        if (this.client !== client) return;
        rejectedToken = undefined;
        console.log('[StompManager] Connected to STOMP broker at', this.socketUrl);
        this.setStatus('CONNECTED');
      },
      onDisconnect: () => {
        if (this.client !== client) return;
        console.log('[StompManager] Disconnected from STOMP broker');
        this.setStatus('DISCONNECTED');
      },
      onStompError: (frame) => {
        if (this.client !== client) return;
        const errorMsg = frame.headers['message'] || '';
        console.error('[StompManager] Broker error:', errorMsg, frame.body);
        this.setStatus('ERROR');

        if (/unauthorized|jwt|expired/i.test(errorMsg)) {
          // The broker closes the socket after ERROR. Recover once on the next reconnect.
          if (rejectedToken) {
            this.disconnect();
          } else {
            rejectedToken = connectToken;
          }
        }
      },
      onWebSocketClose: () => {
        if (this.client === client) this.setStatus('DISCONNECTED');
      },
    });

    this.client = client;
    client.activate();
  }

  private async prepareConnectToken(client: Client, rejectedToken?: string): Promise<string | undefined> {
    while (this.client === client && client.active) {
      const session = useAuthStore.getState();
      const token = session.accessToken;
      if (session.tokenType !== 'ACCESS' || !token) {
        this.disconnect();
        return;
      }

      this.setStatus('CONNECTING');
      try {
        const validToken = token === rejectedToken || isAccessTokenExpiring(token)
          ? await refreshAccessToken(token)
          : token;
        if (this.client === client && useAuthStore.getState().accessToken === validToken) return validToken;
      } catch {
        if (this.client !== client) return;
        // A temporary refresh failure (for example, offline) can recover on the next attempt.
        this.setStatus('ERROR');
        await new Promise((resolve) => setTimeout(resolve, RECONNECT_DELAY));
      }
    }
  }

  public disconnect(): void {
    const client = this.client;
    this.client = null;
    if (client) {
      void client.deactivate().catch((err) => {
        console.error('[StompManager] Error deactivating client:', err);
      });
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

// Ngắt STOMP khi phiên ACCESS kết thúc, kể cả khi đang chờ reconnect.
useAuthStore.subscribe((state, previousState) => {
  if (previousState.tokenType === 'ACCESS' && state.tokenType !== 'ACCESS') {
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
