import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createCameraDelivery } from './eventDelivery.ts'
const settle = () => new Promise(resolve => setImmediate(resolve))
test('retries reuse event IDs, successful events do not block later delivery', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  const calls = []; let fail = true
  const sender = createCameraDelivery(async event => { calls.push({ ...event }); if (fail) throw new Error('offline') }, () => {})
  sender.push('LOOKING_AWAY'); t.mock.timers.tick(700); await settle()
  fail = false; t.mock.timers.tick(1500); await settle()
  assert.equal(calls.length, 2); assert.equal(calls[0].eventId, calls[1].eventId)
  sender.push('NO_FACE'); t.mock.timers.tick(700); await settle()
  assert.equal(calls[2].kind, 'NO_FACE'); assert.notEqual(calls[2].eventId, calls[0].eventId)
  sender.dispose()
})
test('dispose stops queued delivery; queue remains bounded', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  let calls = 0, pending = 0, failed = false
  const sender = createCameraDelivery(async () => { calls++ }, (count, error) => { pending = count; failed = error })
  for (let i = 0; i < 40; i++) sender.push('LOOKING_AWAY')
  assert.equal(pending, 30); assert.equal(failed, true)
  sender.dispose(); t.mock.timers.tick(10000); await settle()
  assert.equal(calls, 0)
})
