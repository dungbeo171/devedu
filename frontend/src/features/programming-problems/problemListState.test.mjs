import { test } from 'node:test'
import assert from 'node:assert/strict'
import { problemListUrl, readProblemListState } from './problemListState.ts'

test('a fresh problem list has no filters and starts on page one', () => {
  assert.deepEqual(readProblemListState(''), {
    topic: null, difficulty: '', language: '', progress: '', search: '', page: 1,
  })
  assert.equal(problemListUrl(readProblemListState('')), '/problems')
})

test('restores every filter and the current page from the previous list URL', () => {
  const state = {
    topic: 'ALGORITHMS', difficulty: 'MEDIUM', language: 'JAVA',
    progress: 'UNSOLVED', search: 'Tổng & hiệu + 2', page: 3,
  }
  const previousUrl = problemListUrl(state)
  assert.deepEqual(readProblemListState(new URL(previousUrl, 'http://localhost').search), state)
  assert.equal(new URL(previousUrl, 'http://localhost').pathname, '/problems')
})

test('invalid query values cannot become filters or invalid page indexes', () => {
  for (const page of ['0', '-2', 'NaN', 'Infinity', '1.2', '9007199254740992']) {
    assert.deepEqual(readProblemListState(`?topic=invalid&difficulty=invalid&language=invalid&progress=invalid&page=${page}`),
      readProblemListState(''))
  }
})

test('clearing filters removes their URL parameters and retired sorting is ignored', () => {
  const initial = readProblemListState('?topic=JAVA&difficulty=HARD&language=JAVA&progress=SOLVED&q=test&page=4&sort=ASC')
  const cleared = { ...initial, difficulty: '', language: '', progress: '', search: '', page: 1 }
  assert.equal(problemListUrl(cleared), '/problems?topic=JAVA')
  assert.equal(problemListUrl(readProblemListState('?sort=DESC')), '/problems')
})
