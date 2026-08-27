import { useEffect, useState, type FormEvent } from 'react'
import { addExamQuestion, createExam, getExamResults, getManagedExams } from '../api/examApi'
import type { CodeLanguage, ExamQuestionType, ExamSummary, TeacherExamResult } from '../types/exam'
import { IconArrowLeft, IconSave } from '../../../shared/components/Icons'

export function TeacherExamStudio({ onBack }: { onBack: () => void }) {
  const [exams, setExams] = useState<ExamSummary[]>([])
  const [examId, setExamId] = useState('')
  const [mode, setMode] = useState<'create' | 'question' | 'results'>('create')
  const [values, setValues] = useState<Record<string, string>>({
    duration: '60',
    points: '1',
    position: '1',
    type: 'MULTIPLE_CHOICE',
    language: 'JAVA',
  })
  const [message, setMessage] = useState('')
  const [results, setResults] = useState<TeacherExamResult[]>([])

  const update = (key: string, value: string) =>
    setValues((current) => ({ ...current, [key]: value }))

  const load = () =>
    void getManagedExams()
      .then((items) => {
        setExams(items)
        if (!examId && items[0]) setExamId(items[0].id)
      })
      .catch((e: unknown) =>
        setMessage(e instanceof Error ? e.message : 'Không thể tải kỳ thi.')
      )

  useEffect(load, [])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setMessage('')
    try {
      if (mode === 'create') {
        const exam = await createExam({
          slug: values.slug ?? '',
          title: values.title ?? '',
          description: values.description ?? '',
          scheduledAt: new Date(values.scheduledAt ?? '').toISOString(),
          durationMinutes: Number(values.duration),
        })
        setExamId(exam.id)
        setMessage(`Đã tạo kỳ thi thành công. ID: ${exam.id}`)
        load()
      } else if (mode === 'question') {
        const type = values.type as ExamQuestionType
        const options =
          type === 'MULTIPLE_CHOICE'
            ? (values.options ?? '').split('\n').map((v) => v.trim()).filter(Boolean)
            : []
        await addExamQuestion(examId, {
          type,
          prompt: values.prompt ?? '',
          options,
          correctOptionIndex:
            type === 'MULTIPLE_CHOICE' ? Number(values.correct) - 1 : undefined,
          codingLanguage:
            type === 'CODING' ? (values.language as CodeLanguage) : undefined,
          points: Number(values.points),
          position: Number(values.position),
        })
        setMessage('Đã thêm câu hỏi vào kỳ thi.')
      } else {
        setResults(await getExamResults(examId))
      }
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Không thể lưu kỳ thi.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-slate-900/80 px-3.5 py-1.5 text-xs font-bold text-slate-300 shadow-sm backdrop-blur-md transition hover:border-white/20 hover:bg-slate-800 hover:text-white"
        >
          <IconArrowLeft className="h-3.5 w-3.5" />
          <span>Giao diện sinh viên</span>
        </button>
        <div className="flex gap-2">
          {(['create', 'question', 'results'] as const).map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setMode(item)}
              className={`rounded-xl px-4 py-2 text-xs font-bold transition-all ${
                mode === item
                  ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-md shadow-blue-600/30 ring-1 ring-white/20'
                  : 'border border-white/10 bg-slate-900/80 text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              {item === 'create'
                ? 'Tạo kỳ thi'
                : item === 'question'
                ? 'Thêm câu hỏi'
                : 'Kết quả bài thi'}
            </button>
          ))}
        </div>
      </div>

      <form
        onSubmit={(e) => void submit(e)}
        className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/40 backdrop-blur-md sm:p-8"
      >
        <div className="grid gap-5 sm:grid-cols-2">
          {mode !== 'create' ? (
            <Select
              label="Chọn kỳ thi"
              value={examId}
              onChange={setExamId}
              options={exams.map((e) => ({ value: e.id, label: e.title }))}
            />
          ) : null}

          {mode === 'create' ? (
            <>
              <Field
                label="Slug URL"
                value={values.slug}
                onChange={(v) => update('slug', v)}
                placeholder="kiem-tra-giua-ky"
              />
              <Field
                label="Tên kỳ thi"
                value={values.title}
                onChange={(v) => update('title', v)}
                placeholder="Kiểm tra giữa kỳ Lập trình Web"
              />
              <Field
                label="Thời gian bắt đầu"
                type="datetime-local"
                value={values.scheduledAt}
                onChange={(v) => update('scheduledAt', v)}
              />
              <Field
                label="Thời lượng (phút)"
                type="number"
                value={values.duration}
                onChange={(v) => update('duration', v)}
              />
              <Area
                label="Mô tả kỳ thi"
                value={values.description}
                onChange={(v) => update('description', v)}
                placeholder="Nội dung, quy định phòng thi..."
              />
            </>
          ) : null}

          {mode === 'question' ? (
            <>
              <Select
                label="Loại câu hỏi"
                value={values.type}
                onChange={(v) => update('type', v)}
                options={[
                  { value: 'MULTIPLE_CHOICE', label: 'Multiple Choice (Trắc nghiệm)' },
                  { value: 'CODING', label: 'Coding (Tự luận lập trình)' },
                ]}
              />
              <Field
                label="Thứ tự câu hỏi"
                type="number"
                value={values.position}
                onChange={(v) => update('position', v)}
              />
              <Area
                label="Nội dung câu hỏi"
                value={values.prompt}
                onChange={(v) => update('prompt', v)}
                placeholder="Nội dung đề bài..."
              />
              {values.type === 'MULTIPLE_CHOICE' ? (
                <>
                  <Area
                    label="Các lựa chọn (mỗi dòng một đáp án)"
                    value={values.options}
                    onChange={(v) => update('options', v)}
                    placeholder="Lựa chọn A&#10;Lựa chọn B&#10;Lựa chọn C&#10;Lựa chọn D"
                  />
                  <Field
                    label="Số thứ tự đáp án đúng (1, 2, 3, ...)"
                    type="number"
                    value={values.correct}
                    onChange={(v) => update('correct', v)}
                  />
                </>
              ) : (
                <Select
                  label="Ngôn ngữ yêu cầu"
                  value={values.language}
                  onChange={(v) => update('language', v)}
                  options={['CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL'].map((v) => ({
                    value: v,
                    label: v,
                  }))}
                />
              )}
              <Field
                label="Điểm số"
                type="number"
                value={values.points}
                onChange={(v) => update('points', v)}
              />
            </>
          ) : null}

          <div className="sm:col-span-2">
            <button
              type="submit"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-blue-600/25 ring-1 ring-white/20 transition hover:from-blue-500 hover:to-indigo-500"
            >
              <IconSave className="h-3.5 w-3.5" />
              <span>{mode === 'results' ? 'Tải kết quả bài thi' : 'Lưu dữ liệu'}</span>
            </button>
          </div>
        </div>

        {message ? (
          <div className="mt-5 rounded-2xl border border-blue-500/30 bg-blue-500/10 p-4 text-xs font-semibold text-blue-300">
            {message}
          </div>
        ) : null}
      </form>

      {mode === 'results' && results.length > 0 ? (
        <div className="overflow-x-auto rounded-3xl border border-white/10 bg-slate-900/80 shadow-2xl shadow-black/40 backdrop-blur-md">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/10 bg-slate-950/60 font-mono text-[11px] font-bold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="p-4">Student ID</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4">Điểm trắc nghiệm</th>
                <th className="p-4">Bài làm sinh viên</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => (
                <tr key={r.attemptId} className="border-t border-white/5 text-slate-300 transition hover:bg-white/5">
                  <td className="p-4 font-mono text-xs font-semibold text-white">{r.studentId}</td>
                  <td className="p-4">
                    <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-1 font-mono text-[10px] font-bold text-blue-400">
                      {r.status}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className="font-bold text-white">
                      {r.automaticScore} / {r.automaticMaxScore}
                    </span>
                    {r.pendingCodingQuestions > 0 ? (
                      <span className="block text-xs font-semibold text-amber-400">
                        ⏳ {r.pendingCodingQuestions} coding chờ chấm
                      </span>
                    ) : null}
                  </td>
                  <td className="p-4">
                    <details className="group">
                      <summary className="cursor-pointer font-bold text-blue-400 hover:text-cyan-300">
                        {r.answers.length} câu đã trả lời ▾
                      </summary>
                      <div className="mt-3 space-y-2">
                        {r.answers.map((answer) => (
                          <pre
                            key={answer.id}
                            className="max-w-md overflow-x-auto whitespace-pre-wrap rounded-xl border border-white/10 bg-slate-950 p-3 font-mono text-xs text-slate-200"
                          >
                            {answer.sourceCode ?? `Lựa chọn #${(answer.selectedOptionIndex ?? 0) + 1}`}
                          </pre>
                        ))}
                      </div>
                    </details>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}

