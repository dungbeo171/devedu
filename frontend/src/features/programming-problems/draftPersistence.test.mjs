import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import { createDraftSession as createSession, draftStorageKey, draftSyncDelay } from './draftPersistence.ts'

const createDraftSession = options => createSession({ ...options, now: () => Date.now() })
const advance = async (t, ms) => { t.mock.timers.tick(ms); await settle() }
const clock = t => t.mock.timers.enable({ apis: ['setTimeout', 'Date'] })

const value = code => ({ language: 'JAVA', sourceCode: code, input: '' })
const record = (code, dirty = true) => ({ value: value(code), revision: crypto.randomUUID(), dirty })
const settle = () => new Promise(resolve => setImmediate(resolve))

function memoryStore(initial = null) {
  let latest = initial
  const languages = new Map(initial ? [[initial.value.language, initial]] : [])
  return {
    async read(language) { return structuredClone(language ? languages.get(language) ?? null : latest) },
    async write(draft) { latest = structuredClone(draft); languages.set(draft.value.language, latest) },
    async acknowledge(draft) {
      if (latest?.revision === draft.revision) latest = { ...latest, dirty: false }
      const saved = languages.get(draft.value.language)
      if (saved?.revision === draft.revision) languages.set(draft.value.language, { ...saved, dirty: false })
    },
  }
}

test('writes locally on each edit and sends only the latest code after 5 seconds idle', async t => {
  assert.equal(draftSyncDelay, 5000)
  clock(t)
  const store = memoryStore()
  const sent = []
  const session = createDraftSession({ store, serverKey: 'debounce', onWarning() {}, saveRemote: async draft => { sent.push(draft); return true } })
  session.edit(value('a'))
  await settle()
  assert.equal((await store.read()).value.sourceCode, 'a')
  t.mock.timers.tick(2000)
  session.edit(value('ab'))
  await settle()
  t.mock.timers.tick(draftSyncDelay - 1)
  await settle()
  assert.equal(sent.length, 0)
  t.mock.timers.tick(1)
  await settle()
  assert.deepEqual(sent, [value('ab')])
  assert.equal((await store.read()).dirty, false)
  session.dispose()
})

test('focus or online retries do not bypass the five-second debounce', async t => {
  clock(t)
  const sent = []
  const session = createDraftSession({ store: memoryStore(), serverKey: 'retry-delay', onWarning() {}, saveRemote: async draft => { sent.push(draft); return true } })
  session.edit(value('code'))
  await settle()
  t.mock.timers.tick(1000)
  session.retry()
  t.mock.timers.tick(3999)
  await settle()
  assert.equal(sent.length, 0)
  t.mock.timers.tick(1)
  await settle()
  assert.deepEqual(sent, [value('code')])
  session.dispose()
})

test('restores unsynchronized IndexedDB edits instead of overwriting them with the server', async () => {
  const session = createDraftSession({ store: memoryStore(record('offline code')), serverKey: 'restore', onWarning() {} })
  let fetched = false
  assert.deepEqual(await session.restore(async () => { fetched = true; return value('old') }), value('offline code'))
  assert.equal(fetched, false)
  session.dispose()
})

test('a clean local copy does not hide newer server code; offline falls back to local', async () => {
  const session = createDraftSession({ store: memoryStore(record('local', false)), serverKey: 'clean', onWarning() {} })
  assert.deepEqual(await session.restore(async () => value('new remote')), value('new remote'))
  assert.deepEqual(await session.restore(async () => { throw new Error('offline') }), value('local'))
  session.dispose()
})

test('network failure retains dirty code and retry sends only after the idle deadline', async t => {
  clock(t)
  const store = memoryStore()
  let online = false
  const warnings = []
  const session = createDraftSession({ store, serverKey: 'retry', onWarning: message => warnings.push(message), saveRemote: async () => online })
  session.edit(value('keep me'))
  await advance(t, 5000)
  assert.equal((await store.read()).dirty, true)
  assert.ok(warnings.at(-1).includes('chưa đồng bộ'))
  online = true
  session.retry()
  await advance(t, 1)
  assert.equal((await store.read()).dirty, false)
  session.dispose()
})

test('an old response cannot acknowledge or send code typed during an in-flight save', async t => {
  clock(t)
  const store = memoryStore()
  let finish
  const blocked = new Promise(resolve => { finish = resolve })
  const sent = []
  const session = createDraftSession({ store, serverKey: 'in-flight', onWarning() {}, saveRemote: async draft => {
    sent.push(draft.sourceCode)
    if (sent.length === 1) await blocked
    return true
  } })
  session.edit(value('old'))
  await advance(t, 5000)
  session.edit(value('new'))
  await settle()
  finish()
  await settle()
  assert.equal((await store.read()).value.sourceCode, 'new')
  assert.equal((await store.read()).dirty, true)
  await advance(t, 4999)
  assert.deepEqual(sent, ['old'])
  await advance(t, 1)
  assert.deepEqual(sent, ['old', 'new'])
  assert.equal((await store.read()).dirty, false)
  session.dispose()
})

test('navigation does not send early and keeps the unsynchronized local draft', async t => {
  clock(t)
  const sent = []
  const store = memoryStore()
  const session = createDraftSession({ store, serverKey: 'leave', onWarning() {}, saveRemote: async draft => { sent.push(draft); return true } })
  session.edit(value('before leave'))
  await advance(t, 1000)
  session.dispose()
  await advance(t, 10000)
  assert.deepEqual(sent, [])
  assert.equal((await store.read()).dirty, true)
  assert.equal((await store.read()).value.sourceCode, 'before leave')
})

