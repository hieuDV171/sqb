import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { createServer } from 'vite';
import { unzipSync, strFromU8 } from 'fflate';

test('password Unicode boundaries and credential workbook roundtrip', async t => {
  const server = await createServer({ root: fileURLToPath(new URL('../', import.meta.url)),
    server: { middlewareMode: true, hmr: false, watch: null }, optimizeDeps: { noDiscovery: true, include: [] }, appType: 'custom' });
  try {
    const policy = await server.ssrLoadModule('/src/lib/passwordPolicy.ts');
    await t.test('Vietnamese letters satisfy case rules, accents never replace punctuation', () => {
      assert.equal(policy.getPasswordError('Đạihọc2026!AB'), null);
      assert.ok(policy.getPasswordError('Đạihọc2026ABC'));
      assert.ok(policy.getPasswordError('Đạihọc2026AB\u0301'));
      assert.equal(policy.getPasswordError('Đạihọc2026!AB'.normalize('NFD')), null);
      assert.equal(policy.passwordsMatch('Đạihọc2026!AB', 'Đạihọc2026!AB'.normalize('NFD')), true);
      assert.ok(policy.getPasswordError('Đạihọc2026!AB', 'Đạihọc2026!AB'.normalize('NFD')));
    });
    await t.test('72 UTF-8 bytes, spaces, hidden characters and short passwords', () => {
      const boundary = 'Aa1!' + 'ệ'.repeat(22) + 'ab';
      assert.equal(new TextEncoder().encode(boundary).length, 72);
      assert.equal(policy.getPasswordError(boundary), null);
      assert.ok(policy.getPasswordError(boundary + 'a'));
      assert.equal(policy.getPasswordError('  Abcdef1234!  '), null);
      assert.ok(policy.getPasswordError('Abcdef1234!\u200b'));
      assert.ok(policy.getPasswordError('Abcdef1234!\n'));
      assert.ok(policy.getPasswordError('Aa1!short'));
    });
    await t.test('XLSX preserves exact Unicode, leading zeroes, spaces and formula-like text', async () => {
      const { createCredentialWorkbook } = await server.ssrLoadModule('/src/features/admin/services/credentialWorkbook.ts');
      const credentials = [{ userId: 1, fullName: 'Nguyễn Văn A & B', studentLecturerCode: '00123',
        email: 'student@example.edu', temporaryPassword: ' =Abc123!_x0041_ ' }];
      const bytes = createCredentialWorkbook(credentials);
      const files = unzipSync(bytes);
      assert.ok(files['[Content_Types].xml']);
      const sheet = strFromU8(files['xl/worksheets/sheet1.xml']);
      const strings = strFromU8(files['xl/sharedStrings.xml']);
      assert.match(strings, /Nguyễn Văn A &amp; B/);
      assert.match(strings, /00123/);
      assert.match(strings, / =Abc123!_x005F_x0041_ /);
      assert.equal((sheet.match(/t="s"/g) ?? []).length, 8);
      assert.ok(!sheet.includes('<f>'));
      // A fixture only: allows independent reading by openpyxl/Excel during QA.
      const { mkdir, writeFile } = await import('node:fs/promises');
      const destination = new URL('../../.tmp/password-qa/', import.meta.url);
      await mkdir(destination, { recursive: true });
      await writeFile(new URL('fixture-accounts.xlsx', destination), bytes);
    });
  } finally { await server.close(); }
});
