import { useEffect, useState } from 'react'
import { SmartCodeEditor } from '../../../shared/components/SmartCodeEditor'
import {
  getProgrammingProblem,
  runProgrammingProblemCode,
  submitProgrammingProblem,
} from '../api/programmingProblemsApi'
import {
  topicLabels,
  type ProgrammingProblemDetail,
  type SubmissionLanguage,
} from '../types/programmingProblem'

interface ProblemWorkspaceProps {
  slug: string
  onBack: () => void
}

interface LanguageOption {
  value: SubmissionLanguage
  label: string
  fileName: string
  starter: string
}

const languageOptions: LanguageOption[] = [
  {
    value: 'CPP',
    label: 'C++',
    fileName: 'main.cpp',
    starter: `#include <iostream>
using namespace std;

int main() {

    return 0;
}`,
  },
  {
    value: 'JAVA',
    label: 'Java',
    fileName: 'Main.java',
    starter: `public class Main {
    public static void main(String[] args) {

    }
}`,
  },
  {
    value: 'PYTHON',
    label: 'Python',
    fileName: 'main.py',
    starter: 'pass\n',
  },
  {
    value: 'HTML',
    label: 'HTML',
    fileName: 'index.html',
    starter: `<!doctype html>
<html lang="vi">
  <body>

  </body>
</html>`,
  },
  {
    value: 'MYSQL',
    label: 'MySQL',
    fileName: 'query.sql',
    starter: 'SELECT 1;',
  },
]

