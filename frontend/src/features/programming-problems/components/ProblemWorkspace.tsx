import { useEffect, useState } from 'react'
import { SmartCodeEditor } from '../../../shared/components/SmartCodeEditor'
import { getProgrammingProblem, submitProgrammingProblem } from '../api/programmingProblemsApi'
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
    // Viết lời giải của bạn
    return 0;
}`,
  },
  {
    value: 'JAVA',
    label: 'Java',
    fileName: 'Main.java',
    starter: `public class Main {
    public static void main(String[] args) {
        // Viết lời giải của bạn
    }
}`,
  },
  {
    value: 'PYTHON',
    label: 'Python',
    fileName: 'main.py',
    starter: '# Viết lời giải của bạn\n',
  },
  {
    value: 'HTML',
    label: 'HTML',
    fileName: 'index.html',
    starter: `<!doctype html>
<html lang="vi">
  <body>
    <!-- Viết lời giải của bạn -->
  </body>
</html>`,
  },
  {
    value: 'MYSQL',
    label: 'MySQL',
    fileName: 'query.sql',
    starter: '-- Viết truy vấn của bạn\nSELECT 1;',
  },
]

export function ProblemWorkspace({ slug, onBack }: ProblemWorkspaceProps) {
  const [problem, setProblem] = useState<ProgrammingProblemDetail | null>(null)
  const [language, setLanguage] = useState<SubmissionLanguage>('CPP')
  const [sourceCode, setSourceCode] = useState(languageOptions[0].starter)
  const [loading, setLoading] = useState(true)
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
        const initialLanguage: SubmissionLanguage = result.topic === 'SQL' ? 'MYSQL' : 'CPP'
        const option = languageOptions.find((item) => item.value === initialLanguage) ?? languageOptions[0]
        setLanguage(initialLanguage)
        setSourceCode(option.starter)
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
    setMessage('')
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
      <div className="grid min-h-80 place-items-center rounded-2xl border border-slate-800 bg-slate-900/60 text-sm text-slate-400">
        Đang tải đề bài...
      </div>
    )
  }

  if (!problem) {
    return (
      <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-8 text-center">
        <p className="text-sm text-rose-200">{message || 'Không tìm thấy bài tập.'}</p>
        <button type="button" onClick={onBack} className="mt-4 text-sm font-semibold text-cyan-300">
          Quay lại danh sách
        </button>
      </div>
    )
  }

  return (
    <div>
      <button type="button" onClick={onBack} className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-slate-400 transition hover:text-cyan-300">
        <span aria-hidden="true">←</span>
        Danh sách bài tập
      </button>

      <div className="grid overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/70 shadow-2xl shadow-slate-950/40 xl:grid-cols-[minmax(320px,0.8fr)_minmax(0,1.2fr)]">
        <article className="border-b border-slate-800 p-5 sm:p-7 xl:border-r xl:border-b-0">
          <span className="inline-flex rounded-full border border-cyan-400/20 bg-cyan-400/5 px-2.5 py-1 text-xs font-semibold text-cyan-300">
            {topicLabels[problem.topic]}
          </span>
          <h3 className="mt-4 text-2xl font-bold tracking-tight text-white">{problem.title}</h3>
          <p className="mt-3 text-sm leading-6 text-slate-400">{problem.summary}</p>

          <div className="my-6 h-px bg-slate-800" />
          <h4 className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Đề bài</h4>
          <p className="mt-3 whitespace-pre-line text-sm leading-7 text-slate-300">{problem.description}</p>

          <div className="mt-8 rounded-xl border border-amber-300/15 bg-amber-300/5 p-4 text-xs leading-5 text-amber-100/70">
            Code được biên dịch và chạy trong Docker sandbox cô lập. Mỗi test case có giới hạn CPU, RAM, thời gian và output riêng.
          </div>
        </article>

        <div className="flex min-h-[580px] flex-col">
          <div className="flex flex-col gap-3 border-b border-slate-800 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="font-mono text-xs text-slate-500">
              solution / <span className="text-slate-300">{selectedLanguage.fileName}</span>
            </p>
            <div className="flex items-center gap-2">
              <label htmlFor="problem-language" className="sr-only">Chọn ngôn ngữ</label>
              <select
                id="problem-language"
                value={language}
                onChange={(event) => changeLanguage(event.target.value as SubmissionLanguage)}
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-100 outline-none focus:border-cyan-400"
              >
                {languageOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
              <button
                type="button"
                onClick={() => void submit()}
                disabled={submitting || !sourceCode.trim()}
                className="rounded-lg bg-cyan-400 px-4 py-2 text-sm font-bold text-slate-950 transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting ? 'Đang gửi' : 'Submit'}
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between gap-3 border-b border-slate-800/80 px-4 py-2 text-[10px] text-slate-500">
            <span>Tab: autocomplete · Enter: auto-indent · Ctrl+Z: hoàn tác</span>
            <span>Input test do hệ thống cung cấp</span>
          </div>
          <SmartCodeEditor
            key={language}
            editorId="problem-code-editor"
            language={language}
            value={sourceCode}
            onChange={setSourceCode}
          />

          {message ? (
            <p aria-live="polite" className="border-t border-slate-800 bg-slate-950 px-4 py-3 text-xs text-slate-300">{message}</p>
          ) : null}
        </div>
      </div>
    </div>
  )
}
