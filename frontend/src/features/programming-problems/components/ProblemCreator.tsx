import { useState, type FormEvent, type ReactNode } from 'react'
import { createProgrammingProblem, updateProgrammingProblem } from '../api/programmingProblemsApi'
import {
  topicLabels,
  type CreateProblemTestCase,
  type ProblemDifficulty,
  type ProblemTopic,
  type ProgrammingProblemDetail,
  type ManagedProgrammingProblem,
  type SubmissionLanguage,
} from '../types/programmingProblem'
import { IconCode } from '../../../shared/components/Icons'
import { createStarterCode } from '../starterCode'

const topics = Object.entries(topicLabels) as [ProblemTopic, string][]
const languageOptions: { value: SubmissionLanguage; label: string }[] = [
  { value: 'CPP', label: 'C++' },
  { value: 'JAVA', label: 'Java' },
  { value: 'PYTHON', label: 'Python' },
  { value: 'HTML', label: 'HTML' },
  { value: 'MYSQL', label: 'MySQL' },
]
const emptyTestCase = (): CreateProblemTestCase => ({ input: '', expectedOutput: '', timeLimitMillis: 1000 })

export function ProblemCreator({ onCreated, standalone = false, onCancel, initialProblem }: {
  onCreated: (problem: ProgrammingProblemDetail) => void
  standalone?: boolean
  onCancel?: () => void
  initialProblem?: ManagedProgrammingProblem
}) {
  const [open, setOpen] = useState(standalone)
  const [title, setTitle] = useState(initialProblem?.title ?? '')
  const [slug, setSlug] = useState(initialProblem?.slug ?? '')
  const [slugEdited, setSlugEdited] = useState(Boolean(initialProblem))
  const [summary, setSummary] = useState(initialProblem?.summary ?? '')
  const [description, setDescription] = useState(initialProblem?.description ?? '')
  const [sampleInput, setSampleInput] = useState(initialProblem?.sampleInput ?? '')
  const [sampleOutput, setSampleOutput] = useState(initialProblem?.sampleOutput ?? '')
  const [topic, setTopic] = useState<ProblemTopic>(initialProblem?.topic ?? 'INTRODUCTION')
  const [difficulty, setDifficulty] = useState<ProblemDifficulty>(initialProblem?.difficulty ?? 'EASY')
  const [allowedLanguages, setAllowedLanguages] = useState<SubmissionLanguage[]>(
    initialProblem ? [...initialProblem.allowedLanguages] : ['CPP'],
  )
  const [starterCodes, setStarterCodes] = useState<Partial<Record<SubmissionLanguage, string>>>(() => ({
    ...(initialProblem?.starterCodes ?? { CPP: createStarterCode('CPP', '') }),
  }))
  const [editedStarterLanguages, setEditedStarterLanguages] = useState<SubmissionLanguage[]>(
    initialProblem ? [...initialProblem.allowedLanguages] : [],
  )
  const [testCases, setTestCases] = useState<CreateProblemTestCase[]>(
    initialProblem ? initialProblem.testCases.map((testCase) => ({ ...testCase })) : [emptyTestCase()],
  )
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function changeTitle(value: string) {
    setTitle(value)
    if (!slugEdited) setSlug(toSlug(value))
    setStarterCodes((current) => Object.fromEntries(
      allowedLanguages.map((language) => [
        language,
        editedStarterLanguages.includes(language)
          ? current[language] ?? ''
          : createStarterCode(language, value),
      ]),
    ))
  }

  function toggleLanguage(language: SubmissionLanguage) {
    setAllowedLanguages((current) => {
      if (current.includes(language)) return current.filter((item) => item !== language)
      setStarterCodes((codes) => ({
        ...codes,
        [language]: codes[language] ?? createStarterCode(language, title),
      }))
      return [...current, language]
    })
  }

  function updateTestCase(index: number, changes: Partial<CreateProblemTestCase>) {
    setTestCases((current) => current.map((testCase, position) =>
      position === index ? { ...testCase, ...changes } : testCase))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (allowedLanguages.length === 0) {
      setError('Chọn ít nhất một ngôn ngữ được phép.')
      return
    }
    setSubmitting(true)
    try {
      const request = {
        slug, title, summary, description, sampleInput, sampleOutput,
        topic, difficulty, allowedLanguages,
        starterCodes: Object.fromEntries(allowedLanguages.map((language) => [language, starterCodes[language] ?? ''])),
        testCases,
      }
      const created = initialProblem
        ? await updateProgrammingProblem(initialProblem.slug, request)
        : await createProgrammingProblem(request)
      if (!initialProblem) resetForm()
      if (!standalone) setOpen(false)
      onCreated(created)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : `Không thể ${initialProblem ? 'sửa' : 'thêm'} bài tập.`)
    } finally {
      setSubmitting(false)
    }
  }

  function resetForm() {
    setTitle('')
    setSlug('')
    setSlugEdited(false)
    setSummary('')
    setDescription('')
    setSampleInput('')
    setSampleOutput('')
    setTopic('INTRODUCTION')
    setDifficulty('EASY')
    setAllowedLanguages(['CPP'])
    setStarterCodes({ CPP: createStarterCode('CPP', '') })
    setEditedStarterLanguages([])
    setTestCases([emptyTestCase()])
    setError('')
  }

  return (
    <div className="mt-5">
      {!standalone ? (
        <button
          type="button"
          onClick={() => setOpen((current) => !current)}
          className="ui-button-primary"
        >
          <IconCode className="h-4 w-4" />
          <span>{open ? 'Đóng form' : 'Thêm bài tập'}</span>
        </button>
      ) : null}

      {open ? (
        <form onSubmit={(event) => void submit(event)} className={`${standalone ? '' : 'mt-4'} ui-panel p-5 sm:p-7`}>
          <div className="flex flex-col gap-1">
            <h1 className="text-2xl font-bold text-slate-950">{initialProblem ? 'Sửa bài tập lập trình' : 'Tạo bài tập lập trình'}</h1>
            <p className="mt-1 text-sm text-slate-600">Test case bên dưới được giữ kín và chỉ dùng khi chấm bài.</p>
          </div>

          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Field label="Tên bài tập">
              <input required maxLength={180} value={title} onChange={(event) => changeTitle(event.target.value)} className={inputClass} />
            </Field>
            <Field label="Slug tiếng Việt không dấu">
              <input required maxLength={120} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" value={slug} onChange={(event) => { setSlugEdited(true); setSlug(event.target.value.toLowerCase()) }} className={inputClass} />
            </Field>
            <Field label="Chủ đề">
              <select value={topic} onChange={(event) => setTopic(event.target.value as ProblemTopic)} className={inputClass}>
                {topics.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </Field>
            <Field label="Độ khó">
              <select value={difficulty} onChange={(event) => setDifficulty(event.target.value as ProblemDifficulty)} className={inputClass}>
                <option value="EASY">Dễ</option>
                <option value="MEDIUM">Trung bình</option>
                <option value="HARD">Khó</option>
              </select>
            </Field>
            <Field label="Tóm tắt" wide>
              <input required maxLength={500} value={summary} onChange={(event) => setSummary(event.target.value)} className={inputClass} />
            </Field>
            <Field label="Đề bài" wide>
              <textarea required maxLength={50000} rows={6} value={description} onChange={(event) => setDescription(event.target.value)} className={inputClass} />
            </Field>
            <Field label="Sample Input">
              <textarea rows={4} value={sampleInput} onChange={(event) => setSampleInput(event.target.value)} className={`${inputClass} font-mono`} />
            </Field>
            <Field label="Sample Output">
              <textarea rows={4} value={sampleOutput} onChange={(event) => setSampleOutput(event.target.value)} className={`${inputClass} font-mono`} />
            </Field>
          </div>

          <fieldset className="mt-5">
            <legend className="text-xs font-bold text-slate-700">Ngôn ngữ được phép</legend>
            <div className="mt-2 flex flex-wrap gap-2">
              {languageOptions.map((option) => (
                <label key={option.value} className={`cursor-pointer rounded-[10px] border px-3 py-2 text-xs font-bold transition ${allowedLanguages.includes(option.value) ? 'border-blue-600 bg-blue-600 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-blue-300 hover:bg-blue-50'}`}>
                  <input type="checkbox" className="sr-only" checked={allowedLanguages.includes(option.value)} onChange={() => toggleLanguage(option.value)} />
                  {option.label}
                </label>
              ))}
            </div>
          </fieldset>

          <fieldset className="mt-5">
            <legend className="text-xs font-bold text-slate-700">Code có sẵn theo ngôn ngữ</legend>
            <p className="mt-1 text-xs text-slate-500">Mỗi bài có template riêng. Người học vẫn có thể sửa hoặc xóa toàn bộ code này.</p>
            <div className="mt-3 space-y-3">
              {allowedLanguages.map((language) => {
                const option = languageOptions.find((item) => item.value === language)
                return (
                  <Field key={language} label={option?.label ?? language}>
                    <textarea
                      required
                      maxLength={100000}
                      rows={10}
                      spellCheck={false}
                      value={starterCodes[language] ?? ''}
                      onChange={(event) => {
                        setStarterCodes((current) => ({ ...current, [language]: event.target.value }))
                        setEditedStarterLanguages((current) => current.includes(language) ? current : [...current, language])
                      }}
                      className={`${inputClass} font-mono`}
                    />
                  </Field>
                )
              })}
            </div>
          </fieldset>

          <div className="mt-6 flex items-center justify-between gap-3">
            <h4 className="text-sm font-black text-slate-900">Test case ẩn</h4>
            <button type="button" onClick={() => setTestCases((current) => [...current, emptyTestCase()])} disabled={testCases.length >= 50} className="ui-button-secondary">
              + Thêm test case
            </button>
          </div>
          <div className="mt-3 space-y-3">
            {testCases.map((testCase, index) => (
              <div key={index} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-black text-blue-700">Test case #{index + 1}</span>
                  {testCases.length > 1 ? <button type="button" onClick={() => setTestCases((current) => current.filter((_, position) => position !== index))} className="text-xs font-bold text-blue-700 hover:underline">Xóa</button> : null}
                </div>
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  <Field label="Input">
                    <textarea rows={3} value={testCase.input} onChange={(event) => updateTestCase(index, { input: event.target.value })} className={`${inputClass} font-mono`} />
                  </Field>
                  <Field label="Expected Output">
                    <textarea rows={3} value={testCase.expectedOutput} onChange={(event) => updateTestCase(index, { expectedOutput: event.target.value })} className={`${inputClass} font-mono`} />
                  </Field>
                  <Field label="Time limit (ms)">
                    <input required type="number" min={100} max={30000} value={testCase.timeLimitMillis} onChange={(event) => updateTestCase(index, { timeLimitMillis: Number(event.target.value) })} className={inputClass} />
                  </Field>
                </div>
              </div>
            ))}
          </div>

          {error ? <p role="alert" className="mt-4 rounded-xl border border-blue-200 bg-blue-50 p-3 text-xs font-bold text-blue-800">{error}</p> : null}
          <div className="mt-6 flex flex-col-reverse justify-end gap-2 border-t border-slate-200 pt-5 sm:flex-row">
            <button type="button" onClick={() => onCancel ? onCancel() : setOpen(false)} className="ui-button-secondary">Hủy</button>
            <button type="submit" disabled={submitting} className="ui-button-primary">
              {submitting ? 'Đang lưu...' : initialProblem ? 'Lưu thay đổi' : 'Lưu bài tập'}
            </button>
          </div>
        </form>
      ) : null}
    </div>
  )
}

function Field({ label, wide = false, children }: { label: string; wide?: boolean; children: ReactNode }) {
  return <label className={`block text-sm font-semibold text-slate-700 ${wide ? 'sm:col-span-2' : ''}`}>{label}{children}</label>
}

const inputClass = 'ui-control mt-2'

function toSlug(value: string) {
  return value
    .trim()
    .toLocaleLowerCase('vi')
    .replace(/đ/g, 'd')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}
