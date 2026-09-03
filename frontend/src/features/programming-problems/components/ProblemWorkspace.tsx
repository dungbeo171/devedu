import { useEffect, useState } from 'react'
import { SmartCodeEditor } from '../../../shared/components/SmartCodeEditor'
import {
  getProgrammingProblem,
  getProgrammingProblemDraft,
  runProgrammingProblemCode,
  runProgrammingProblemTests,
  saveProgrammingProblemDraft,
  submitProgrammingProblem,
} from '../api/programmingProblemsApi'
import {
  topicLabels,
  type ProgrammingProblemDetail,
  type ProblemTestCaseRunResult,
  type SubmissionLanguage,
} from '../types/programmingProblem'
import {
  IconArrowLeft,
  IconCheck,
  IconChevronDown,
  IconPlay,
  IconSave,
} from '../../../shared/components/Icons'
import { createStarterCode } from '../starterCode'

interface ProblemWorkspaceProps {
  slug: string
  onBack: () => void
  onAccepted: (problemId: string) => void
}

type RunStatus = 'SUCCESS' | 'WRONG_ANSWER' | 'COMPILE_ERROR' | 'RUNTIME_ERROR' | 'TIME_LIMIT'

interface LanguageOption {
  value: SubmissionLanguage
  label: string
  fileName: string
}

const languageOptions: LanguageOption[] = [
  {
    value: 'CPP',
    label: 'C++',
    fileName: 'main.cpp',
  },
  {
    value: 'JAVA',
    label: 'Java',
    fileName: 'Main.java',
  },
  {
    value: 'PYTHON',
    label: 'Python',
    fileName: 'main.py',
  },
  {
    value: 'HTML',
    label: 'HTML',
    fileName: 'index.html',
  },
  {
    value: 'MYSQL',
    label: 'MySQL',
    fileName: 'query.sql',
  },
]

