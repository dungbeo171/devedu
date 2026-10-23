import { useState, type FormEvent } from 'react'
import { IconCode } from '../../../shared/components/Icons'
import { generateProgrammingProblemDraft } from '../api/programmingProblemsApi'
import type { CreateProgrammingProblem, ProgrammingProblemDetail } from '../types/programmingProblem'
import { ProblemCreator } from './ProblemCreator'

const MINIMUM_DESCRIPTION_LENGTH = 20
const MAXIMUM_DESCRIPTION_LENGTH = 4000

export function AiProblemGenerator({
  onCreated,
  saveProblem,
  scopeLabel,
}: {
  onCreated: (problem: ProgrammingProblemDetail) => void
  saveProblem?: (problem: CreateProgrammingProblem) => Promise<ProgrammingProblemDetail>
  scopeLabel?: string
}) {
  const [description, setDescription] = useState('')
  const [draft, setDraft] = useState<CreateProgrammingProblem | null>(null)
  const [draftRevision, setDraftRevision] = useState(0)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')

  async function generate(event: FormEvent) {
    event.preventDefault()
    const prompt = description.trim()
    if (prompt.length < MINIMUM_DESCRIPTION_LENGTH) {
      setError('Hãy mô tả bài tập bằng ít nhất 20 ký tự.')
      return
    }

    setGenerating(true)
    setError('')
    try {
      const generated = await generateProgrammingProblemDraft(prompt)
      setDraft(generated)
      setDraftRevision((current) => current + 1)
    } catch (reason) {
      const timedOut = reason instanceof DOMException && reason.name === 'TimeoutError'
      setError(timedOut
        ? 'Gemini phản hồi quá lâu. Vui lòng thử lại.'
        : reason instanceof Error ? reason.message : 'Không thể tạo bài tập bằng Gemini.')
    } finally {
      setGenerating(false)
    }
  }

  return (
    <div className="space-y-5">
      <form onSubmit={(event) => void generate(event)} className="ui-panel p-5 sm:p-7">
        <div className="flex items-start gap-3">
          <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-blue-600 text-white">
            <IconCode className="h-5 w-5" />
          </span>
          <div>
            <h1 className="text-xl font-bold text-slate-950">Tạo bản nháp bằng Gemini</h1>
            <p className="mt-1 text-sm leading-6 text-slate-600">
              Mô tả mục tiêu, chủ đề, độ khó và ngôn ngữ mong muốn. Gemini sẽ điền đề bài, code khởi tạo và test case vào form để bạn kiểm tra.
            </p>
          </div>
        </div>

        <label className="mt-5 block text-sm font-semibold text-slate-700" htmlFor="ai-problem-description">
          Mô tả bài tập
        </label>
        <textarea
          id="ai-problem-description"
          rows={7}
          minLength={MINIMUM_DESCRIPTION_LENGTH}
          maxLength={MAXIMUM_DESCRIPTION_LENGTH}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          disabled={generating}
          className="ui-control mt-2 resize-y"
          placeholder="Ví dụ: Tạo bài mức dễ về mảng một chiều. Người học tìm phần tử lớn thứ hai bằng C++, Java và Python; dữ liệu gồm n và n số nguyên..."
        />
        <div className="mt-2 flex flex-col gap-2 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>Nội dung mô tả được gửi tới Gemini. Không nhập thông tin cá nhân hoặc dữ liệu bí mật.</p>
          <span className="shrink-0 tabular-nums">{description.length}/{MAXIMUM_DESCRIPTION_LENGTH}</span>
        </div>

        {error ? (
          <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-semibold text-red-700">
            {error}
          </p>
        ) : null}

        <div className="mt-5 flex justify-end">
          <button
            type="submit"
            disabled={generating}
            className="ui-button-primary min-w-40"
          >
            {generating ? 'Gemini đang tạo...' : draft ? 'Tạo lại bản nháp' : 'Tạo bằng Gemini'}
          </button>
        </div>
      </form>

      {draft ? (
        <div>
          <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
            <strong>Đã tạo bản nháp.</strong> Hãy kiểm tra kỹ đề bài, code và expected output của từng test case trước khi lưu.
          </div>
          <ProblemCreator
            key={draftRevision}
            standalone
            initialDraft={draft}
            onCancel={() => setDraft(null)}
            onCreated={onCreated}
            saveProblem={saveProblem}
            scopeLabel={scopeLabel}
          />
        </div>
      ) : null}
    </div>
  )
}
