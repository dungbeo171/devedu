import { useEffect, useState } from 'react'
import { getExamResult, saveExamAnswer, submitExam } from '../api/examApi'
import type { ExamAnswer, ExamResult, ExamSession } from '../types/exam'
import {
  IconArrowLeft,
  IconClock,
  IconSave,
  IconTrophy,
} from '../../../shared/components/Icons'

export function ExamWorkspace({
  initialSession,
  onBack,
}: {
  initialSession: ExamSession
  onBack: () => void
}) {
  const [session, setSession] = useState(initialSession)
  const [answers, setAnswers] = useState<Record<string, ExamAnswer>>(() =>
    Object.fromEntries(initialSession.answers.map((a) => [a.questionId, a]))
  )
  const [drafts, setDrafts] = useState<Record<string, string>>(() =>
    Object.fromEntries(initialSession.answers.map((a) => [a.questionId, a.sourceCode ?? '']))
  )
  const [result, setResult] = useState<ExamResult | null>(null)
  const [message, setMessage] = useState('')
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    if (initialSession.status === 'SUBMITTED') {
      void getExamResult(initialSession.attemptId)
        .then(setResult)
        .catch((error: unknown) =>
          setMessage(error instanceof Error ? error.message : 'Không thể tải điểm.')
        )
    }
  }, [initialSession])

  const secondsLeft = Math.max(0, Math.floor((new Date(session.expiresAt).getTime() - now) / 1000))
  const timeLabel = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`

  const persist = async (
    questionId: string,
    body: { selectedOptionIndex?: number; sourceCode?: string }
  ) => {
    setMessage('')
    try {
      const answer = await saveExamAnswer(session.attemptId, questionId, body)
      setAnswers((current) => ({ ...current, [questionId]: answer }))
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Không thể lưu câu trả lời.')
    }
  }

  const finish = async () => {
    if (!window.confirm('Nộp bài ngay? Sau khi nộp bạn không thể sửa câu trả lời.')) return
    try {
      const submitted = await submitExam(session.attemptId)
      setResult(submitted)
      setSession((current) => ({ ...current, status: 'SUBMITTED' }))
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Không thể nộp bài.')
    }
  }

  if (result) {
    return (
      <div className="ui-panel mx-auto max-w-xl p-8 text-center sm:p-12">
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-xl bg-blue-50 text-blue-600 ring-1 ring-blue-100">
          <IconTrophy className="h-8 w-8" />
        </div>
        <p className="mt-5 text-xs font-bold uppercase tracking-widest text-blue-700">
          Đã hoàn thành kỳ thi
        </p>
        <h1 className="mt-3 text-4xl font-bold text-slate-950">
          {result.automaticScore} <span className="text-xl font-bold text-slate-500">/ {result.automaticMaxScore}</span>
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          Điểm tự động từ các câu trắc nghiệm (Multiple Choice).
        </p>

        {result.pendingCodingQuestions > 0 ? (
          <div className="mt-5 flex items-center justify-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-amber-800">
            <IconClock className="h-4 w-4" /> {result.pendingCodingQuestions} câu Coding đang chờ giảng viên chấm điểm.
          </div>
        ) : null}

        <button
          type="button"
          onClick={onBack}
          className="ui-button-primary mt-8"
        >
          <IconArrowLeft className="h-3.5 w-3.5" />
          <span>Về danh sách kỳ thi</span>
        </button>
      </div>
    )
  }

  if (session.status === 'SUBMITTED') {
    return (
      <div className="ui-state text-sm font-semibold">
        {message || 'Đang tải kết quả bài thi...'}
      </div>
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
      {/* Questions list */}
      <div className="space-y-6">
        <div className="ui-panel p-6">
          <button
            type="button"
            onClick={onBack}
            className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-500 transition hover:text-blue-700"
          >
            <IconArrowLeft className="h-3.5 w-3.5" />
            <span>Danh sách kỳ thi</span>
          </button>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950">{session.exam.title}</h1>
          <p className="mt-1 text-sm leading-6 text-slate-600">{session.exam.description}</p>
        </div>

        {session.exam.questions.map((question) => (
          <article
            key={question.id}
            className="ui-panel p-6 sm:p-7"
          >
            <div className="flex items-start justify-between gap-4">
              <p className="text-sm font-bold leading-6 text-slate-950">
                <span className="mr-2 inline-block rounded-lg border border-blue-500/30 bg-blue-500/10 px-2 py-0.5 font-mono text-xs font-bold text-blue-400">
                  Câu {question.position}
                </span>
                {question.prompt}
              </p>
              <span className="shrink-0 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 font-mono text-xs font-bold text-slate-600">
                {question.points} điểm
              </span>
            </div>

            {question.type === 'MULTIPLE_CHOICE' ? (
              <div className="mt-5 space-y-2.5">
                {question.options.map((option, index) => {
                  const isChecked = answers[question.id]?.selectedOptionIndex === index
                  return (
                    <label
                      key={index}
                      className={`flex cursor-pointer items-center gap-3.5 rounded-2xl border p-4 text-xs font-semibold transition-all ${
                        isChecked
                          ? 'border-blue-500 bg-blue-50 text-blue-950 shadow-sm ring-1 ring-blue-200'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-blue-300 hover:bg-blue-50/40'
                      }`}
                    >
                      <input
                        type="radio"
                        name={question.id}
                        checked={isChecked}
                        onChange={() => void persist(question.id, { selectedOptionIndex: index })}
                        className="h-4 w-4 accent-blue-500"
                      />
                      <span>{option}</span>
                    </label>
                  )
                })}
              </div>
            ) : (
              <div className="mt-5">
                <div className="mb-2 flex items-center justify-between text-xs text-slate-400">
                  <span className="rounded-md border border-indigo-500/30 bg-indigo-500/10 px-2 py-0.5 font-mono text-[10px] font-bold text-indigo-400">
                    Ngôn ngữ: {question.codingLanguage}
                  </span>
                  <span className="text-[11px] text-slate-500">Không chạy judge tự động</span>
                </div>
                <textarea
                  value={drafts[question.id] ?? ''}
                  onChange={(e) => setDrafts((current) => ({ ...current, [question.id]: e.target.value }))}
                  rows={12}
                  spellCheck={false}
                  placeholder="Viết mã nguồn của bạn tại đây..."
                  className="w-full rounded-2xl border border-white/10 bg-slate-950 p-4 font-mono text-xs leading-6 text-slate-200 outline-none placeholder:text-slate-600 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                />
                <button
                  type="button"
                  onClick={() => void persist(question.id, { sourceCode: drafts[question.id] ?? '' })}
                  className="ui-button-secondary mt-3"
                >
                  <IconSave className="h-3.5 w-3.5" />
                  <span>Lưu mã nguồn</span>
                </button>
              </div>
            )}
          </article>
        ))}

        {message ? (
          <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
            {message}
          </div>
        ) : null}
      </div>

      {/* Sticky Exam Timer & Question Matrix */}
      <aside className="ui-panel h-fit p-6 lg:sticky lg:top-24">
        <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-slate-400">
          <IconClock className="h-3.5 w-3.5" />
          <span>Thời gian còn lại</span>
        </div>
        <p
          className={`mt-2 font-mono text-4xl font-black tracking-tight ${
            secondsLeft < 300 ? 'text-rose-500 animate-pulse' : 'text-blue-400'
          }`}
        >
          {timeLabel}
        </p>

        <div className="my-5 h-px bg-slate-200" />

        <p className="font-mono text-[11px] font-bold uppercase tracking-wider text-slate-400">Bảng câu hỏi</p>
        <div className="mt-3 grid grid-cols-5 gap-2">
          {session.exam.questions.map((q) => {
            const hasAnswer = !!answers[q.id]
            return (
              <span
                key={q.id}
                className={`grid h-8 place-items-center rounded-xl font-mono text-xs font-bold transition-all ${
                  hasAnswer
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'border border-slate-200 bg-slate-50 text-slate-500'
                }`}
              >
                {q.position}
              </span>
            )
          })}
        </div>

        <p className="mt-4 text-center text-xs font-semibold text-slate-400">
          Đã trả lời <span className="font-bold text-blue-400">{Object.keys(answers).length}</span> /{' '}
          {session.exam.questions.length} câu
        </p>

        <button
          type="button"
          onClick={() => void finish()}
          className="ui-button-primary mt-6 w-full"
        >
          Nộp bài thi
        </button>
      </aside>
    </div>
  )
}
