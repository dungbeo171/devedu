import { test } from 'node:test'
import assert from 'node:assert/strict'
import { drawFaceMesh } from './faceMeshOverlay.ts'

test('draws normal and emphasized face-mesh connections without retaining a frame', () => {
  const strokes = [], arcs = []
  const context = {
    strokeStyle: '', lineWidth: 0, fillStyle: '', lineCap: '',
    clearRect() {}, beginPath() {}, moveTo() {}, lineTo() {}, fill() {},
    stroke() { strokes.push({ color: this.strokeStyle, width: this.lineWidth }) },
    arc(x, y, radius) { arcs.push({ x, y, radius }) },
  }
  const canvas = { width: 0, height: 0, getContext: () => context }
  drawFaceMesh(canvas, {
    landmarks: Array.from({ length: 6 }, (_, index) => ({ x: index / 10, y: index / 20, z: 0 })),
    connections: [{ start: 0, end: 1, emphasized: false }, { start: 1, end: 2, emphasized: true }],
  }, 640, 480)

  assert.deepEqual([canvas.width, canvas.height], [640, 480])
  assert.equal(strokes.length, 2)
  assert.ok(strokes[1].width > strokes[0].width)
  assert.equal(arcs.length, 2)
})
