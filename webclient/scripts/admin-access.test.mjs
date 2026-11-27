import assert from 'node:assert/strict';
import test from 'node:test';
import { getAdminPageRedirect, isAccessGatewayResponse, resolveApiBaseUrl } from '@/lib/adminAccess.ts';

test('production API and SockJS URLs stay on the current origin even with the old build variable', () => {
  for (const hostname of ['admin.hieudv.id.vn', 'hieudv.id.vn', 'www.hieudv.id.vn', 'snsqb.duckdns.org']) {
    const origin = `https://${hostname}`;
    const api = resolveApiBaseUrl({ hostname, origin }, 'https://hieudv.id.vn/api/v1');
    assert.equal(api, `${origin}/api/v1`);
    assert.equal(`${api}/ws`, `${origin}/api/v1/ws`);
  }
});

test('local development keeps its configured API server and resolves relative URLs', () => {
  const location = { hostname: 'localhost', origin: 'http://localhost:5173' };
  assert.equal(resolveApiBaseUrl(location), 'http://localhost:8080/api/v1');
  assert.equal(resolveApiBaseUrl(location, 'http://localhost:8085/api/v1/'), 'http://localhost:8085/api/v1');
  assert.equal(resolveApiBaseUrl(location, '/api/v1'), 'http://localhost:5173/api/v1');
});

test('old production admin routes navigate through the protected origin without a redirect loop', () => {
  for (const hostname of ['hieudv.id.vn', 'www.hieudv.id.vn', 'snsqb.duckdns.org']) {
    assert.equal(getAdminPageRedirect(hostname, '/admin/reports/123', '?page=2', '#evidence'),
      'https://admin.hieudv.id.vn/admin/reports/123?page=2#evidence');
    assert.equal(getAdminPageRedirect(hostname, '/ADMIN/users'), 'https://admin.hieudv.id.vn/ADMIN/users');
    assert.equal(getAdminPageRedirect(hostname, '/feed'), null);
    assert.equal(getAdminPageRedirect(hostname, '//attacker.invalid/admin/users'), null);
  }
  assert.equal(getAdminPageRedirect('admin.hieudv.id.vn', '/admin/users'), null);
  assert.equal(getAdminPageRedirect('localhost', '/admin/users'), null);
});

test('Access denials are distinct from backend authentication, permissions, and network errors', () => {
  assert.equal(isAccessGatewayResponse(401, ''), true);
  assert.equal(isAccessGatewayResponse(403, '<html>Access denied</html>'), true);
  assert.equal(isAccessGatewayResponse(401, { code: '9995', message: 'Token expired', data: null }), false);
  assert.equal(isAccessGatewayResponse(403, { code: '1013', message: 'Onboarding required' }), false);
  assert.equal(isAccessGatewayResponse(undefined, undefined), false);
  assert.equal(isAccessGatewayResponse(503, ''), false);
});