export function ProblemWorkspace({ slug, onBack }: ProblemWorkspaceProps) {
  const [problem, setProblem] = useState<ProgrammingProblemDetail | null>(null)
  const [language, setLanguage] = useState<SubmissionLanguage>('CPP')
  const [sourceCode, setSourceCode] = useState(languageOptions[0].starter)
  const [input, setInput] = useState('')
  const [output, setOutput] = useState('Nhấn Chạy thử để xem output của chương trình.')
  const [loading, setLoading] = useState(true)
  const [running, setRunning] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setMessage('')

    void getProgrammingProblem(slug)
      .then((result) => {
        if (ignore) return
        setProblem(result)
        const initialLanguage = defaultLanguageForTopic(result.topic)
        const option = languageOptions.find((item) => item.value === initialLanguage) ?? languageOptions[0]
        setLanguage(initialLanguage)
        setSourceCode(option.starter)
        setInput(result.sampleInput)
        setOutput(result.sampleOutput
          ? `Output mẫu:\n${result.sampleOutput}`
          : 'Nhấn Chạy thử để xem output của chương trình.')
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

  const selectedLanguage = languageOptions.find((item) => item.value === language) ?? languageOptions[0]

  function changeLanguage(nextLanguage: SubmissionLanguage) {
    const option = languageOptions.find((item) => item.value === nextLanguage)
    if (!option) return
    setLanguage(nextLanguage)
    setSourceCode(option.starter)
    setOutput(problem?.sampleOutput
      ? `Output mẫu:\n${problem.sampleOutput}`
      : 'Nhấn Chạy thử để xem output của chương trình.')
    setMessage('')
  }

  async function runCode() {
    if (!sourceCode.trim() || running) return

    setRunning(true)
    setOutput('Đang chạy trong Docker sandbox...')
    try {
      const result = await runProgrammingProblemCode(language, sourceCode, input)
      setOutput(`[${result.status}]\n${result.output}`)
    } catch (error) {
      setOutput(error instanceof Error ? error.message : 'Không thể chạy thử code lúc này.')
    } finally {
      setRunning(false)
    }
  }

  async function submit() {
    if (!problem || !sourceCode.trim() || submitting) return

    setSubmitting(true)
    setMessage('')
    try {
      const submission = await submitProgrammingProblem(problem.slug, language, sourceCode)
      setMessage(`${submission.status} · ${submission.passedTests}/${submission.totalTests} test · ${submission.executionTimeMillis} ms · ${submission.diagnostic}`)
    } catch (error) {
      const reason = error instanceof Error ? error.message : ''
      if (reason === 'AUTHENTICATION_REQUIRED') {
        setMessage('Bạn cần đăng nhập bằng tài khoản sinh viên trước khi submit.')
      } else if (reason === 'STUDENT_ROLE_REQUIRED') {
        setMessage('Chỉ tài khoản STUDENT có thể submit bài.')
      } else {
        setMessage(reason || 'Không thể gửi bài lúc này.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="grid min-h-80 place-items-center rounded-2xl border border-blue-100 bg-white/60 text-sm text-slate-600">
        Đang tải đề bài...
      </div>
    )
  }

  if (!problem) {
    return (
      <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-8 text-center">
        <p className="text-sm text-rose-200">{message || 'Không tìm thấy bài tập.'}</p>
        <button type="button" onClick={onBack} className="mt-4 text-sm font-semibold text-blue-600">
          Quay lại danh sách
        </button>
      </div>
    )
  }

  return (
    <div>
      <button type="button" onClick={onBack} className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-slate-600 transition hover:text-blue-600">
        <span aria-hidden="true">←</span>
        Danh sách bài tập
      </button>

      <div className="grid overflow-hidden rounded-2xl border border-blue-100 bg-white/70 shadow-2xl shadow-blue-200/40 xl:grid-cols-[minmax(320px,0.8fr)_minmax(0,1.2fr)]">
        <article className="border-b border-blue-100 p-5 sm:p-7 xl:border-r xl:border-b-0">
          <span className="inline-flex rounded-full border border-blue-500/20 bg-blue-600/5 px-2.5 py-1 text-xs font-semibold text-blue-600">
            {topicLabels[problem.topic]}
          </span>
          <h3 className="mt-4 text-2xl font-bold tracking-tight text-slate-900">{problem.title}</h3>
          <p className="mt-3 text-sm leading-6 text-slate-600">{problem.summary}</p>

          <div className="my-6 h-px bg-blue-50" />
          <h4 className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Đề bài</h4>
          <p className="mt-3 whitespace-pre-line text-sm leading-7 text-slate-700">{problem.description}</p>

          <div className="mt-7 grid gap-3 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
            <div className="overflow-hidden rounded-xl border border-blue-100 bg-blue-50/60">
              <p className="border-b border-blue-100 px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">Sample Input</p>
              <pre className="overflow-auto whitespace-pre-wrap p-3 font-mono text-sm leading-6 text-slate-700">{problem.sampleInput || '(trống)'}</pre>
            </div>
            <div className="overflow-hidden rounded-xl border border-blue-100 bg-blue-50/60">
              <p className="border-b border-blue-100 px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">Sample Output</p>
              <pre className="overflow-auto whitespace-pre-wrap p-3 font-mono text-sm leading-6 text-slate-700">{problem.sampleOutput || '(trống)'}</pre>
            </div>
          </div>

        </article>

        <div className="flex min-h-[580px] flex-col">
          <div className="flex flex-col gap-3 border-b border-blue-100 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="font-mono text-xs text-slate-500">
              solution / <span className="text-slate-700">{selectedLanguage.fileName}</span>
            </p>
            <div className="flex items-center gap-2">
              <label htmlFor="problem-language" className="sr-only">Chọn ngôn ngữ</label>
              <select
                id="problem-language"
                value={language}
                onChange={(event) => changeLanguage(event.target.value as SubmissionLanguage)}
                className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-500"
              >
                {languageOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
              <button
                type="button"
                onClick={() => void runCode()}
                disabled={running || submitting || !sourceCode.trim()}
                className="rounded-lg border border-blue-500/40 bg-blue-600/10 px-4 py-2 text-sm font-bold text-blue-700 transition hover:bg-blue-600/20 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {running ? 'Đang chạy' : 'Chạy thử'}
              </button>
              <button
                type="button"
                onClick={() => void submit()}
                disabled={submitting || running || !sourceCode.trim()}
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-bold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting ? 'Đang gửi' : 'Submit'}
              </button>
            </div>
          </div>

          <div className="flex min-h-0 flex-1 flex-col">
            <div className="min-h-[380px] flex-1">
              <SmartCodeEditor
                key={language}
                editorId="problem-code-editor"
                language={language}
                value={sourceCode}
                onChange={setSourceCode}
              />
            </div>

            <div className="grid min-h-[230px] border-t border-blue-100 bg-blue-50/50 sm:grid-cols-2">
              <div className="flex min-h-[210px] flex-col border-b border-blue-100 sm:border-r sm:border-b-0">
                <div className="flex items-center justify-between gap-2 px-4 py-2.5">
                  <label htmlFor="problem-input" className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-600">
                    Input chạy thử
                  </label>
                  {problem.sampleInput ? (
                    <button
                      type="button"
                      onClick={() => setInput(problem.sampleInput)}
                      className="text-[11px] font-medium text-blue-600 hover:text-blue-700"
                    >
                      Khôi phục mẫu
                    </button>
                  ) : null}
                </div>
                <textarea
                  id="problem-input"
                  value={input}
                  onChange={(event) => setInput(event.target.value)}
                  placeholder="Có thể để trống nếu bài không cần input"
                  spellCheck={false}
                  className="min-h-0 flex-1 resize-none border-t border-blue-100/70 bg-white p-4 font-mono text-sm leading-6 text-slate-700 outline-none placeholder:text-slate-700 focus:bg-blue-50"
                />
              </div>

              <div className="flex min-h-[210px] flex-col">
                <div className="flex items-center justify-between gap-2 px-4 py-2.5">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-600">Output</p>
                  <span className="flex items-center gap-1.5 text-[11px] text-amber-300/80">
                    <span className="h-1.5 w-1.5 rounded-full bg-amber-300" />
                    Docker sandbox
                  </span>
                </div>
                <pre aria-live="polite" className="min-h-0 flex-1 overflow-auto whitespace-pre-wrap border-t border-blue-100/70 bg-blue-50 p-4 font-mono text-sm leading-6 text-slate-600">
                  {output}
                </pre>
              </div>
            </div>
          </div>

          {message ? (
            <p aria-live="polite" className="border-t border-blue-100 bg-blue-50 px-4 py-3 text-xs text-slate-700">{message}</p>
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
