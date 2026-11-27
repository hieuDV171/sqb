import assert from 'node:assert/strict';
import test from 'node:test';
import { createDraftAutosave } from '@/features/session/lib/draftAutosave.ts';
import { acquireBodyScrollLock, hasActiveOverlay, registerOverlayLayer, trapOverlayTab } from '@/lib/overlayLifecycle.ts';

function storage() {
  const values = new Map();
  return { values, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) };
}

test('successful submit/discard invalidates pending autosaves, including replacement timers', t => {
  t.mock.timers.enable({ apis: ['setTimeout', 'Date'] });
  const store = storage();
  const save = createDraftAutosave('draft', () => store);
  save.schedule({ title: 'old' });
  t.mock.timers.tick(400);
  save.schedule({ title: 'latest' });
  save.saveNow({ title: 'latest' });
  assert.equal(JSON.parse(store.values.get('draft')).title, 'latest');
  save.clear();
  t.mock.timers.tick(2000);
  assert.equal(store.values.has('draft'), false);
  save.schedule({ title: 'new form' });
  t.mock.timers.tick(1000);
  assert.equal(JSON.parse(store.values.get('draft')).title, 'new form');
});

test('failed request can retain latest input, while unmount cancels later writes', t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const store = storage();
  const save = createDraftAutosave('draft', () => store);
  save.saveNow({ title: 'latest submitted input' });
  save.schedule({ title: 'pending input' });
  save.cancel();
  t.mock.timers.tick(2000);
  assert.equal(JSON.parse(store.values.get('draft')).title, 'latest submitted input');
});

test('unavailable storage does not prevent form actions', t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const save = createDraftAutosave('draft', () => { throw new Error('Storage blocked'); });
  assert.doesNotThrow(() => { save.saveNow({ title: 'input' }); save.clear(); save.schedule({ title: 'input' }); t.mock.timers.tick(1000); });
});

test('scroll owners can close out of order and release repeatedly without unlocking another owner', () => {
  const body = { style: { overflow: 'auto' } };
  const first = acquireBodyScrollLock(body);
  const second = acquireBodyScrollLock(body);
  first();
  assert.equal(body.style.overflow, 'hidden');
  first();
  assert.equal(body.style.overflow, 'hidden');
  second();
  assert.equal(body.style.overflow, 'auto');
  const next = acquireBodyScrollLock(body);
  second();
  assert.equal(body.style.overflow, 'hidden');
  next();
  assert.equal(body.style.overflow, 'auto');
});

test('only top overlay owns dismissal; removing a background layer preserves top ownership', () => {
  const first = registerOverlayLayer(), second = registerOverlayLayer(), third = registerOverlayLayer();
  try {
    assert.equal(first.isTopmost(), false);
    assert.equal(third.isTopmost(), true);
    second.release();
    assert.equal(third.isTopmost(), true);
    third.release();
    assert.equal(first.isTopmost(), true);
  } finally { first.release(); second.release(); third.release(); }
  assert.equal(hasActiveOverlay(), false);
});

test('Tab remains in active dialog and skips hidden controls', () => {
  const ownerDocument = { activeElement: null };
  const element = visible => ({ getClientRects: () => visible ? [{}] : [], closest: () => null,
    focus() { ownerDocument.activeElement = this; } });
  const first = element(true), hidden = element(false), last = element(true);
  const root = { ownerDocument, querySelectorAll: () => [first, hidden, last], contains: node => [first, last].includes(node) };
  let prevented = 0;
  ownerDocument.activeElement = last;
  trapOverlayTab(root, { key: 'Tab', shiftKey: false, preventDefault() { prevented++; } });
  assert.equal(ownerDocument.activeElement, first);
  trapOverlayTab(root, { key: 'Tab', shiftKey: true, preventDefault() { prevented++; } });
  assert.equal(ownerDocument.activeElement, last);
  ownerDocument.activeElement = {};
  trapOverlayTab(root, { key: 'Tab', shiftKey: false, preventDefault() { prevented++; } });
  assert.equal(ownerDocument.activeElement, first);
  assert.equal(prevented, 3);
});