export function ProblemWorkspace({ slug, onBack, onAccepted }: ProblemWorkspaceProps) {
  const [problem, setProblem] = useState<ProgrammingProblemDetail | null>(null)
  const [language, setLanguage] = useState<SubmissionLanguage>('CPP')
  const [sourceCode, setSourceCode] = useState('')
  const [input, setInput] = useState('')
  const [output, setOutput] = useState('Nhấn Chạy test để kiểm tra toàn bộ test case.')
  const [loading, setLoading] = useState(true)
  const [running, setRunning] = useState(false)
  const [runningInput, setRunningInput] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [runStatus, setRunStatus] = useState<RunStatus | null>(null)
  const [testCaseResults, setTestCaseResults] = useState<ProblemTestCaseRunResult[]>([])
  const [draftReady, setDraftReady] = useState(false)

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setMessage('')
    setDraftReady(false)

    void getProgrammingProblem(slug)
      .then(async (result) => {
        if (ignore) return
        const draft = await getProgrammingProblemDraft(result.slug).catch(() => null)
        if (ignore) return
        setProblem(result)
        const allowedOptions = languageOptions.filter((item) => result.allowedLanguages.includes(item.value))
        const preferredLanguage = defaultLanguageForTopic(result.topic)
        const initialLanguage = result.allowedLanguages.includes(preferredLanguage)
          ? preferredLanguage
          : (allowedOptions[0]?.value ?? 'CPP')
        const restoredLanguage = draft && result.allowedLanguages.includes(draft.language)
          ? draft.language
          : initialLanguage
        setLanguage(restoredLanguage)
        setSourceCode(draft?.sourceCode ?? starterCodeFor(result, restoredLanguage))
        setInput(draft?.input ?? result.sampleInput)
        setOutput(result.sampleOutput
          ? `Output mẫu:\n${result.sampleOutput}`
          : 'Nhấn Chạy test để kiểm tra toàn bộ test case.')
        setRunStatus(null)
        setTestCaseResults([])
        setDraftReady(true)
      })
      .catch((error: unknown) => {
        if (!ignore) setMessage(error instanceof Error ? error.message : 'Không thể tải đề bài.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [slug])

  useEffect(() => {
    if (!problem || !draftReady) return
    const timeoutId = window.setTimeout(() => {
      void saveProgrammingProblemDraft(problem.slug, language, sourceCode, input)
        .catch(() => undefined)
    }, 700)
    return () => window.clearTimeout(timeoutId)
  }, [problem, language, sourceCode, input, draftReady])

  const selectedLanguage = languageOptions.find((item) => item.value === language) ?? languageOptions[0]
  const allowedLanguageOptions = problem
    ? languageOptions.filter((item) => problem.allowedLanguages.includes(item.value))
    : languageOptions

  function changeLanguage(nextLanguage: SubmissionLanguage) {
    const option = languageOptions.find((item) => item.value === nextLanguage)
    if (!option) return
    setLanguage(nextLanguage)
    setSourceCode(problem ? starterCodeFor(problem, nextLanguage) : createStarterCode(nextLanguage, ''))
    setOutput(problem?.sampleOutput
      ? `Output mẫu:\n${problem.sampleOutput}`
      : 'Nhấn Chạy test để kiểm tra toàn bộ test case.')
    setMessage('')
    setRunStatus(null)
    setTestCaseResults([])
  }

  async function runCode() {
    if (!problem || !sourceCode.trim() || running || runningInput || submitting) return

    setRunning(true)
    setMessage('')
    setRunStatus(null)
    setTestCaseResults([])
    setOutput('Đang chạy toàn bộ test case...')
    try {
      await saveProgrammingProblemDraft(problem.slug, language, sourceCode, input).catch(() => null)
      const result = await runProgrammingProblemTests(problem.slug, language, sourceCode)
      setTestCaseResults(result.testCases)
      setRunStatus(result.status === 'ACCEPTED' ? 'SUCCESS' : result.status)
      setOutput(`${result.passedTests}/${result.totalTests} test case đúng · ${result.executionTimeMillis} ms\n${result.diagnostic}`)
    } catch (error) {
      setRunStatus(null)
      setTestCaseResults([])
      const reason = error instanceof Error ? error.message : ''
      if (reason === 'AUTHENTICATION_REQUIRED') {
        setOutput('Bạn cần đăng nhập bằng tài khoản sinh viên trước khi chạy test case.')
      } else if (reason === 'STUDENT_ROLE_REQUIRED') {
        setOutput('Chỉ tài khoản STUDENT có thể chạy test case.')
      } else {
        setOutput(reason || 'Không thể chạy test case lúc này.')
      }
    } finally {
      setRunning(false)
    }
  }

  async function runCustomInput() {
    if (!sourceCode.trim() || running || runningInput || submitting) return
    setRunningInput(true)
    setMessage('')
    setRunStatus(null)
    setTestCaseResults([])
    setOutput('Đang chạy với input tùy chỉnh...')
    try {
      const result = await runProgrammingProblemCode(language, sourceCode, input)
      setOutput(result.output)
      if (result.status !== 'SUCCESS') setRunStatus(result.status)
    } catch (error) {
      setOutput(error instanceof Error ? error.message : 'Không thể chạy input lúc này.')
    } finally {
      setRunningInput(false)
    }
  }

  async function submit() {
    if (!problem || !sourceCode.trim() || submitting) return

    setSubmitting(true)
    setMessage('')
    try {
      await saveProgrammingProblemDraft(problem.slug, language, sourceCode, input).catch(() => null)
      const submission = await submitProgrammingProblem(problem.slug, language, sourceCode)
      if (submission.status === 'ACCEPTED') {
        onAccepted(submission.problemId)
        return
      }
      setMessage(`${submission.status} · ${submission.passedTests}/${submission.totalTests} test · ${submission.executionTimeMillis} ms · ${submission.diagnostic}`)
    } catch (error) {
      const reason = error instanceof Error ? error.message : ''
      if (reason === 'AUTHENTICATION_REQUIRED') {
        setMessage('Bạn cần đăng nhập bằng tài khoản sinh viên trước khi lưu bài.')
      } else if (reason === 'STUDENT_ROLE_REQUIRED') {
        setMessage('Chỉ tài khoản STUDENT có thể lưu bài.')
      } else {
        setMessage(reason || 'Không thể gửi bài lúc này.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function leaveWorkspace() {
    if (problem && draftReady) {
      await saveProgrammingProblemDraft(problem.slug, language, sourceCode, input).catch(() => null)
    }
    onBack()
  }

  if (loading) {
    return (
      <div className="ui-panel grid min-h-96 place-items-center p-8">
        <div className="flex items-center gap-3 text-sm font-semibold text-slate-600">
          <svg className="h-5 w-5 animate-spin text-blue-500" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <span>Đang tải đề bài & workspace...</span>
        </div>
      </div>
    )
  }

  if (!problem) {
    return (
      <div className="rounded-[18px] border border-red-200 bg-red-50 p-10 text-center">
        <p className="text-sm font-semibold text-red-700">{message || 'Không tìm thấy bài tập.'}</p>
        <button
          type="button"
          onClick={onBack}
          className="ui-button-primary mt-5"
        >
          <IconArrowLeft className="h-4 w-4" />
          <span>Quay lại danh sách bài tập</span>
        </button>
      </div>
    )
  }

  return (
    <div>
      {/* Back button */}
      <button
        type="button"
        onClick={() => void leaveWorkspace()}
        className="ui-button-ghost mb-5 px-0 hover:bg-transparent hover:text-blue-700"
      >
        <IconArrowLeft className="h-3.5 w-3.5" />
        <span>Danh sách bài tập</span>
      </button>

      {/* Main Workspace Frame */}
      <div className="grid overflow-hidden rounded-[18px] border border-slate-300 bg-white shadow-[0_18px_45px_-24px_rgba(15,23,42,.3)] xl:grid-cols-[minmax(340px,0.72fr)_minmax(0,1.68fr)]">
        {/* Problem Statement Pane */}
        <article className="border-b border-slate-200 bg-white p-6 sm:p-8 xl:border-r xl:border-b-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-[11px] font-bold text-blue-400">
              {topicLabels[problem.topic]}
            </span>
            <span className={`rounded-lg px-2.5 py-0.5 text-[11px] font-bold ${
              problem.difficulty === 'EASY'
              ? 'border border-emerald-200 bg-emerald-50 text-emerald-700'
                : problem.difficulty === 'MEDIUM'
                ? 'border border-amber-200 bg-amber-50 text-amber-700'
                : 'border border-red-200 bg-red-50 text-red-700'
            }`}>
              {problem.difficulty === 'EASY' ? 'Dễ' : problem.difficulty === 'MEDIUM' ? 'Trung bình' : 'Khó'}
            </span>
          </div>

          <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-950">{problem.title}</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">{problem.summary}</p>

          <div className="my-6 h-px bg-slate-200" />
          <h2 className="text-sm font-bold text-slate-900">Yêu cầu đề bài</h2>
          <p className="mt-3 whitespace-pre-line text-sm leading-7 text-slate-700">{problem.description}</p>

          <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
            <div className="overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
              <p className="border-b border-slate-200 bg-slate-100 px-3.5 py-2 font-mono text-[10px] font-bold uppercase tracking-wider text-slate-600">
                Sample Input
              </p>
              <pre className="overflow-auto whitespace-pre-wrap p-3.5 font-mono text-xs leading-6 text-slate-700">
                {problem.sampleInput || '(trống)'}
              </pre>
            </div>
            <div className="overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
              <p className="border-b border-slate-200 bg-slate-100 px-3.5 py-2 font-mono text-[10px] font-bold uppercase tracking-wider text-slate-600">
                Sample Output
              </p>
              <pre className="overflow-auto whitespace-pre-wrap p-3.5 font-mono text-xs leading-6 text-slate-700">
                {problem.sampleOutput || '(trống)'}
              </pre>
            </div>
          </div>
        </article>

        {/* Code Editor & Execution Pane */}
        <div className="flex min-h-[600px] flex-col bg-slate-950 text-slate-100">
          {/* Top action toolbar */}
          <div className="flex flex-col gap-3 border-b border-white/10 bg-slate-900/80 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="font-mono text-xs text-slate-400">
              solution / <span className="font-semibold text-slate-200">{selectedLanguage.fileName}</span>
            </p>
            <div className="flex items-center gap-2.5">
              <div className="relative">
                <label htmlFor="problem-language" className="sr-only">Chọn ngôn ngữ</label>
                <select
                  id="problem-language"
                  value={language}
                  onChange={(event) => changeLanguage(event.target.value as SubmissionLanguage)}
                  className="appearance-none rounded-xl border border-white/10 bg-slate-800/90 py-1.5 pl-3 pr-7 font-mono text-xs font-bold text-slate-200 outline-none hover:border-white/20 focus:border-blue-500"
                >
                  {allowedLanguageOptions.map((option) => (
                    <option key={option.value} value={option.value} className="bg-slate-900 text-white">
                      {option.label}
                    </option>
                  ))}
                </select>
                <IconChevronDown className="pointer-events-none absolute right-2 top-1/2 h-3 w-3 -translate-y-1/2 text-slate-400" />
              </div>

              <button
                type="button"
                onClick={() => void runCode()}
                disabled={running || runningInput || submitting || !sourceCode.trim()}
                className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-slate-800/90 px-3.5 py-1.5 text-xs font-bold text-slate-200 shadow-sm transition hover:bg-slate-700 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                {running ? (
                  <>
                    <svg className="h-3 w-3 animate-spin" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    <span>Đang chạy...</span>
                  </>
                ) : (
                  <>
                    <IconPlay className="h-3 w-3 text-emerald-400" />
                    <span>Chạy test</span>
                  </>
                )}
              </button>

              <button
                type="button"
                onClick={() => void submit()}
                disabled={submitting || running || runningInput || !sourceCode.trim()}
              className="ui-button-primary min-h-9 px-3 py-1.5"
              >
                {submitting ? (
                  <>
                    <svg className="h-3 w-3 animate-spin" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    <span>Đang lưu...</span>
                  </>
                ) : (
                  <>
                    <IconSave className="h-3.5 w-3.5" />
                    <span>Lưu bài</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Split editor & IO */}
          <div className="grid min-h-[610px] flex-1 lg:grid-cols-[minmax(0,1.55fr)_minmax(280px,0.85fr)]">
            <div className="flex min-h-[420px] flex-col border-b border-white/10 lg:border-r lg:border-b-0">
              <SmartCodeEditor
                key={language}
                editorId="problem-code-editor"
                language={language}
                value={sourceCode}
                onChange={(value) => {
                  setSourceCode(value)
                  setRunStatus(null)
                  setTestCaseResults([])
                }}
              />
            </div>

            <div className="grid min-h-[360px] grid-rows-2 bg-slate-950/90 dark-scroll">
              {/* Input test case */}
              <div className="flex min-h-[180px] flex-col border-b border-white/10">
                <div className="flex items-center justify-between gap-2 border-b border-white/5 bg-slate-900/60 px-4 py-2 text-xs">
                  <label htmlFor="problem-input" className="font-mono text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Input chạy thử
                  </label>
                  <div className="flex items-center gap-3">
                    {problem.sampleInput ? (
                      <button
                        type="button"
                        onClick={() => setInput(problem.sampleInput)}
                        className="font-mono text-[10px] font-semibold text-blue-400 hover:text-blue-300 hover:underline"
                      >
                        Khôi phục mẫu
                      </button>
                    ) : null}
                    <button
                      type="button"
                      onClick={() => void runCustomInput()}
                      disabled={running || runningInput || submitting || !sourceCode.trim()}
                      className="rounded-md border border-blue-500/40 bg-blue-500/15 px-2 py-1 font-mono text-[10px] font-bold text-blue-300 hover:bg-blue-500/25 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {runningInput ? 'Đang chạy...' : 'Chạy input'}
                    </button>
                  </div>
                </div>
                <textarea
                  id="problem-input"
                  value={input}
                  onChange={(event) => {
                    setInput(event.target.value)
                  }}
                  placeholder="Có thể để trống nếu bài không cần input"
                  spellCheck={false}
                  className="min-h-0 flex-1 resize-none bg-transparent p-4 font-mono text-xs leading-6 text-slate-300 outline-none placeholder:text-slate-600 focus:bg-slate-950/50"
                />
              </div>

              {/* Output & Judge status */}
              <div className="flex min-h-[180px] flex-col bg-slate-950">
                <div className="flex items-center justify-between gap-2 border-b border-white/10 bg-slate-900/60 px-4 py-2 text-xs">
                  <span className="font-mono text-[10px] font-bold uppercase tracking-wider text-slate-400">Output</span>
                  {runStatus === 'SUCCESS' ? (
                    <span className="flex items-center gap-1 rounded-md border border-emerald-500/40 bg-emerald-500/20 px-2 py-0.5 font-mono text-[10px] font-bold text-emerald-400">
                      <IconCheck className="h-3 w-3" /> SUCCESS
                    </span>
                  ) : runStatus === 'WRONG_ANSWER' ? (
                    <span className="rounded-md border border-rose-500/40 bg-rose-500/20 px-2 py-0.5 font-mono text-[10px] font-bold text-rose-300">
                      WRONG ANSWER
                    </span>
                  ) : runStatus ? (
                    <span className="rounded-md border border-rose-500/40 bg-rose-500/20 px-2 py-0.5 font-mono text-[10px] font-bold text-rose-300">
                      {runStatus}
                    </span>
                  ) : null}
                </div>
                {testCaseResults.length > 0 ? (
                  <div className="flex flex-wrap gap-2 border-b border-white/10 p-3">
                    {testCaseResults.map((testCase) => (
                      <div
                        key={testCase.position}
                        className={`flex items-center gap-2 rounded-lg border px-2.5 py-1.5 font-mono text-[10px] font-bold ${
                          testCase.passed
                            ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-400'
                            : 'border-rose-500/40 bg-rose-500/15 text-rose-400'
                        }`}
                        title={testCase.status}
                      >
                        {testCase.passed ? <IconCheck className="h-3.5 w-3.5" /> : <span className="text-base leading-none">×</span>}
                        <span>Test case {testCase.position}</span>
                      </div>
                    ))}
                  </div>
                ) : null}
                <pre
                  aria-live="polite"
                  className="min-h-0 flex-1 overflow-auto whitespace-pre-wrap p-4 font-mono text-xs leading-6 text-slate-300 selection:bg-blue-600/30"
                >
                  {output}
                </pre>
              </div>
            </div>
          </div>

          {message ? (
            <div aria-live="polite" className="border-t border-amber-800 bg-amber-950 px-4 py-3 text-xs font-semibold text-amber-200">
              {message}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}

function defaultLanguageForTopic(topic: ProgrammingProblemDetail['topic']): SubmissionLanguage {
  if (topic === 'JAVA') return 'JAVA'
  if (topic === 'PYTHON') return 'PYTHON'
  if (topic === 'SQL') return 'MYSQL'
  return 'CPP'
}

function starterCodeFor(problem: ProgrammingProblemDetail, language: SubmissionLanguage): string {
  return problem.starterCodes[language] ?? createStarterCode(language, problem.title)
}
