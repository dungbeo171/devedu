import { useEffect, useState } from 'react'
import { getInterviewQuestion, getInterviewQuestions } from '../api/interviewApi'
import { difficultyLabels, interviewTopicLabels, type InterviewDifficulty, type InterviewQuestionDetail, type InterviewQuestionSummary, type InterviewTopic } from '../types/interview'

const topics = Object.entries(interviewTopicLabels) as [InterviewTopic, string][]
const difficulties = Object.entries(difficultyLabels) as [InterviewDifficulty, string][]
const difficultyStyle: Record<InterviewDifficulty, string> = {
  EASY: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
  MEDIUM: 'border-amber-400/20 bg-amber-400/10 text-amber-300',
  HARD: 'border-rose-400/20 bg-rose-400/10 text-rose-300',
}

export function InterviewModule() {
  const [topic, setTopic] = useState<InterviewTopic | 'ALL'>('ALL')
  const [difficulty, setDifficulty] = useState<InterviewDifficulty | 'ALL'>('ALL')
  const [questions, setQuestions] = useState<InterviewQuestionSummary[]>([])
  const [detail, setDetail] = useState<InterviewQuestionDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let ignore = false
    setLoading(true); setMessage(''); setDetail(null)
    void getInterviewQuestions(topic === 'ALL' ? undefined : topic, difficulty === 'ALL' ? undefined : difficulty)
      .then((result) => { if (!ignore) setQuestions(result) })
      .catch((error: unknown) => {
        if (ignore) return
        setQuestions([])
        const code = error instanceof Error ? error.message : ''
        setMessage(code === 'AUTHENTICATION_REQUIRED' ? 'Đăng nhập bằng tài khoản sinh viên để luyện phỏng vấn.' :
          code === 'STUDENT_ROLE_REQUIRED' ? 'Trang Interview chỉ dành cho sinh viên.' : code)
      }).finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [topic, difficulty])

  const reveal = async (id: string) => {
    if (detail?.id === id) { setDetail(null); return }
    setMessage('')
    try { setDetail(await getInterviewQuestion(id)) }
    catch (error) { setMessage(error instanceof Error ? error.message : 'Không thể tải đáp án.') }
  }

  return <section>
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="font-mono text-xs uppercase tracking-[.2em] text-blue-600">Interview preparation</p><h2 className="mt-2 text-2xl font-bold text-slate-900 sm:text-3xl">Câu hỏi phỏng vấn</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Ôn kiến thức theo chủ đề, tự trả lời trước rồi mở đáp án và phần giải thích.</p></div><span className="text-sm text-slate-500">{loading ? 'Đang tải...' : `${questions.length} câu hỏi`}</span></div>
    <div className="mt-6 rounded-xl border border-blue-100 bg-white/50 p-4"><div className="flex flex-col gap-4 lg:flex-row lg:items-center"><Filter label="Topic" value={topic} onChange={(value) => setTopic(value as InterviewTopic | 'ALL')} options={topics}/><Filter label="Difficulty" value={difficulty} onChange={(value) => setDifficulty(value as InterviewDifficulty | 'ALL')} options={difficulties}/><button type="button" onClick={() => { setTopic('ALL'); setDifficulty('ALL') }} className="mt-auto rounded-lg border border-blue-200 px-4 py-2.5 text-xs font-semibold text-slate-600 hover:text-slate-900">Xóa bộ lọc</button></div></div>
    {message ? <p className="mt-5 rounded-xl border border-amber-400/20 bg-amber-400/5 p-4 text-sm text-amber-200">{message}</p> : null}
    <div className="mt-5 space-y-3">{questions.map((item, index) => <article key={item.id} className={`rounded-xl border transition ${detail?.id === item.id ? 'border-blue-500/40 bg-white' : 'border-blue-100 bg-white/60'}`}><button type="button" onClick={() => void reveal(item.id)} className="flex w-full items-start gap-4 p-5 text-left"><span className="font-mono text-xs text-slate-600">{String(index + 1).padStart(2, '0')}</span><span className="flex-1"><span className="block font-semibold leading-6 text-slate-900">{item.question}</span><span className="mt-3 flex flex-wrap gap-2"><span className="rounded-full border border-blue-500/20 bg-blue-600/5 px-2.5 py-1 text-[11px] text-blue-600">{interviewTopicLabels[item.topic]}</span><span className={`rounded-full border px-2.5 py-1 text-[11px] ${difficultyStyle[item.difficulty]}`}>{difficultyLabels[item.difficulty]}</span></span></span><span className="shrink-0 text-xs font-semibold text-blue-600">{detail?.id === item.id ? 'Ẩn' : 'Xem đáp án'} {detail?.id === item.id ? '↑' : '↓'}</span></button>
      {detail?.id === item.id ? <div className="border-t border-blue-100 px-5 py-6 sm:px-12"><p className="text-xs font-bold uppercase tracking-wider text-blue-600">Đáp án</p><p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-800">{detail.answer}</p><div className="mt-5 rounded-lg border border-blue-100 bg-blue-50/60 p-4"><p className="text-xs font-bold uppercase tracking-wider text-slate-500">Giải thích</p><p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-600">{detail.explanation}</p></div></div> : null}</article>)}</div>
    {!loading && !message && questions.length === 0 ? <p className="mt-5 rounded-xl border border-dashed border-blue-200 p-10 text-center text-sm text-slate-500">Không có câu hỏi phù hợp với bộ lọc.</p> : null}
    {loading ? <div className="mt-5 space-y-3">{Array.from({length:4},(_,i)=><div key={i} className="h-28 animate-pulse rounded-xl border border-blue-100 bg-white/50"/>)}</div> : null}
  </section>
}

function Filter({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: [string, string][] }) {
  return <label className="flex-1 text-xs font-medium text-slate-500">{label}<select value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-lg border border-blue-200 bg-blue-50 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500"><option value="ALL">Tất cả</option>{options.map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
}
