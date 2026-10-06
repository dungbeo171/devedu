import { test } from 'node:test'
import assert from 'node:assert/strict'
import { contestStatus, countdown, startsIn, endedAgo, durationLabel } from './contestTime.ts'

test('transitions at the exact server start and end boundaries', () => {
  const c = { startsAt: '2026-11-01T10:00:00Z', endsAt: '2026-11-01T12:00:00Z' }
  assert.equal(contestStatus(c, Date.parse(c.startsAt) - 1), 'UPCOMING')
  assert.equal(contestStatus(c, Date.parse(c.startsAt)), 'ONGOING')
  assert.equal(contestStatus(c, Date.parse(c.endsAt) - 1), 'ONGOING')
  assert.equal(contestStatus(c, Date.parse(c.endsAt)), 'FINISHED')
})
test('countdown clamps negative values and supports contests longer than one day', () => {
  assert.equal(countdown(-1000), '00:00:00')
  assert.equal(countdown(4995000), '01:23:15')
  assert.equal(countdown(1), '00:00:01')
  assert.equal(countdown(90000000), '25:00:00')
  assert.equal(startsIn((2 * 24 * 60 + 4 * 60 + 21) * 60000), '2d 04h 21m')
})
test('renders compact duration and past contest text', () => {
  assert.equal(durationLabel(120), '2h')
  assert.equal(durationLabel(90), '1h 30m')
  assert.equal(durationLabel(1), '1m')
  assert.equal(endedAgo(200), 'Ended just now')
  assert.equal(endedAgo(7200000), 'Ended 2 hours ago')
})
