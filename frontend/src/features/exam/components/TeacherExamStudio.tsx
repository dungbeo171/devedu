import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { addExamQuestion, createExam, getExamResults, getManagedExams } from '../api/examApi'
import type { CodeLanguage, ExamQuestionType, ExamSummary, TeacherExamResult } from '../types/exam'
import { IconArrowLeft, IconCheck, IconChevronDown, IconSave, IconSearch } from '../../../shared/components/Icons'
import { getProgrammingProblem, getProgrammingProblems } from '../../programming-problems/api/programmingProblemsApi'
import type {
  ProgrammingProblemDetail,
  ProgrammingProblemSummary,
} from '../../programming-problems/types/programmingProblem'

const answerLabels = ['A', 'B', 'C', 'D'] as const

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
  const [optionCount, setOptionCount] = useState(4)
  const [choiceOptions, setChoiceOptions] = useState(['', '', '', ''])
  const [correctOptionIndex, setCorrectOptionIndex] = useState(0)
  const [problemCatalog, setProblemCatalog] = useState<ProgrammingProblemSummary[]>([])
  const [problemCatalogLoaded, setProblemCatalogLoaded] = useState(false)
  const [problemCatalogLoading, setProblemCatalogLoading] = useState(false)
  const [problemCatalogError, setProblemCatalogError] = useState('')
  const [problemSearch, setProblemSearch] = useState('')
  const [selectedProblemSlug, setSelectedProblemSlug] = useState('')
  const [selectedProblem, setSelectedProblem] = useState<ProgrammingProblemDetail | null>(null)
  const [selectedProblemLoading, setSelectedProblemLoading] = useState(false)

  const visibleProblems = useMemo(() => {
    const query = problemSearch.trim().toLocaleLowerCase('vi')
    if (!query) return problemCatalog
    return problemCatalog.filter((problem) =>
      `${problem.title} ${problem.slug} ${problem.summary}`.toLocaleLowerCase('vi').includes(query),
    )
  }, [problemCatalog, problemSearch])

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

  useEffect(() => {
    if (mode !== 'question' || values.type !== 'CODING' || problemCatalogLoaded) return
    let cancelled = false
    setProblemCatalogLoading(true)
    setProblemCatalogError('')
    void getProgrammingProblems()
      .then((items) => {
        if (cancelled) return
        setProblemCatalog(items)
        setProblemCatalogLoaded(true)
        setSelectedProblemSlug((current) => current || items[0]?.slug || '')
      })
      .catch((error: unknown) => {
        if (cancelled) return
        setProblemCatalogError(error instanceof Error ? error.message : 'Không thể tải kho bài tập.')
      })
      .finally(() => {
        if (!cancelled) setProblemCatalogLoading(false)
      })
    return () => { cancelled = true }
  }, [mode, problemCatalogLoaded, values.type])

  useEffect(() => {
    if (mode !== 'question' || values.type !== 'CODING' || !selectedProblemSlug) {
      setSelectedProblem(null)
      return
    }
    let cancelled = false
    setSelectedProblem(null)
    setSelectedProblemLoading(true)
    setProblemCatalogError('')
    void getProgrammingProblem(selectedProblemSlug)
      .then((problem) => {
        if (cancelled) return
        setSelectedProblem(problem)
        setValues((current) => ({
          ...current,
          language: problem.allowedLanguages.includes(current.language as CodeLanguage)
            ? current.language
            : problem.allowedLanguages[0] ?? '',
        }))
      })
      .catch((error: unknown) => {
        if (cancelled) return
        setSelectedProblem(null)
        setProblemCatalogError(error instanceof Error ? error.message : 'Không thể tải bài tập.')
      })
      .finally(() => {
        if (!cancelled) setSelectedProblemLoading(false)
      })
    return () => { cancelled = true }
  }, [mode, selectedProblemSlug, values.type])

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
        const options = type === 'MULTIPLE_CHOICE'
          ? choiceOptions.slice(0, optionCount).map((option) => option.trim())
          : []
        if (type === 'MULTIPLE_CHOICE' && options.some((option) => !option)) {
          throw new Error('Vui lòng nhập đầy đủ nội dung cho tất cả đáp án.')
        }
        if (type === 'CODING' && !selectedProblem) {
          throw new Error('Vui lòng chọn một bài lập trình có sẵn.')
        }
        const prompt = type === 'CODING'
          ? createCodingQuestionPrompt(selectedProblem as ProgrammingProblemDetail)
          : values.prompt ?? ''
        await addExamQuestion(examId, {
          type,
          prompt,
          options,
          correctOptionIndex:
            type === 'MULTIPLE_CHOICE' ? correctOptionIndex : undefined,
          codingLanguage:
            type === 'CODING' ? (values.language as CodeLanguage) : undefined,
          points: Number(values.points),
          position: Number(values.position),
        })
        setValues((current) => ({
          ...current,
          prompt: '',
          position: String(Number(current.position || 0) + 1),
        }))
        setChoiceOptions(['', '', '', ''])
        setCorrectOptionIndex(0)
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
          className="ui-button-ghost px-0 hover:bg-transparent hover:text-blue-700"
        >
          <IconArrowLeft className="h-3.5 w-3.5" />
          <span>Danh sách kỳ thi</span>
        </button>
        <div className="flex gap-2">
          {(['create', 'question', 'results'] as const).map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setMode(item)}
              className={`min-h-10 rounded-[10px] px-4 text-xs font-bold transition-all ${
                mode === item
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'border border-slate-200 bg-white text-slate-600 hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700'
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
        className="ui-panel p-6 sm:p-8"
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
              {values.type === 'MULTIPLE_CHOICE' ? (
                <>
                  <Area
                    label="Nội dung câu hỏi"
                    value={values.prompt}
                    onChange={(v) => update('prompt', v)}
                    placeholder="Nhập nội dung câu hỏi trắc nghiệm..."
                  />
                  <Select
                    label="Số lượng đáp án"
                    value={String(optionCount)}
                    onChange={(value) => {
                      const count = Number(value)
                      setOptionCount(count)
                      setCorrectOptionIndex((current) => Math.min(current, count - 1))
                    }}
                    options={[2, 3, 4].map((count) => ({
                      value: String(count),
                      label: `${count} đáp án`,
                    }))}
                  />
                  <Select
                    label="Đáp án đúng"
                    value={String(correctOptionIndex)}
                    onChange={(value) => setCorrectOptionIndex(Number(value))}
                    options={answerLabels.slice(0, optionCount).map((label, index) => ({
                      value: String(index),
                      label: choiceOptions[index].trim()
                        ? `${label} — ${choiceOptions[index]}`
                        : `Đáp án ${label}`,
                    }))}
                  />
                  <div className="grid gap-3 sm:col-span-2 sm:grid-cols-2">
                    {answerLabels.slice(0, optionCount).map((label, index) => (
                      <label key={label} className="block text-sm font-semibold text-slate-700">
                        Đáp án {label}
                        <input
                          required
                          maxLength={1000}
                          value={choiceOptions[index]}
                          onChange={(event) => setChoiceOptions((current) =>
                            current.map((option, optionIndex) => optionIndex === index ? event.target.value : option))}
                          placeholder={`Nhập nội dung đáp án ${label}`}
                          className="ui-control mt-2"
                        />
                      </label>
                    ))}
                  </div>
                </>
              ) : (
                <>
                  <SearchableProblemSelect
                    label="Bài tập có sẵn"
                    value={selectedProblemSlug}
                    selectedLabel={problemCatalog.find((problem) => problem.slug === selectedProblemSlug)?.title ?? ''}
                    search={problemSearch}
                    onSearchChange={setProblemSearch}
                    onChange={setSelectedProblemSlug}
                    disabled={problemCatalogLoading || problemCatalog.length === 0}
                    options={visibleProblems.map((problem) => ({
                      value: problem.slug,
                      label: problem.title,
                      meta: difficultyLabel(problem.difficulty),
                    }))}
                  />

                  {problemCatalogLoading || selectedProblemLoading ? (
                    <div className="sm:col-span-2 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm font-semibold text-slate-500">
                      Đang tải kho bài tập...
                    </div>
                  ) : problemCatalogError ? (
                    <div role="alert" className="sm:col-span-2 rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
                      {problemCatalogError}
                    </div>
                  ) : problemCatalogLoaded && problemCatalog.length === 0 ? (
                    <div className="sm:col-span-2 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm font-semibold text-slate-500">
                      Chưa có bài lập trình trong hệ thống.
                    </div>
                  ) : null}

                  {selectedProblem ? (
                    <>
                      <Select
                        label="Ngôn ngữ làm bài"
                        value={values.language}
                        onChange={(v) => update('language', v)}
                        options={selectedProblem.allowedLanguages.map((language) => ({
                          value: language,
                          label: languageLabel(language),
                        }))}
                      />
                      <div className="sm:col-span-2 rounded-xl border border-blue-100 bg-blue-50/60 p-5">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="text-xs font-bold uppercase tracking-wider text-blue-700">Bài được chọn</p>
                            <h3 className="mt-1 text-lg font-bold text-slate-950">{selectedProblem.title}</h3>
                            <p className="mt-1 text-sm leading-6 text-slate-600">{selectedProblem.description}</p>
                          </div>
                          <span className="rounded-lg border border-blue-200 bg-white px-2.5 py-1 text-xs font-bold text-blue-700">
                            {difficultyLabel(selectedProblem.difficulty)}
                          </span>
                        </div>
                        <div className="mt-4 grid gap-3 md:grid-cols-2">
                          <QuestionPreview label="Yêu cầu Input" value={selectedProblem.inputDescription} />
                          <QuestionPreview label="Yêu cầu Output" value={selectedProblem.outputDescription} />
                          <QuestionPreview label="Input mẫu" value={selectedProblem.sampleInput || 'Không có'} code />
                          <QuestionPreview label="Output mẫu" value={selectedProblem.sampleOutput || 'Không có'} code />
                        </div>
                      </div>
                    </>
                  ) : null}
                </>
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
              className="ui-button-primary"
            >
              <IconSave className="h-3.5 w-3.5" />
              <span>{mode === 'results' ? 'Tải kết quả bài thi' : 'Lưu dữ liệu'}</span>
            </button>
          </div>
        </div>

        {message ? (
          <div className="mt-5 rounded-xl border border-blue-200 bg-blue-50 p-4 text-sm font-semibold text-blue-800">
            {message}
          </div>
        ) : null}
      </form>

      {mode === 'results' && results.length > 0 ? (
        <div className="ui-panel overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs font-bold text-slate-600">
              <tr>
                <th className="p-4">Student ID</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4">Điểm trắc nghiệm</th>
                <th className="p-4">Bài làm sinh viên</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => (
                <tr key={r.attemptId} className="border-t border-slate-100 text-slate-700 transition hover:bg-blue-50/40">
                  <td className="p-4 font-mono text-xs font-semibold text-slate-950">{r.studentId}</td>
                  <td className="p-4">
                    <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-1 font-mono text-[10px] font-bold text-blue-400">
                      {r.status}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className="font-bold text-slate-950">
                      {r.automaticScore} / {r.automaticMaxScore}
                    </span>
                    {r.pendingCodingQuestions > 0 ? (
                      <span className="block text-xs font-semibold text-amber-400">
                        {r.pendingCodingQuestions} coding chờ chấm
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
                            className="max-w-md overflow-x-auto whitespace-pre-wrap rounded-xl border border-slate-800 bg-slate-950 p-3 font-mono text-xs text-slate-200"
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
    <label className="block text-sm font-semibold text-slate-700">
      {label}
      <input
        required
        type={type}
        min={type === 'number' ? 1 : undefined}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="ui-control mt-2"
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
    <label className="block text-sm font-semibold text-slate-700 sm:col-span-2">
      {label}
      <textarea
        required
        rows={4}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="ui-control mt-2"
      />
    </label>
  )
}

function Select({
  label,
  value,
  onChange,
  options,
  disabled = false,
}: {
  label: string
  value: string
  onChange: (v: string) => void
  options: { value: string; label: string }[]
  disabled?: boolean
}) {
  return (
    <label className="block text-sm font-semibold text-slate-700">
      {label}
      <select
        required
        disabled={disabled}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="ui-control mt-2 font-semibold disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value} className="bg-white text-slate-900">
            {o.label}
          </option>
        ))}
      </select>
    </label>
  )
}

function SearchableProblemSelect({
  label,
  value,
  selectedLabel,
  search,
  onSearchChange,
  onChange,
  options,
  disabled,
}: {
  label: string
  value: string
  selectedLabel: string
  search: string
  onSearchChange: (value: string) => void
  onChange: (value: string) => void
  options: { value: string; label: string; meta: string }[]
  disabled: boolean
}) {
  return (
    <div className="relative z-20 sm:col-span-2">
      <p className="text-sm font-semibold text-slate-700">{label}</p>
      <details className="group relative mt-2">
        <summary
          aria-disabled={disabled}
          onClick={(event) => { if (disabled) event.preventDefault() }}
          className={`ui-control flex list-none items-center justify-between gap-3 font-semibold [&::-webkit-details-marker]:hidden ${
            disabled ? 'cursor-not-allowed bg-slate-100 text-slate-400' : 'cursor-pointer hover:border-blue-300 group-open:border-blue-400 group-open:ring-2 group-open:ring-blue-100'
          }`}
        >
          <span className="min-w-0 truncate">{selectedLabel || (disabled ? 'Đang tải danh sách bài tập...' : 'Chọn bài tập')}</span>
          <IconChevronDown className="h-4 w-4 shrink-0 text-slate-400 transition-transform group-open:rotate-180" />
        </summary>
        <div className="absolute inset-x-0 top-[calc(100%+.4rem)] z-50 rounded-xl border border-slate-200 bg-white p-2 shadow-[0_18px_45px_-18px_rgba(15,23,42,.35)]">
          <label className="relative block">
            <span className="sr-only">Tìm bài lập trình</span>
            <IconSearch className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={search}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder="Tìm theo tên hoặc slug..."
              className="ui-control ui-control-with-leading-icon pr-3 text-sm font-medium"
            />
          </label>
          <div className="mt-2 max-h-64 overflow-y-auto">
            {options.length > 0 ? options.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={(event) => {
                  onChange(option.value)
                  event.currentTarget.closest('details')?.removeAttribute('open')
                }}
                className={`flex min-h-10 w-full cursor-pointer items-center justify-between gap-3 rounded-lg px-3 py-2 text-left transition ${
                  option.value === value ? 'bg-blue-50 text-blue-700' : 'text-slate-700 hover:bg-slate-100'
                }`}
              >
                <span className="min-w-0 truncate text-sm font-semibold">{option.label}</span>
                <span className="flex shrink-0 items-center gap-2 text-xs text-slate-500">
                  {option.meta}
                  {option.value === value ? <IconCheck className="h-4 w-4 text-blue-600" /> : null}
                </span>
              </button>
            )) : (
              <p className="px-3 py-5 text-center text-sm font-medium text-slate-500">Không tìm thấy bài tập phù hợp.</p>
            )}
          </div>
        </div>
      </details>
    </div>
  )
}

function QuestionPreview({ label, value, code = false }: { label: string; value: string; code?: boolean }) {
  return (
    <div className="rounded-xl border border-blue-100 bg-white p-3.5">
      <p className="text-[11px] font-bold uppercase tracking-wider text-slate-500">{label}</p>
      <p className={`mt-1.5 whitespace-pre-wrap text-sm leading-6 text-slate-700 ${code ? 'font-mono' : ''}`}>
        {value}
      </p>
    </div>
  )
}

function createCodingQuestionPrompt(problem: ProgrammingProblemDetail): string {
  const sections = [
    problem.title,
    problem.description,
    `Yêu cầu Input:\n${problem.inputDescription}`,
    `Yêu cầu Output:\n${problem.outputDescription}`,
  ]
  if (problem.sampleInput || problem.sampleOutput) {
    sections.push(
      `Input mẫu:\n${problem.sampleInput || '(trống)'}`,
      `Output mẫu:\n${problem.sampleOutput || '(trống)'}`,
    )
  }
  return sections.join('\n\n')
}

function difficultyLabel(difficulty: ProgrammingProblemSummary['difficulty']): string {
  if (difficulty === 'EASY') return 'Dễ'
  if (difficulty === 'MEDIUM') return 'Trung bình'
  return 'Khó'
}

function languageLabel(language: CodeLanguage): string {
  if (language === 'CPP') return 'C++'
  if (language === 'MYSQL') return 'MySQL'
  return language[0] + language.slice(1).toLowerCase()
}
