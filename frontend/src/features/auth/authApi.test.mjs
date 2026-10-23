import { test } from 'node:test'
import assert from 'node:assert/strict'

test('uses the current JWT role instead of a stale cached user role', async t => {
  const values = new Map()
  const localStorage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: key => values.delete(key),
  }
  const original = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
  Object.defineProperty(globalThis, 'localStorage', { value: localStorage, configurable: true })
  t.after(() => original
    ? Object.defineProperty(globalThis, 'localStorage', original)
    : delete globalThis.localStorage)

  const payload = Buffer.from(JSON.stringify({ role: 'ADMIN', exp: Math.floor(Date.now() / 1000) + 3600 }))
    .toString('base64url')
  values.set('devedu.accessToken', `header.${payload}.signature`)
  values.set('devedu.user', JSON.stringify({ id: 0, publicId: 0, name: 'Admin', email: 'admin@devedu.com', role: 'STUDENT' }))

  const { getStoredUser } = await import('./api/authApi.ts')
  assert.equal(getStoredUser().role, 'ADMIN')
  assert.equal(JSON.parse(values.get('devedu.user')).role, 'ADMIN')
})
