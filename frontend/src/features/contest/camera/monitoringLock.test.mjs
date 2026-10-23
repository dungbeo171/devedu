import { test } from 'node:test'
import assert from 'node:assert/strict'
import { emptyPresenceLock, observePresence, presenceLocked, presenceLockSeconds } from './monitoringLock.ts'

test('confirmed missing face locks for at least ten seconds', () => {
  const lock = observePresence(emptyPresenceLock(), 'NO_FACE', 'NO_FACE', 1000)
  assert.equal(presenceLockSeconds(lock, 1000), 10)
  assert.equal(presenceLocked(lock, 10_999), true)
})

test('returning to frame keeps cooldown, then unlocks after ten seconds', () => {
  let lock = observePresence(emptyPresenceLock(), 'NO_FACE', 'NO_FACE', 1000)
  lock = observePresence(lock, 'FORWARD', undefined, 3000)
  assert.equal(lock.noFaceActive, false)
  assert.equal(presenceLocked(lock, 10_999), true)
  assert.equal(presenceLocked(lock, 11_000), false)
})

test('remaining outside the frame keeps the workspace locked after cooldown', () => {
  const lock = observePresence(emptyPresenceLock(), 'NO_FACE', 'NO_FACE', 1000)
  assert.equal(presenceLockSeconds(lock, 20_000), 0)
  assert.equal(presenceLocked(lock, 20_000), true)
})
