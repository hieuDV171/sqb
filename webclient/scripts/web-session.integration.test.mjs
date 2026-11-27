import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import axios from 'axios';
import { createServer } from 'vite';

test('startup deduplicates rotation, returns the validated profile, and retries transient failures', async () => {
  const previousWindow = Object.getOwnPropertyDescriptor(globalThis, 'window');
  const previousStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage');
  const originalAdapter = axios.defaults.adapter;
  const storage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
  Object.defineProperty(globalThis, 'window', { configurable: true, value: {
    location: { hostname: 'admin.hieudv.id.vn', origin: 'https://admin.hieudv.id.vn', pathname: '/admin/users' },
  } });
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: storage });
  let server;
  try {
    server = await createServer({
      root: fileURLToPath(new URL('../', import.meta.url)),
      server: { middlewareMode: true, hmr: false, watch: null },
      optimizeDeps: { noDiscovery: true, include: [] }, appType: 'custom',
    });
    const { restoreWebSession } = await server.ssrLoadModule('/src/api/webSession.ts');
    const calls = [];
    let releaseRefresh;
    const refreshBarrier = new Promise((resolve) => { releaseRefresh = resolve; });
    const reply = (config, data) => ({ config, status: 200, statusText: 'OK', headers: {}, data: { code: '1000', data } });
    axios.defaults.adapter = async (config) => {
      calls.push(config.url);
      assert.equal(config.withCredentials, true);
      assert.equal(config.headers.get('X-Requested-With'), 'XMLHttpRequest');
      if (config.url.endsWith('/auth/refresh')) {
        await refreshBarrier;
        return reply(config, { access_token: 'fixture-at' });
      }
      assert.equal(config.headers.get('Authorization'), 'Bearer fixture-at');
      return reply(config, { user_id: 7, email: 'fixture@example.test', role: 'ADMIN', full_name: 'Fixture', verified: true, profile_completed: true });
    };
    const first = restoreWebSession();
    const second = restoreWebSession();
    assert.equal(first, second);
    assert.equal(calls.length, 1);
    releaseRefresh();
    const session = await first;
    assert.equal(calls.length, 2);
    assert.equal(session.user.id, 7);
    assert.equal(session.profile.userId, 7);
    assert.equal(session.profile.fullName, 'Fixture');
    assert.equal(session.accessToken, 'fixture-at');
    assert.deepEqual(performance.getEntriesByType('measure').filter(e => e.name.startsWith('sqb:')).map(e => e.name).sort(),
      ['sqb:auth-refresh', 'sqb:session-lock-wait', 'sqb:session-profile', 'sqb:session-restore'].sort());

    const deny = (config, status, data) => { throw new axios.AxiosError('Fixture failure', 'ERR_BAD_RESPONSE', config, undefined,
      { config, status, statusText: 'Failure', headers: {}, data }); };
    axios.defaults.adapter = async (config) => deny(config, 503, { code: '9999', message: 'Unavailable' });
    await assert.rejects(restoreWebSession(), { name: 'AxiosError' });
    axios.defaults.adapter = async (config) => deny(config, 401, { code: '9995', message: 'Expired' });
    assert.equal(await restoreWebSession(), null);
    axios.defaults.adapter = async (config) => deny(config, 401, '<html>Access denied</html>');
    await assert.rejects(restoreWebSession(), { name: 'AccessGatewayError' });
    axios.defaults.adapter = async (config) => config.url.endsWith('/auth/refresh')
      ? reply(config, { access_token: 'fixture-at' }) : reply(config, { user_id: 7, email: 'fixture@example.test', role: 'UNKNOWN' });
    await assert.rejects(restoreWebSession(), /Không thể khôi phục thông tin tài khoản/);
  } finally {
    axios.defaults.adapter = originalAdapter;
    await server?.close();
    if (previousWindow) Object.defineProperty(globalThis, 'window', previousWindow); else delete globalThis.window;
    if (previousStorage) Object.defineProperty(globalThis, 'localStorage', previousStorage); else delete globalThis.localStorage;
    for (const name of ['sqb:auth-refresh', 'sqb:session-lock-wait', 'sqb:session-profile', 'sqb:session-restore']) performance.clearMeasures(name);
  }
});
