import assert from 'node:assert/strict';
import test from 'node:test';

const entries = new Map([['sqb-auth-storage', JSON.stringify({ state: { accessToken: 'legacy-at', refreshToken: 'legacy-rt' } })]]);
const writes = [];
Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: {
  getItem: (key) => entries.get(key) ?? null,
  setItem: (key, value) => { entries.set(key, value); writes.push([key, value]); },
  removeItem: (key) => entries.delete(key),
} });
const { useAuthStore, AUTH_EVENT_KEY } = await import('../src/stores/useAuthStore.ts');
const user = { id: 7, username: 'fixture@test', role: 'STUDENT', verified: true, profileCompleted: true };

test('old persisted credentials are removed rather than hydrated', () => {
  assert.equal(entries.has('sqb-auth-storage'), false);
  assert.equal(useAuthStore.getState().accessToken, null);
  assert.equal(useAuthStore.getState().isAuthenticated, false);
});

test('login and token rotation keep credentials exclusively in the store', () => {
  useAuthStore.getState().setAuth('fixture-at', user, 'ACCESS', 'ACTIVE');
  useAuthStore.getState().setTokens('rotated-fixture-at');
  assert.equal(useAuthStore.getState().accessToken, 'rotated-fixture-at');
  assert.equal(useAuthStore.getState().user.id, 7);
  assert.equal(writes.length, 1);
  assert.equal(writes[0][0], AUTH_EVENT_KEY);
  assert.deepEqual(Object.keys(JSON.parse(writes[0][1])).sort(), ['id', 'type']);
  assert.equal(JSON.parse(writes[0][1]).type, 'login');
  assert.equal(JSON.stringify([...entries]).includes('fixture-at'), false);
  assert.equal(JSON.stringify([...entries]).includes('fixture@test'), false);
});

test('cookie restoration does not rebroadcast login and cause a tab loop', () => {
  const before = writes.length;
  useAuthStore.getState().setAuth('restored-at', user, 'ACCESS', 'ACTIVE', false);
  assert.equal(writes.length, before);
});

test('logout clears the in-memory session and broadcasts no credentials', () => {
  useAuthStore.getState().logout();
  assert.equal(useAuthStore.getState().accessToken, null);
  assert.equal(useAuthStore.getState().user, null);
  assert.equal(useAuthStore.getState().isAuthenticated, false);
  assert.equal(JSON.parse(entries.get(AUTH_EVENT_KEY)).type, 'logout');
  const before = writes.length;
  useAuthStore.getState().logout(false);
  assert.equal(writes.length, before);
});
