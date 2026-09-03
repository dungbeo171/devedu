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
  IconBookOpen,
  IconCheckCircle,
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
      <div className="ui-page-header">
        <div>
          <div className="ui-kicker">
            <IconSparkles className="h-3.5 w-3.5" />
            <span>Ôn tập kiến thức</span>
          </div>
          <h1 className="ui-page-title mt-2">
            Câu hỏi phỏng vấn
          </h1>
          <p className="ui-page-description">
            Ôn luyện kiến thức cốt lõi theo chủ đề, tự suy nghĩ trước khi mở đáp án & phần giải thích chi tiết.
          </p>
        </div>
        <div className="ui-badge">
          <span className="h-2 w-2 rounded-full bg-blue-600" />
          <span>{loading ? 'Đang tải...' : `${questions.length} câu hỏi`}</span>
        </div>
      </div>

      <div className="ui-panel mt-6 p-5">
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
            className="ui-button-secondary mt-auto"
          >
            Xóa bộ lọc
          </button>
        </div>
      </div>

      {message ? (
        <div className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-5 text-sm font-semibold text-amber-800">
          {message}
        </div>
      ) : null}

      <div className="mt-6 space-y-4">
        {questions.map((item, index) => {
          const isOpen = detail?.id === item.id
          return (
            <article
              key={item.id}
              className={`overflow-hidden rounded-[14px] border bg-white transition-all ${
                isOpen
                  ? 'border-blue-400 shadow-[0_14px_30px_-22px_rgba(13,110,253,.7)] ring-1 ring-blue-100'
                  : 'border-slate-200 hover:border-blue-300 hover:shadow-sm'
              }`}
            >
              <button
                type="button"
                onClick={() => void reveal(item.id)}
                className="flex w-full items-start gap-4 p-6 text-left"
              >
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-slate-200 bg-slate-50 font-mono text-xs font-bold text-slate-500">
                  #{String(index + 1).padStart(2, '0')}
                </span>
                <span className="flex-1">
                  <span className="block text-base font-bold leading-snug text-slate-950">{item.question}</span>
                  <span className="mt-3 flex flex-wrap gap-2">
                    <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-[10px] font-bold text-blue-400">
                      {interviewTopicLabels[item.topic]}
                    </span>
                    <span className={`rounded-lg px-2.5 py-0.5 text-[10px] font-bold ${difficultyStyle[item.difficulty]}`}>
                      {difficultyLabels[item.difficulty]}
                    </span>
                  </span>
                </span>
                <span className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-blue-200 bg-blue-50 px-3.5 py-1.5 text-xs font-bold text-blue-700 transition hover:bg-blue-100">
                  <span>{isOpen ? 'Ẩn đáp án' : 'Xem đáp án'}</span>
                  {isOpen ? <IconChevronUp className="h-3.5 w-3.5" /> : <IconChevronDown className="h-3.5 w-3.5" />}
                </span>
              </button>

              {isOpen && detail ? (
                <div className="border-t border-slate-200 bg-slate-50 p-6 sm:px-10">
                  <div className="rounded-xl border border-blue-200 bg-white p-5">
                    <p className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-blue-700">
                      <IconCheckCircle className="h-4 w-4" /> Đáp án gợi ý
                    </p>
                    <p className="mt-2.5 whitespace-pre-wrap text-sm leading-7 text-slate-700">
                      {detail.answer}
                    </p>
                  </div>

                  <div className="mt-4 rounded-xl border border-slate-200 bg-white p-5">
                    <p className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-700">
                      <IconBookOpen className="h-4 w-4 text-blue-600" /> Giải thích chi tiết
                    </p>
                    <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-700">
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
        <div className="ui-state mt-6 text-sm font-medium">
          Không có câu hỏi phù hợp với bộ lọc đã chọn.
        </div>
      ) : null}

      {loading ? (
        <div className="mt-6 space-y-4">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="ui-skeleton h-28 rounded-[14px] border border-slate-200" />
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
    <label className="block flex-1 text-sm font-semibold text-slate-700">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="ui-control mt-2 font-semibold"
      >
        <option value="ALL" className="bg-white text-slate-900">Tất cả</option>
        {options.map(([key, label]) => (
          <option key={key} value={key} className="bg-white text-slate-900">
            {label}
          </option>
        ))}
      </select>
    </label>
  )
}
