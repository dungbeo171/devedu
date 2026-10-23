import { test } from 'node:test'
import assert from 'node:assert/strict'
import { verdictLabel, matchesSubmissionFilter, languageLabel, difficultyLabel, submissionFilterLabels, scoringRuleLabel, contestErrorLabel } from './contestPresentation.ts'

test('Vietnamese difficulty labels preserve existing contest and problem codes', () => {
  for (const code of ['BEGINNER', 'EASY']) assert.equal(difficultyLabel(code), 'Dễ')
  for (const code of ['INTERMEDIATE', 'MEDIUM']) assert.equal(difficultyLabel(code), 'Trung bình')
  for (const code of ['ADVANCED', 'HARD']) assert.equal(difficultyLabel(code), 'Khó')
  assert.equal(difficultyLabel(null), 'Chưa xác định')
  assert.equal(submissionFilterLabels['Wrong Answer'], 'Sai kết quả')
})

test('translates known server copy without changing custom rules or unknown errors', () => {
  assert.equal(scoringRuleLabel('Mỗi bài Accepted được cộng toàn bộ điểm một lần.'), 'Mỗi bài giải đúng được cộng toàn bộ điểm một lần.')
  assert.equal(scoringRuleLabel('Custom scoring rule'), 'Custom scoring rule')
  assert.equal(contestErrorLabel('Contest has ended'), 'Cuộc thi đã kết thúc.')
  assert.equal(contestErrorLabel('Thông báo từ người tổ chức'), 'Thông báo từ người tổ chức')
})

test('maps supported verdicts and language labels without fabricating a result', () => {
  assert.equal(verdictLabel('TIME_LIMIT'), 'Quá thời gian')
  assert.equal(verdictLabel('COMPILE_ERROR'), 'Lỗi biên dịch')
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
