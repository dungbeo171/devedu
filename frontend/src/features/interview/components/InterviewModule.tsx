import { useEffect, useState } from 'react'
import { getInterviewQuestion, getInterviewQuestions } from '../api/interviewApi'
import {
  difficultyLabels,
  interviewTopicLabels,
  type InterviewDifficulty,
  type InterviewQuestionDetail,
  type InterviewQuestionSummary,
  type InterviewTopic,
} from '../types/interview'
import {
  IconChevronDown,
  IconChevronUp,
  IconSparkles,
} from '../../../shared/components/Icons'

const topics = Object.entries(interviewTopicLabels) as [InterviewTopic, string][]
const difficulties = Object.entries(difficultyLabels) as [InterviewDifficulty, string][]
const difficultyStyle: Record<InterviewDifficulty, string> = {
  EASY: 'border border-emerald-500/30 bg-emerald-500/10 text-emerald-400',
  MEDIUM: 'border border-amber-500/30 bg-amber-500/10 text-amber-400',
  HARD: 'border border-rose-500/30 bg-rose-500/10 text-rose-400',
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
    setLoading(true)
    setMessage('')
    setDetail(null)
    void getInterviewQuestions(
      topic === 'ALL' ? undefined : topic,
      difficulty === 'ALL' ? undefined : difficulty
    )
      .then((result) => {
        if (!ignore) setQuestions(result)
      })
      .catch((error: unknown) => {
        if (ignore) return
        setQuestions([])
        const code = error instanceof Error ? error.message : ''
        setMessage(
          code === 'AUTHENTICATION_REQUIRED'
            ? 'Đăng nhập bằng tài khoản sinh viên để luyện phỏng vấn.'
            : code === 'STUDENT_ROLE_REQUIRED'
            ? 'Trang Interview chỉ dành cho sinh viên.'
            : code
        )
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [topic, difficulty])

  const reveal = async (id: string) => {
    if (detail?.id === id) {
      setDetail(null)
      return
    }
    setMessage('')
    try {
      setDetail(await getInterviewQuestion(id))
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Không thể tải đáp án.')
    }
  }

  return (
    <section>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="inline-flex items-center gap-2 rounded-xl border border-teal-500/30 bg-teal-500/10 px-3 py-1 font-mono text-[11px] font-bold uppercase tracking-wider text-teal-400">
            <IconSparkles className="h-3.5 w-3.5" />
            <span>Interview preparation</span>
          </div>
          <h2 className="mt-3 text-2xl font-black tracking-tight text-white sm:text-3xl">
            Câu hỏi phỏng vấn
          </h2>
          <p className="mt-1.5 max-w-2xl text-xs text-slate-400 leading-relaxed">
            Ôn luyện kiến thức cốt lõi theo chủ đề, tự suy nghĩ trước khi mở đáp án & phần giải thích chi tiết.
          </p>
        </div>
        <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-slate-900/80 px-3 py-1.5 text-xs font-semibold text-slate-300">
          <span className="h-2 w-2 rounded-full bg-teal-500" />
          <span>{loading ? 'Đang tải...' : `${questions.length} câu hỏi`}</span>
        </div>
      </div>

      <div className="mt-6 rounded-3xl border border-white/10 bg-slate-900/80 p-5 shadow-xl shadow-black/30 backdrop-blur-md">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
          <Filter
            label="Chủ đề kiến thức"
            value={topic}
            onChange={(value) => setTopic(value as InterviewTopic | 'ALL')}
            options={topics}
          />
          <Filter
            label="Độ khó"
            value={difficulty}
            onChange={(value) => setDifficulty(value as InterviewDifficulty | 'ALL')}
            options={difficulties}
          />
          <button
            type="button"
            onClick={() => {
              setTopic('ALL')
              setDifficulty('ALL')
            }}
            className="mt-auto inline-flex items-center justify-center rounded-xl border border-white/10 bg-slate-800/80 px-4 py-2.5 text-xs font-bold text-slate-300 transition hover:bg-slate-700 hover:text-white"
          >
            Xóa bộ lọc
          </button>
        </div>
      </div>

      {message ? (
        <div className="mt-6 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-5 text-xs font-bold text-amber-300">
          {message}
        </div>
      ) : null}

      <div className="mt-6 space-y-4">
        {questions.map((item, index) => {
          const isOpen = detail?.id === item.id
          return (
            <article
              key={item.id}
              className={`rounded-3xl border transition-all ${
                isOpen
                  ? 'border-blue-500/50 bg-slate-900/95 shadow-2xl shadow-blue-500/10 ring-1 ring-blue-500/30'
                  : 'border-white/10 bg-slate-900/80 hover:border-white/20 hover:bg-slate-900'
              }`}
            >
              <button
                type="button"
                onClick={() => void reveal(item.id)}
                className="flex w-full items-start gap-4 p-6 text-left"
              >
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-xl border border-white/5 bg-slate-800 font-mono text-xs font-bold text-slate-400">
                  #{String(index + 1).padStart(2, '0')}
                </span>
                <span className="flex-1">
                  <span className="block text-base font-bold text-white leading-snug">{item.question}</span>
                  <span className="mt-3 flex flex-wrap gap-2">
                    <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-[10px] font-bold text-blue-400">
                      {interviewTopicLabels[item.topic]}
                    </span>
                    <span className={`rounded-lg px-2.5 py-0.5 text-[10px] font-bold ${difficultyStyle[item.difficulty]}`}>
                      {difficultyLabels[item.difficulty]}
                    </span>
                  </span>
                </span>
                <span className="shrink-0 inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-slate-800/80 px-3.5 py-1.5 text-xs font-bold text-blue-400 transition group-hover:text-cyan-300">
                  <span>{isOpen ? 'Ẩn đáp án' : 'Xem đáp án'}</span>
                  {isOpen ? <IconChevronUp className="h-3.5 w-3.5" /> : <IconChevronDown className="h-3.5 w-3.5" />}
                </span>
              </button>

              {isOpen && detail ? (
                <div className="border-t border-white/10 bg-slate-950/60 p-6 sm:px-10">
                  <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-5">
                    <p className="font-mono text-[10px] font-bold uppercase tracking-wider text-emerald-400">
                      💡 Đáp án gợi ý
                    </p>
                    <p className="mt-2.5 whitespace-pre-wrap text-xs leading-7 text-slate-200">
                      {detail.answer}
                    </p>
                  </div>

                  <div className="mt-4 rounded-2xl border border-blue-500/30 bg-blue-500/10 p-5">
                    <p className="font-mono text-[10px] font-bold uppercase tracking-wider text-blue-400">
                      📖 Giải thích chi tiết
                    </p>
                    <p className="mt-2 whitespace-pre-wrap text-xs leading-7 text-slate-300">
                      {detail.explanation}
                    </p>
                  </div>
                </div>
              ) : null}
            </article>
          )
        })}
      </div>

      {!loading && !message && questions.length === 0 ? (
        <div className="mt-6 rounded-3xl border border-dashed border-white/10 bg-slate-900/50 p-12 text-center text-xs font-medium text-slate-400">
          Không có câu hỏi phù hợp với bộ lọc đã chọn.
        </div>
      ) : null}

      {loading ? (
        <div className="mt-6 space-y-4">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="h-28 animate-pulse rounded-3xl border border-white/5 bg-slate-900/50" />
          ))}
        </div>
      ) : null}
    </section>
  )
}

function Filter({
  label,
  value,
  onChange,
  options,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: [string, string][]
}) {
  return (
    <label className="flex-1 block text-xs font-bold text-slate-300">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3.5 py-2.5 text-xs font-semibold text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
      >
        <option value="ALL" className="bg-slate-900 text-white">Tất cả</option>
        {options.map(([key, label]) => (
          <option key={key} value={key} className="bg-slate-900 text-white">
            {label}
          </option>
        ))}
      </select>
    </label>
  )
}
