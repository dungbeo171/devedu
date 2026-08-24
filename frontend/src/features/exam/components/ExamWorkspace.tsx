import { useEffect, useState } from 'react'
import { getExamResult, saveExamAnswer, submitExam } from '../api/examApi'
import type { ExamAnswer, ExamResult, ExamSession } from '../types/exam'

export function ExamWorkspace({ initialSession, onBack }: { initialSession: ExamSession; onBack: () => void }) {
  const [session, setSession] = useState(initialSession)
  const [answers, setAnswers] = useState<Record<string, ExamAnswer>>(() => Object.fromEntries(initialSession.answers.map((a) => [a.questionId, a])))
  const [drafts, setDrafts] = useState<Record<string, string>>(() => Object.fromEntries(initialSession.answers.map((a) => [a.questionId, a.sourceCode ?? ''])))
  const [result, setResult] = useState<ExamResult | null>(null)
  const [message, setMessage] = useState('')
  const [now, setNow] = useState(Date.now())
  useEffect(() => { const timer = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(timer) }, [])
  useEffect(() => {
    if (initialSession.status === 'SUBMITTED') {
      void getExamResult(initialSession.attemptId).then(setResult).catch((error: unknown) =>
        setMessage(error instanceof Error ? error.message : 'Không thể tải điểm.'),
      )
    }
  }, [initialSession])
  const secondsLeft = Math.max(0, Math.floor((new Date(session.expiresAt).getTime() - now) / 1000))
  const timeLabel = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`

  const persist = async (questionId: string, body: { selectedOptionIndex?: number; sourceCode?: string }) => {
    setMessage('')
    try { const answer = await saveExamAnswer(session.attemptId, questionId, body); setAnswers((current) => ({ ...current, [questionId]: answer })) }
    catch (error) { setMessage(error instanceof Error ? error.message : 'Không thể lưu câu trả lời.') }
  }
  const finish = async () => {
    if (!window.confirm('Nộp bài ngay? Sau khi nộp bạn không thể sửa câu trả lời.')) return
    try { const submitted = await submitExam(session.attemptId); setResult(submitted); setSession((current) => ({ ...current, status: 'SUBMITTED' })) }
    catch (error) { setMessage(error instanceof Error ? error.message : 'Không thể nộp bài.') }
  }

  if (result) return <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 p-7 text-center"><p className="font-mono text-xs uppercase tracking-[.2em] text-emerald-300">Đã nộp bài</p><h3 className="mt-4 text-3xl font-black text-white">{result.automaticScore} / {result.automaticMaxScore}</h3><p className="mt-2 text-sm text-slate-400">Điểm tự động từ câu Multiple Choice.</p>{result.pendingCodingQuestions > 0 ? <p className="mt-3 text-sm text-amber-300">{result.pendingCodingQuestions} câu Coding đang chờ chấm.</p> : null}<button onClick={onBack} className="mt-7 rounded-lg border border-slate-700 px-4 py-2 text-sm text-white">Về danh sách kỳ thi</button></div>
  if (session.status === 'SUBMITTED') return <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-8 text-center text-sm text-slate-400">{message || 'Đang tải kết quả...'}</div>

  return <div className="grid gap-5 lg:grid-cols-[1fr_260px]">
    <div className="space-y-5"><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5"><button onClick={onBack} className="text-sm text-cyan-400">← Danh sách kỳ thi</button><h3 className="mt-4 text-2xl font-bold text-white">{session.exam.title}</h3><p className="mt-2 text-sm text-slate-400">{session.exam.description}</p></div>
      {session.exam.questions.map((question) => <article key={question.id} className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 sm:p-6"><div className="flex items-start justify-between gap-4"><p className="text-sm font-semibold leading-6 text-white"><span className="mr-2 font-mono text-cyan-400">{question.position}.</span>{question.prompt}</p><span className="shrink-0 text-xs text-slate-500">{question.points} điểm</span></div>
        {question.type === 'MULTIPLE_CHOICE' ? <div className="mt-5 space-y-2">{question.options.map((option, index) => <label key={index} className={`flex cursor-pointer gap-3 rounded-lg border p-3 text-sm transition ${answers[question.id]?.selectedOptionIndex === index ? 'border-cyan-400 bg-cyan-400/10 text-white' : 'border-slate-800 text-slate-400 hover:border-slate-700'}`}><input type="radio" name={question.id} checked={answers[question.id]?.selectedOptionIndex === index} onChange={() => void persist(question.id, { selectedOptionIndex: index })} className="accent-cyan-400" /><span>{option}</span></label>)}</div> : <div className="mt-5"><div className="mb-2 flex justify-between text-xs text-slate-500"><span>{question.codingLanguage}</span><span>Không chạy judge tự động</span></div><textarea value={drafts[question.id] ?? ''} onChange={(e) => setDrafts((current) => ({ ...current, [question.id]: e.target.value }))} rows={12} spellCheck={false} className="w-full rounded-xl border border-slate-700 bg-slate-950 p-4 font-mono text-sm leading-6 text-slate-200 outline-none focus:border-cyan-400" /><button onClick={() => void persist(question.id, { sourceCode: drafts[question.id] ?? '' })} className="mt-3 rounded-lg border border-cyan-400/40 px-4 py-2 text-xs font-bold text-cyan-300">Lưu code</button></div>}
      </article>)}{message ? <p className="rounded-lg border border-rose-400/20 bg-rose-400/5 p-4 text-sm text-rose-200">{message}</p> : null}</div>
    <aside className="h-fit rounded-2xl border border-slate-800 bg-slate-900/80 p-5 lg:sticky lg:top-5"><p className="text-xs uppercase tracking-wider text-slate-500">Thời gian còn lại</p><p className={`mt-2 font-mono text-3xl font-bold ${secondsLeft < 300 ? 'text-rose-400' : 'text-cyan-300'}`}>{timeLabel}</p><div className="mt-5 grid grid-cols-5 gap-2">{session.exam.questions.map((q) => <span key={q.id} className={`grid h-8 place-items-center rounded text-xs ${answers[q.id] ? 'bg-cyan-400 text-slate-950' : 'bg-slate-800 text-slate-500'}`}>{q.position}</span>)}</div><p className="mt-4 text-xs text-slate-500">Đã lưu {Object.keys(answers).length}/{session.exam.questions.length} câu</p><button onClick={() => void finish()} className="mt-5 w-full rounded-lg bg-cyan-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-cyan-300">Nộp bài</button></aside>
  </div>
}
