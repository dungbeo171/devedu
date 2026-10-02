import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildWebPreview } from './webProject.ts'

test('bundles the local stylesheet and JavaScript with Unicode intact', () => {
  const files = { 'style.css': 'body { color: blue; }', 'script.js': "document.title = 'Xin chào';" }
  const result = buildWebPreview('<link rel="stylesheet" href="./style.css"><script src="script.js"></script>', files)
  const css = /data:text\/css;base64,([^"']+)/.exec(result)?.[1]
  const script = /data:text\/javascript;base64,([^"']+)/.exec(result)?.[1]
  assert.equal(Buffer.from(css, 'base64').toString('utf8'), files['style.css'])
  assert.equal(Buffer.from(script, 'base64').toString('utf8'), files['script.js'])
})

test('encodes closing tags instead of allowing file contents to break the HTML document', () => {
  const result = buildWebPreview("<link href='style.css'><script src='script.js'></script>", {
    'style.css': '/* </style><img src=x> */',
    'script.js': 'const value = "</script><h1>injected</h1>";',
  })
  assert.equal(result.includes('<h1>injected'), false)
  assert.equal(result.includes('<img'), false)
})

test('does not resolve remote assets or execute scripts while bundling', () => {
  const html = '<script src="https://example.com/script.js"></script><link href="other.css">'
  assert.equal(buildWebPreview(html, {}), html)
})
