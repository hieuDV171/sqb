import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import axios from 'axios';
import { createServer } from 'vite';

test('Access edge denials do not refresh or clear SQB sessions; Spring 401 still refreshes', async () => {
  const storage = new Map();
  const browserStorage = {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: (key) => storage.delete(key),
  };
  const oldWindow = Object.getOwnPropertyDescriptor(globalThis, 'window');
  const oldStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage');
  Object.defineProperty(globalThis, 'window', { configurable: true, value: {
    location: { hostname: 'admin.hieudv.id.vn', origin: 'https://admin.hieudv.id.vn', pathname: '/admin/users' },
    localStorage: browserStorage,
  } });
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: browserStorage });
  const originalAdapter = axios.defaults.adapter;
  let server;
  try {
    server = await createServer({
      root: fileURLToPath(new URL('../', import.meta.url)),
      server: { middlewareMode: true, hmr: false, watch: null },
      optimizeDeps: { noDiscovery: true, include: [] },
      appType: 'custom',
    });
    const { axiosClient } = await server.ssrLoadModule('/src/api/axiosClient.ts');
    const { useAuthStore } = await server.ssrLoadModule('/src/stores/useAuthStore.ts');
    const { useToastStore } = await server.ssrLoadModule('/src/stores/useToastStore.ts');
    const jwt = (expiry) => `header.${Buffer.from(JSON.stringify({ userId: 1, deviceId: 'test-device', sessionId: 'test-session', exp: expiry })).toString('base64url')}.signature`;
    const token = jwt(Math.floor(Date.now() / 1000) + 600);
    const setSession = (value) => useAuthStore.getState().setAuth(value, { id: 1, username: 'test', role: 'ADMIN' }, 'ACCESS', 'ACTIVE');
    const deny = (config, status, data) => {
      throw new axios.AxiosError('Denied', 'ERR_BAD_REQUEST', config, undefined,
        { config, data, status, statusText: 'Denied', headers: {} });
    };
    let refreshCount = 0;
    axios.defaults.adapter = async (config) => {
      refreshCount++;
      return { config, data: { code: 200, message: 'OK', data: { access_token: token } }, status: 200, statusText: 'OK', headers: {} };
    };
    for (const status of [401, 403]) {
      setSession(token);
      axiosClient.defaults.adapter = async (config) => {
        assert.equal(config.baseURL, 'https://admin.hieudv.id.vn/api/v1');
        assert.equal(config.headers.get('X-Requested-With'), 'XMLHttpRequest');
        return deny(config, status, '<html>Cloudflare Access</html>');
      };
      await assert.rejects(axiosClient.get('/admin/semesters'), { name: 'AccessGatewayError' });
      assert.equal(useAuthStore.getState().accessToken, token);
      assert.equal(refreshCount, 0);
    }
    // An expired SQB token refreshes before dispatch. Access may deny that refresh itself.
    const expiredToken = jwt(1);
    setSession(expiredToken);
    axios.defaults.adapter = async (config) => {
      assert.equal(config.headers.get('X-Requested-With'), 'XMLHttpRequest');
      return deny(config, 401, 'Access session expired');
    };
    await assert.rejects(axiosClient.get('/admin/semesters'), { name: 'AccessGatewayError' });
    assert.equal(useAuthStore.getState().accessToken, expiredToken);
    assert.equal(useToastStore.getState().toasts.at(-1)?.title, 'Xác thực truy cập quản trị');

    // A genuine backend expiry must still use SQB refresh, retry once, and keep the header.
    setSession(token);
    let apiCalls = 0;
    refreshCount = 0;
    axios.defaults.adapter = async (config) => {
      refreshCount++;
      return { config, data: { code: 200, message: 'OK', data: { access_token: token } }, status: 200, statusText: 'OK', headers: {} };
    };
    axiosClient.defaults.adapter = async (config) => {
      assert.equal(config.headers.get('X-Requested-With'), 'XMLHttpRequest');
      if (apiCalls++ === 0) return deny(config, 401, { code: '9995', message: 'Token expired', data: null });
      return { config, data: { code: 200, message: 'OK', data: [] }, status: 200, statusText: 'OK', headers: {} };
    };
    await axiosClient.get('/admin/semesters');
    assert.equal(apiCalls, 2);
    assert.equal(refreshCount, 1);
  } finally {
    axios.defaults.adapter = originalAdapter;
    await server?.close();
    if (oldWindow) Object.defineProperty(globalThis, 'window', oldWindow);
    else delete globalThis.window;
    if (oldStorage) Object.defineProperty(globalThis, 'localStorage', oldStorage);
    else delete globalThis.localStorage;
  }
});
