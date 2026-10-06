import { test } from 'node:test'
import assert from 'node:assert/strict'
import { subscribeContestUpdates } from './contestUpdates.ts'

test('polling pauses in hidden tabs, refreshes on focus and cleans up', () => {
  const priorWindow = globalThis.window, priorDocument = globalThis.document
  const events = new Map()
  let tick, interval, cleared = false, calls = 0
  globalThis.window = {
    setInterval(fn, ms) { tick = fn; interval = ms; return 1 },
    clearInterval(id) { cleared = id === 1 },
    addEventListener(name, fn) { events.set(name, fn) },
    removeEventListener(name) { events.delete(name) },
  }
  globalThis.document = { hidden: false, addEventListener: window.addEventListener, removeEventListener: window.removeEventListener }
  try {
    const unsubscribe = subscribeContestUpdates(() => calls++, 5000)
    assert.equal(interval, 5000)
    tick(); assert.equal(calls, 1)
    document.hidden = true; tick(); assert.equal(calls, 1)
    document.hidden = false; events.get('focus')(); events.get('visibilitychange')()
    assert.equal(calls, 3)
    unsubscribe(); assert.equal(cleared, true); assert.equal(events.size, 0)
  } finally {
    if (priorWindow === undefined) delete globalThis.window; else globalThis.window = priorWindow
    if (priorDocument === undefined) delete globalThis.document; else globalThis.document = priorDocument
  }
})
