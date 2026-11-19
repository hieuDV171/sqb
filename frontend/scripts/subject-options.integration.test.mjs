import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { QueryClient, QueryObserver } from '@tanstack/react-query';
import { createServer } from 'vite';

test('subject queries preserve publishing rules, cancellation, account isolation and errors', async t => {
  const previousWindow = Object.getOwnPropertyDescriptor(globalThis, 'window');
  const previousStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage');
  Object.defineProperty(globalThis, 'window', { configurable: true, value: { location: { hostname: 'localhost', origin: 'http://localhost:5173' } } });
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: { getItem: () => null, setItem() {}, removeItem() {} } });
  let server;
  try {
    server = await createServer({ root: fileURLToPath(new URL('../', import.meta.url)),
      server: { middlewareMode: true, hmr: false, watch: null }, optimizeDeps: { noDiscovery: true, include: [] }, appType: 'custom' });
    const { subjectOptionsQuery } = await server.ssrLoadModule('/src/features/session/services/subjectOptions.ts');
    const { sessionService } = await server.ssrLoadModule('/src/features/session/services/sessionService.ts');
    const { courseClassService } = await server.ssrLoadModule('/src/features/course-class/services/courseClassService.ts');
    const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
    const calls = [];
    sessionService.getMyEnrolledSubjects = async signal => { calls.push(['enrolled', signal]); return { data: [] }; };
    sessionService.getAllSubjects = async signal => { calls.push(['catalog', signal]); return { data: [{ subjectId: 99, code: 'ALL', name: 'Catalog' }] }; };
    courseClassService.getMyClasses = async (history, signal) => {
      calls.push(['classes', history, signal]);
      return { data: [{ subjectId: 4, subjectCode: 'C4', subjectName: 'Teaching' }, { subjectId: 4, subjectCode: 'C4', subjectName: 'Teaching' }] };
    };
    await t.test('lecturer publishing falls back to distinct current teaching subjects', async () => {
      const data = await client.fetchQuery(subjectOptionsQuery(1, 'LECTURER', 'publishing'));
      assert.deepEqual(data, [{ subjectId: 4, code: 'C4', name: 'Teaching' }]);
      assert.deepEqual(calls.map(call => call[0]), ['enrolled', 'classes']);
      assert.equal(calls[1][1], false);
      assert.ok(calls[1][2] instanceof AbortSignal);
    });
    await t.test('admin keeps enrolled-first publishing; feed scopes remain separate', async () => {
      calls.length = 0;
      sessionService.getMyEnrolledSubjects = async () => { calls.push(['enrolled']); return { data: [{ subjectId: 5, code: 'E5', name: 'Enrolled' }] }; };
      const publishing = await client.fetchQuery(subjectOptionsQuery(2, 'ADMIN', 'publishing'));
      assert.equal(publishing[0].subjectId, 5);
      assert.deepEqual(calls.map(call => call[0]), ['enrolled']);
      const catalog = await client.fetchQuery(subjectOptionsQuery(2, 'ADMIN', 'catalog'));
      assert.equal(catalog[0].subjectId, 99);
      const enrolled = await client.fetchQuery(subjectOptionsQuery(2, 'ADMIN', 'enrolled'));
      assert.equal(enrolled[0].subjectId, 5);
      sessionService.getMyEnrolledSubjects = async () => ({ data: [] });
      assert.equal((await client.fetchQuery(subjectOptionsQuery(3, 'ADMIN', 'publishing')))[0].subjectId, 99);
    });
    await t.test('equal keys deduplicate, errors remain errors rather than empty success', async () => {
      let count = 0;
      sessionService.getMyEnrolledSubjects = async () => { count++; await Promise.resolve(); return { data: [] }; };
      await Promise.all([client.fetchQuery(subjectOptionsQuery(4, 'STUDENT', 'enrolled')), client.fetchQuery(subjectOptionsQuery(4, 'STUDENT', 'enrolled'))]);
      assert.equal(count, 1);
      sessionService.getMyEnrolledSubjects = async () => { throw new Error('Fixture network failure'); };
      await assert.rejects(client.fetchQuery(subjectOptionsQuery(5, 'STUDENT', 'enrolled')), /Fixture network failure/);
    });
    await t.test('switching accounts cancels old scope and ignores a late result', async () => {
      const requests = [];
      sessionService.getMyEnrolledSubjects = signal => new Promise(resolve => requests.push({ signal, resolve }));
      const observer = new QueryObserver(client, subjectOptionsQuery(6, 'STUDENT', 'enrolled'));
      const unsubscribe = observer.subscribe(() => {});
      try {
        observer.setOptions(subjectOptionsQuery(7, 'STUDENT', 'enrolled'));
        assert.equal(requests[0].signal.aborted, true);
        requests[1].resolve({ data: [{ subjectId: 7, code: 'NEW', name: 'New account' }] });
        await new Promise(resolve => setImmediate(resolve));
        requests[0].resolve({ data: [{ subjectId: 6, code: 'OLD', name: 'Old account' }] });
        await new Promise(resolve => setImmediate(resolve));
        assert.equal(observer.getCurrentResult().data[0].subjectId, 7);
        assert.equal(client.getQueryData(subjectOptionsQuery(6, 'STUDENT', 'enrolled').queryKey), undefined);
      } finally { unsubscribe(); }
    });
    client.clear();
  } finally {
    await server?.close();
    if (previousWindow) Object.defineProperty(globalThis, 'window', previousWindow); else delete globalThis.window;
    if (previousStorage) Object.defineProperty(globalThis, 'localStorage', previousStorage); else delete globalThis.localStorage;
  }
});
