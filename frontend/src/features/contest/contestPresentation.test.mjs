import { test } from 'node:test'
import assert from 'node:assert/strict'
import { verdictLabel, matchesSubmissionFilter, languageLabel } from './contestPresentation.ts'

test('maps supported verdicts and language labels without fabricating a result', () => {
  assert.equal(verdictLabel('TIME_LIMIT'), 'Time Limit Exceeded')
  assert.equal(verdictLabel('COMPILE_ERROR'), 'Compilation Error')
  assert.equal(verdictLabel('NEW_STATUS'), 'NEW_STATUS')
  assert.equal(languageLabel('CPP'), 'C++')
})
test('submission filters split accepted, wrong answer and other verdicts', () => {
  for (const status of ['ACCEPTED', 'WRONG_ANSWER', 'COMPILE_ERROR', 'RUNTIME_ERROR', 'TIME_LIMIT']) {
    assert.equal(matchesSubmissionFilter(status, 'All'), true)
    assert.equal(matchesSubmissionFilter(status, 'Accepted'), status === 'ACCEPTED')
    assert.equal(matchesSubmissionFilter(status, 'Wrong Answer'), status === 'WRONG_ANSWER')
    assert.equal(matchesSubmissionFilter(status, 'Other'), !['ACCEPTED', 'WRONG_ANSWER'].includes(status))
  }
})