test('per-language drafts survive switching and empty code is still saved', async () => {
  const store = memoryStore()
  const session = createDraftSession({ store, serverKey: 'languages', onWarning() {} })
  session.edit(value(''))
  session.edit({ language: 'PYTHON', sourceCode: 'print(1)', input: 'test' })
  assert.deepEqual(await session.forLanguage('JAVA'), value(''))
  await session.persistLocal()
  assert.equal((await store.read()).value.language, 'PYTHON')
  session.dispose()
})

test('keys isolate accounts, guest drafts, problems, official and virtual sessions', () => {
  const keys = [
    draftStorageKey('user:1', 'problems', 'sum'),
    draftStorageKey('user:2', 'problems', 'sum'),
    draftStorageKey('guest', 'problems', 'sum'),
    draftStorageKey('user:1', 'problems', 'other'),
    draftStorageKey('user:1', 'contest:1:official', 'sum'),
    draftStorageKey('user:1', 'contest:1:virtual-1', 'sum'),
    draftStorageKey('user:1', 'problems', 'sum', 'JAVA'),
  ]
  assert.equal(new Set(keys).size, keys.length)
})

test('unavailable local storage still waits five seconds before server save', async t => {
  clock(t)
  const warnings = []
  const session = createDraftSession({
    store: { async read() { throw new Error('disabled') }, async write() { throw new Error('quota') }, async acknowledge() {} },
    serverKey: 'no-idb', onWarning: message => warnings.push(message), saveRemote: async () => true,
  })
  session.edit(value('code'))
  await advance(t, 5000)
  assert.ok(warnings.at(-1).includes('không lưu được trên thiết bị'))
  session.dispose()
})

test('Run or leaving the editor can wait for local persistence but cannot force a server write', async t => {
  clock(t)
  const sent = []
  const session = createDraftSession({ store: memoryStore(), serverKey: 'local-only', onWarning() {}, saveRemote: async draft => { sent.push(draft); return true } })
  session.edit(value('a'))
  await advance(t, 1000)
  await session.persistLocal()
  assert.equal(sent.length, 0)
  await advance(t, 3999)
  assert.equal(sent.length, 0)
  await advance(t, 1)
  assert.deepEqual(sent, [value('a')])
  session.dispose()
})

test('every keystroke restarts the full idle period, even after 4.9 seconds', async t => {
  clock(t)
  const sent = []
  const session = createDraftSession({ store: memoryStore(), serverKey: 'continuous', onWarning() {}, saveRemote: async draft => { sent.push(draft); return true } })
  for (let i = 0; i < 10; i++) {
    session.edit(value(String(i)))
    await advance(t, 4900)
    assert.equal(sent.length, 0)
  }
  await advance(t, 100)
  assert.deepEqual(sent, [value('9')])
  session.dispose()
})

test('an autosave waiting in the server queue rechecks the deadline after newer edits', async t => {
  clock(t)
  let finish
  const blocked = new Promise(resolve => { finish = resolve })
  const sent = []
  const first = createDraftSession({ store: memoryStore(), serverKey: 'shared', onWarning() {}, saveRemote: async () => { await blocked; return true } })
  const second = createDraftSession({ store: memoryStore(), serverKey: 'shared', onWarning() {}, saveRemote: async draft => { sent.push(draft); return true } })
  first.edit(value('first session'))
  second.edit(value('queued'))
  await advance(t, 5000)
  second.edit(value('just typed'))
  await second.persistLocal()
  finish()
  await settle()
  assert.equal(sent.length, 0)
  await advance(t, 4999)
  assert.equal(sent.length, 0)
  await advance(t, 1)
  assert.deepEqual(sent, [value('just typed')])
  first.dispose()
  second.dispose()
})

test('real HTTP autosave waits five seconds after the last edit, not after the first one', { timeout: 12000 }, async () => {
  const received = []
  let receive
  const arrived = new Promise(resolve => { receive = resolve })
  const server = createServer(async (request, response) => {
    let body = ''
    for await (const chunk of request) body += chunk
    received.push({ at: performance.now(), body: JSON.parse(body) })
    response.end('{}')
    receive()
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  const store = memoryStore()
  const session = createSession({ store, serverKey: 'real-http', onWarning() {}, saveRemote: async draft => {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/draft`, { method: 'PUT', body: JSON.stringify(draft) })
    return response.ok
  } })
  let timeout
  try {
    session.edit(value('first edit'))
    await new Promise(resolve => setTimeout(resolve, 1000))
    const lastEditAt = performance.now()
    session.edit(value('latest edit'))
    await session.persistLocal()
    assert.equal((await store.read()).value.sourceCode, 'latest edit')
    await new Promise(resolve => setTimeout(resolve, 1000))
    assert.equal(received.length, 0)
    await Promise.race([arrived, new Promise((_, reject) => { timeout = setTimeout(() => reject(new Error('No HTTP autosave')), 7000) })])
    assert.equal(received.length, 1)
    assert.deepEqual(received[0].body, value('latest edit'))
    assert.ok(received[0].at - lastEditAt >= 5000)
  } finally {
    clearTimeout(timeout)
    session.dispose()
    server.closeAllConnections()
    await new Promise(resolve => server.close(resolve))
  }
})
