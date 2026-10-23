import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createPoseTracker, poseFromMatrix } from './headPose.ts'

const forward = { yaw: 0, pitch: 0 }, away = { yaw: 40, pitch: 0 }
function fixture() {
  const tracker = createPoseTracker()
  let at = 0
  const tick = (pose = forward, faces = 1, step = 200) => { at += step; return tracker.sample(pose, faces, at) }
  for (let i = 0; i < 21; i++) tick()
  return { tick }
}
test('counts immediately when turning away, once per episode, and frontal return rearms', () => {
  const { tick } = fixture()
  assert.equal(tick(away).event, 'LOOKING_AWAY')
  for (let i = 0; i < 50; i++) assert.equal(tick(away).event, undefined)
  tick()
  assert.equal(tick(away).event, 'LOOKING_AWAY')
})
test('threshold jitter does not create duplicate episodes before a frontal frame', () => {
  const { tick } = fixture()
  assert.equal(tick(away).event, 'LOOKING_AWAY')
  tick({ yaw: 25, pitch: 0 })
  for (let i = 0; i < 15; i++) assert.equal(tick(away).event, undefined)
  tick()
  assert.equal(tick(away).event, 'LOOKING_AWAY')
})
test('missing/multiple faces are never counted as looking away', () => {
  const { tick } = fixture()
  for (let i = 0; i < 10; i++) assert.equal(tick(null, 0).event, undefined)
  assert.equal(tick(null, 0).event, 'NO_FACE')
  for (let i = 0; i < 20; i++) assert.equal(tick(null, 0).event, undefined)
  assert.deepEqual(tick(null, 2), { state: 'UNKNOWN' })
})
test('background gaps do not count as observed time', () => {
  const { tick } = fixture()
  assert.equal(tick(away).event, 'LOOKING_AWAY')
  assert.equal(tick(away, 1, 10000).event, undefined)
  tick()
  assert.equal(tick(away).event, 'LOOKING_AWAY')
})
test('slow foreground inference still counts an observed turn immediately', () => {
  const { tick } = fixture()
  assert.equal(tick(away, 1, 1200).event, 'LOOKING_AWAY')
})
test('starting an attempt clears a pre-attempt latch without losing calibration', () => {
  const tracker = createPoseTracker()
  let at = 0
  for (let i = 0; i < 21; i++) tracker.sample(forward, 1, at += 200)
  tracker.sample(away, 1, at += 200)
  tracker.resetObservationWindow()
  assert.equal(tracker.sample(away, 1, at += 200).event, 'LOOKING_AWAY')
})
test('missing face does not rearm an already counted away episode', () => {
  const { tick } = fixture()
  tick(away)
  for (let i = 0; i < 20; i++) tick(null, 0)
  for (let i = 0; i < 20; i++) assert.equal(tick(away).event, undefined)
})
test('calibration requires 20 stable frontal samples and does not count turns', () => {
  const tracker = createPoseTracker()
  for (let i = 1; i <= 40; i++) assert.deepEqual(tracker.sample(away, 1, i * 200), { state: 'CALIBRATING' })
  for (let i = 41; i <= 60; i++) assert.equal(tracker.sample({ yaw: 10, pitch: 5 }, 1, i * 200).event, undefined)
  assert.equal(tracker.sample({ yaw: 10, pitch: 5 }, 1, 12200).state, 'FORWARD')
})
test('matrix direction handles neutral/left/right and invalid data', () => {
  const identity = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]
  assert.equal(poseFromMatrix(identity).yaw, 0)
  assert.equal(Math.abs(poseFromMatrix(identity).pitch), 0)
  for (const degrees of [-40, 40]) {
    const matrix = [...identity]; matrix[8] = Math.sin(degrees * Math.PI / 180); matrix[10] = Math.cos(degrees * Math.PI / 180)
    assert.ok(Math.abs(poseFromMatrix(matrix).yaw - degrees) < 0.001)
  }
  assert.equal(poseFromMatrix([]), null)
  assert.equal(poseFromMatrix(Array(16).fill(NaN)), null)
})
test('matrix direction treats a reversed neutral Z axis as frontal', () => {
  const reversedNeutral = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1]
  assert.equal(poseFromMatrix(reversedNeutral).yaw, 0)
  for (const degrees of [-40, 40]) {
    const matrix = [...reversedNeutral]
    matrix[8] = Math.sin(degrees * Math.PI / 180)
    matrix[10] = -Math.cos(degrees * Math.PI / 180)
    assert.ok(Math.abs(poseFromMatrix(matrix).yaw - degrees) < 0.001)
  }
})
