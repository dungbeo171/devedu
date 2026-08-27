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
      <div className="mx-auto max-w-xl rounded-3xl border border-white/10 bg-slate-900/90 p-8 text-center shadow-2xl shadow-black/60 backdrop-blur-xl ring-1 ring-white/5 sm:p-12">
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-gradient-to-br from-emerald-500/20 to-teal-500/20 text-emerald-400 ring-1 ring-emerald-500/30">
          <IconTrophy className="h-8 w-8" />
        </div>
        <p className="mt-5 font-mono text-xs font-bold uppercase tracking-widest text-emerald-400">
          Đã hoàn thành kỳ thi
        </p>
        <h3 className="mt-3 text-4xl font-black text-white">
          {result.automaticScore} <span className="text-xl font-bold text-slate-500">/ {result.automaticMaxScore}</span>
        </h3>
        <p className="mt-2 text-xs text-slate-400">
          Điểm tự động từ các câu trắc nghiệm (Multiple Choice).
        </p>

        {result.pendingCodingQuestions > 0 ? (
          <div className="mt-5 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-xs font-bold text-amber-300">
            ⏳ {result.pendingCodingQuestions} câu Coding đang chờ giảng viên chấm điểm.
          </div>
        ) : null}

        <button
          type="button"
          onClick={onBack}
          className="mt-8 inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-blue-600/25 transition hover:from-blue-500 hover:to-indigo-500"
        >
          <IconArrowLeft className="h-3.5 w-3.5" />
          <span>Về danh sách kỳ thi</span>
        </button>
      </div>
    )
  }

  if (session.status === 'SUBMITTED') {
    return (
      <div className="rounded-3xl border border-white/10 bg-slate-900/80 p-12 text-center text-xs font-semibold text-slate-400">
        {message || 'Đang tải kết quả bài thi...'}
      </div>
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
      {/* Questions list */}
      <div className="space-y-6">
        <div className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-xl shadow-black/30 backdrop-blur-md">
          <button
            type="button"
            onClick={onBack}
            className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-400 transition hover:text-white"
          >
            <IconArrowLeft className="h-3.5 w-3.5" />
            <span>Danh sách kỳ thi</span>
          </button>
          <h3 className="mt-3 text-2xl font-black tracking-tight text-white">{session.exam.title}</h3>
          <p className="mt-1 text-xs text-slate-400">{session.exam.description}</p>
        </div>

        {session.exam.questions.map((question) => (
          <article
            key={question.id}
            className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-xl shadow-black/30 backdrop-blur-md sm:p-7"
          >
            <div className="flex items-start justify-between gap-4">
              <p className="text-sm font-bold leading-6 text-white">
                <span className="mr-2 inline-block rounded-lg border border-blue-500/30 bg-blue-500/10 px-2 py-0.5 font-mono text-xs font-bold text-blue-400">
                  Câu {question.position}
                </span>
                {question.prompt}
              </p>
              <span className="shrink-0 rounded-lg border border-white/5 bg-slate-800/60 px-2.5 py-1 font-mono text-xs font-bold text-slate-400">
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
                          ? 'border-blue-500 bg-blue-500/15 text-white shadow-md shadow-blue-500/10 ring-1 ring-blue-500/30'
                          : 'border-white/10 bg-slate-950/40 text-slate-300 hover:border-white/20 hover:bg-slate-950/80'
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
                  className="mt-3 inline-flex items-center gap-1.5 rounded-xl border border-blue-500/30 bg-blue-500/10 px-4 py-2 text-xs font-bold text-blue-400 transition hover:bg-blue-500/20"
                >
                  <IconSave className="h-3.5 w-3.5" />
                  <span>Lưu mã nguồn</span>
                </button>
              </div>
            )}
          </article>
        ))}

        {message ? (
          <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4 text-xs font-bold text-rose-300">
            {message}
          </div>
        ) : null}
      </div>

      {/* Sticky Exam Timer & Question Matrix */}
      <aside className="h-fit rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/40 backdrop-blur-xl ring-1 ring-white/5 lg:sticky lg:top-24">
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

        <div className="my-5 h-px bg-white/10" />

        <p className="font-mono text-[11px] font-bold uppercase tracking-wider text-slate-400">Bảng câu hỏi</p>
        <div className="mt-3 grid grid-cols-5 gap-2">
          {session.exam.questions.map((q) => {
            const hasAnswer = !!answers[q.id]
            return (
              <span
                key={q.id}
                className={`grid h-8 place-items-center rounded-xl font-mono text-xs font-bold transition-all ${
                  hasAnswer
                    ? 'bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-md shadow-blue-600/30 ring-1 ring-white/20'
                    : 'border border-white/10 bg-slate-800/60 text-slate-400'
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
          className="mt-6 w-full rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 px-4 py-3 text-xs font-bold text-white shadow-lg shadow-blue-600/25 ring-1 ring-white/20 transition hover:from-blue-500 hover:to-indigo-500"
        >
          Nộp bài thi
        </button>
      </aside>
    </div>
  )
}
