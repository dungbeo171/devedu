import { useEffect, useState } from 'react'
import { getExams, startExam } from '../api/examApi'
import type { ExamSession, ExamSummary } from '../types/exam'
import { ExamWorkspace } from './ExamWorkspace'
import { TeacherExamStudio } from './TeacherExamStudio'
import {
  IconArrowRight,
  IconCalendar,
  IconClock,
  IconSettings,
  IconTrophy,
} from '../../../shared/components/Icons'

export function ExamModule() {
  const [exams, setExams] = useState<ExamSummary[]>([])
  const [session, setSession] = useState<ExamSession | null>(null)
  const [teacher, setTeacher] = useState(false)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (teacher) return
    setLoading(true)
    setMessage('')
    void getExams()
      .then(setExams)
      .catch((e: unknown) => {
        const code = e instanceof Error ? e.message : ''
        setMessage(
          code === 'AUTHENTICATION_REQUIRED'
            ? 'Đăng nhập bằng tài khoản sinh viên để xem kỳ thi.'
            : code === 'ROLE_REQUIRED'
            ? 'Danh sách này chỉ dành cho sinh viên.'
            : code
        )
      })
      .finally(() => setLoading(false))
  }, [teacher])

  const join = async (slug: string) => {
    setMessage('')
    try {
      setSession(await startExam(slug))
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Không thể bắt đầu kỳ thi.')
    }
  }

  if (session) return <ExamWorkspace initialSession={session} onBack={() => setSession(null)} />
  if (teacher) return <TeacherExamStudio onBack={() => setTeacher(false)} />

  return (
    <section>
      <div className="ui-page-header">
        <div>
          <div className="ui-kicker">
            <IconTrophy className="h-3.5 w-3.5" />
            <span>Đánh giá năng lực</span>
          </div>
          <h1 className="ui-page-title mt-2">Kỳ thi lập trình</h1>
          <p className="ui-page-description">
            Multiple Choice được chấm tự động ngay sau khi nộp; Coding được lưu lại cho ban giám khảo chấm.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setTeacher(true)}
          className="ui-button-secondary"
        >
          <IconSettings className="h-3.5 w-3.5 text-blue-600" />
          <span>Quản lý kỳ thi</span>
        </button>
      </div>

      {message ? (
        <div className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-5 text-sm font-semibold text-amber-800">
          {message}
        </div>
      ) : null}

      {loading ? (
        <div className="mt-6 grid grid-flow-dense gap-5 md:grid-cols-2 xl:grid-cols-3" aria-label="Đang tải kỳ thi">
          {[1, 2, 3].map((item) => <div key={item} className="ui-skeleton h-64 rounded-[14px]" />)}
        </div>
      ) : null}

      {!loading ? <div className="mt-6 grid grid-flow-dense gap-5 md:grid-cols-2 xl:grid-cols-3">
        {exams.map((exam) => (
          <article
            key={exam.id}
            className="group ui-card ui-card-interactive flex min-h-64 flex-col p-6"
          >
            <div className="flex items-center justify-between">
              <span className="ui-badge">
                Kỳ thi
              </span>
              <span className="flex items-center gap-1.5 rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                <IconClock className="h-3 w-3 text-blue-600" />
                <span>{exam.durationMinutes} phút</span>
              </span>
            </div>

            <h2 className="mt-4 text-xl font-bold text-slate-950 transition-colors group-hover:text-blue-700">
              {exam.title}
            </h2>
            <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">
              {exam.description}
            </p>

            <div className="mt-4 flex items-center gap-2 text-xs text-slate-500">
              <IconCalendar className="h-3.5 w-3.5 text-slate-500" />
              <span>Bắt đầu: {new Date(exam.scheduledAt).toLocaleString('vi-VN')}</span>
            </div>

            <button
              type="button"
              onClick={() => void join(exam.slug)}
              className="ui-button-primary mt-auto w-full"
            >
              <span>Tham gia kỳ thi</span>
              <IconArrowRight className="h-3.5 w-3.5" />
            </button>
          </article>
        ))}

        {exams.length === 0 && !message ? (
          <div className="ui-state col-span-full text-sm font-medium">
            Hiện chưa có kỳ thi nào đang mở.
          </div>
        ) : null}
      </div> : null}
    </section>
  )
}
