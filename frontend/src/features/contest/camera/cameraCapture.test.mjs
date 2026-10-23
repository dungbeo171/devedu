import { test } from 'node:test'
import assert from 'node:assert/strict'
import { registerHooks } from 'node:module'

// Resolve the existing Vite-style extensionless import without adding a test dependency.
const hooks = registerHooks({ resolve(specifier, context, next) {
  return next(specifier === './headPose' && context.parentURL?.endsWith('/cameraCapture.ts') ? './headPose.ts' : specifier, context)
} })
const { openContestCamera } = await import('./cameraCapture.ts')
hooks.deregister()

function environment(t, acquire) {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  let stopped = 0, terminated = 0, requests = 0
  const workers = [], events = [], states = [], errors = []
  const track = { muted: false, stop() { stopped++ }, addEventListener() {} }
  const stream = { getTracks: () => [track], getVideoTracks: () => [track] }
  class Worker {
    constructor() { workers.push(this) }
    postMessage(message) {
      if (message.type === 'INIT') queueMicrotask(() => this.onmessage({ data: { type: 'READY' } }))
    }
    terminate() { terminated++ }
  }
  const globals = {
    window: { isSecureContext: true, Worker, createImageBitmap: () => {}, location: { origin: 'http://localhost' } },
    navigator: { mediaDevices: { getUserMedia: async () => { requests++; return acquire ? acquire(stream) : stream } } },
    document: { hidden: false }, Worker,
    createImageBitmap: async () => ({ close() {} }),
  }
  for (const [key, value] of Object.entries(globals)) {
    const original = Object.getOwnPropertyDescriptor(globalThis, key)
    Object.defineProperty(globalThis, key, { value, configurable: true, writable: true })
    t.after(() => { if (original) Object.defineProperty(globalThis, key, original); else delete globalThis[key] })
  }
  const video = { srcObject: null, readyState: 2, currentTime: 1, play: async () => {}, className: 'sr-only' }
  const controller = new AbortController()
  const open = () => openContestCamera(video, controller.signal, state => states.push(state), event => events.push(event), error => errors.push(error))
  t.after(() => controller.abort())
  return { video, controller, open, workers, events, states, errors, counts: () => ({ stopped, terminated, requests }) }
}

test('hiding the preview or backgrounding keeps the stream; abort stops camera and worker', async t => {
  const env = environment(t)
  await env.open()
  const stream = env.video.srcObject
  env.video.className = 'preview-visible'
  env.video.className = 'sr-only'
  document.hidden = true
  t.mock.timers.tick(200)
  assert.equal(env.video.srcObject, stream)
  assert.deepEqual(env.counts(), { requests: 1, stopped: 0, terminated: 0 })
  assert.deepEqual(env.events, ['MONITORING_STARTED'])
  assert.equal(env.states.at(-1), 'UNKNOWN')
  env.controller.abort()
  assert.equal(env.video.srcObject, null)
  assert.deepEqual(env.counts(), { requests: 1, stopped: 1, terminated: 1 })
})

test('leaving while permission is pending stops a late stream without starting a worker', async t => {
  let resolve
  const env = environment(t, stream => new Promise(done => { resolve = () => done(stream) }))
  const pending = env.open()
  env.controller.abort()
  resolve()
  await pending
  assert.equal(env.video.srcObject, null)
  assert.deepEqual(env.counts(), { requests: 1, stopped: 1, terminated: 0 })
  assert.deepEqual(env.events, [])
})

test('worker failure reports interruption once and releases the camera', async t => {
  const env = environment(t)
  await env.open()
  env.workers[0].onerror()
  env.workers[0].onerror()
  assert.equal(env.errors.length, 1)
  assert.match(env.errors[0], /Kết nối lại camera/)
  assert.equal(env.video.srcObject, null)
  assert.deepEqual(env.counts(), { requests: 1, stopped: 1, terminated: 1 })
})

test('permission denial is reported without claiming the camera is active', async t => {
  const env = environment(t, () => { throw new DOMException('Denied', 'NotAllowedError') })
  await assert.rejects(env.open(), /Chưa được cấp quyền camera/)
  assert.deepEqual(env.events, [])
  assert.equal(env.video.srcObject, null)
})
