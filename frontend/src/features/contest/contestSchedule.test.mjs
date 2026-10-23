import { test } from 'node:test'
import assert from 'node:assert/strict'
import { contestSchedule } from './contestSchedule.ts'

test('allows past opening, separate closing time and personal working duration', () => {
  assert.deepEqual(contestSchedule('2020-01-01T08:00:00+07:00', '2020-01-01T12:00:00+07:00', 60), {
    startsAt: '2020-01-01T01:00:00.000Z', endsAt: '2020-01-01T05:00:00.000Z', durationMinutes: 60,
  })
})
test('requires a valid window and bounded integer duration', () => {
  for (const [start, end, minutes] of [
    ['', '2026-10-07', 60], ['2026-10-07', '', 60], ['2026-10-07', '2026-10-07', 60],
    ['2026-10-08', '2026-10-07', 60], ['2026-10-07', '2026-10-08', 0],
    ['2026-10-07', '2026-10-08', 1.5], ['2026-10-07', '2026-10-08', 10081],
  ]) assert.throws(() => contestSchedule(start, end, minutes))
})
