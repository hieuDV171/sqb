import axios from 'axios';
import { ACCESS_REQUEST_HEADERS, API_BASE_URL } from './accessSession';

const probeTimeoutMs = 5_000;
const probeCooldownMs = 5_000;
const recoveryIntervalMs = 15_000;
const slowRequestMs = 8_000;

type ConnectionSnapshot = {
  status: 'connected' | 'offline' | 'unavailable';
  checking: boolean;
};

const listeners = new Set<() => void>();
const slowRequestTimers = new WeakMap<object, ReturnType<typeof setTimeout>>();
let apiUnavailable = false;
let probe: AbortController | undefined;
let probeTimer: ReturnType<typeof setTimeout> | undefined;
let lastProbeFinishedAt = -Infinity;
let snapshot: ConnectionSnapshot = { status: isBrowserOnline() ? 'connected' : 'offline', checking: false };

function isBrowserOnline() {
  return typeof navigator === 'undefined' || navigator.onLine !== false;
}

function publishStatus() {
  const status = !isBrowserOnline() ? 'offline' : apiUnavailable ? 'unavailable' : 'connected';
  const checking = Boolean(probe);
  if (snapshot.status === status && snapshot.checking === checking) return;

  snapshot = { status, checking };
  for (const listener of listeners) listener();
}

function clearProbeTimer() {
  if (probeTimer !== undefined) clearTimeout(probeTimer);
  probeTimer = undefined;
}

function scheduleProbe(delay: number) {
  if (probeTimer !== undefined) return;
  probeTimer = setTimeout(() => {
    probeTimer = undefined;
    void checkApiConnection();
  }, delay);
}

export async function checkApiConnection() {
  if (!listeners.size || !isBrowserOnline() || probe) return;
  clearProbeTimer();

  const current = new AbortController();
  probe = current;
  publishStatus();
  const timeout = setTimeout(() => current.abort(), probeTimeoutMs);
  try {
    const response = await fetch(`${API_BASE_URL}/actuator/health/readiness`, {
      credentials: 'include',
      cache: 'no-store',
      headers: { Accept: 'application/json', ...ACCESS_REQUEST_HEADERS },
      signal: current.signal,
    });
    let unavailable = response.status >= 500;
    if (response.ok && response.headers.get('content-type')?.includes('json')) {
      const body: { status?: unknown } | null = await response.json();
      unavailable = typeof body?.status === 'string' && body.status !== 'UP';
    }
    if (probe === current) apiUnavailable = unavailable;
  } catch {
    if (probe === current) apiUnavailable = true;
  } finally {
    clearTimeout(timeout);
    if (probe === current) {
      probe = undefined;
      lastProbeFinishedAt = performance.now();
      publishStatus();
      if (apiUnavailable) scheduleProbe(recoveryIntervalMs);
    }
  }
}

function cancelProbe() {
  clearProbeTimer();
  const current = probe;
  probe = undefined;
  current?.abort();
}

function onOffline() {
  cancelProbe();
  publishStatus();
}

function onOnline() {
  publishStatus();
  void checkApiConnection();
}

export function getConnectionSnapshot() {
  return snapshot;
}

export function subscribeToConnection(listener: () => void) {
  listeners.add(listener);
  if (listeners.size === 1) {
    window.addEventListener('offline', onOffline);
    window.addEventListener('online', onOnline);
    publishStatus();
    if (apiUnavailable && isBrowserOnline()) scheduleProbe(0);
  }

  return () => {
    listeners.delete(listener);
    if (listeners.size) return;
    window.removeEventListener('offline', onOffline);
    window.removeEventListener('online', onOnline);
    cancelProbe();
    publishStatus();
  };
}

function suspectApiOutage() {
  if (!listeners.size || !isBrowserOnline() || probe) return;
  const delay = probeCooldownMs - (performance.now() - lastProbeFinishedAt);
  if (delay > 0) scheduleProbe(delay);
  else void checkApiConnection();
}

export function reportApiConnectionFailure(error: unknown) {
  if (!axios.isAxiosError(error) || axios.isCancel(error)) return;
  const status = error.response?.status;
  if (status !== undefined ? status >= 500 : ['ERR_NETWORK', 'ETIMEDOUT', 'ECONNABORTED'].includes(error.code ?? '')) {
    suspectApiOutage();
  }
}

export function trackApiRequest(config: object) {
  if (listeners.size && isBrowserOnline()) slowRequestTimers.set(config, setTimeout(suspectApiOutage, slowRequestMs));
}

export function finishApiRequest(config?: object, succeeded = false) {
  if (!config) return;
  clearTimeout(slowRequestTimers.get(config));
  slowRequestTimers.delete(config);
  if (succeeded && apiUnavailable) suspectApiOutage();
}