function Field({
  label,
  value = '',
  onChange,
  type = 'text',
  placeholder,
}: {
  label: string
  value?: string
  onChange: (v: string) => void
  type?: string
  placeholder?: string
}) {
  return (
    <label className="block text-xs font-bold text-slate-300">
      {label}
      <input
        required
        type={type}
        min={type === 'number' ? 1 : undefined}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="mt-2 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3.5 py-2.5 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
      />
    </label>
  )
}

function Area({
  label,
  value = '',
  onChange,
  placeholder,
}: {
  label: string
  value?: string
  onChange: (v: string) => void
  placeholder?: string
}) {
  return (
    <label className="block text-xs font-bold text-slate-300 sm:col-span-2">
      {label}
      <textarea
        required
        rows={4}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="mt-2 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3.5 py-2.5 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
      />
    </label>
  )
}

function Select({
  label,
  value,
  onChange,
  options,
}: {
  label: string
  value: string
  onChange: (v: string) => void
  options: { value: string; label: string }[]
}) {
  return (
    <label className="block text-xs font-bold text-slate-300">
      {label}
      <select
        required
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-2 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3.5 py-2.5 text-xs font-semibold text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value} className="bg-slate-900 text-white">
            {o.label}
          </option>
        ))}
      </select>
    </label>
  )
}
