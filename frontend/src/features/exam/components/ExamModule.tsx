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

  useEffect(() => {
    if (teacher) return
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
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="inline-flex items-center gap-2 rounded-xl border border-indigo-500/30 bg-indigo-500/10 px-3 py-1 font-mono text-[11px] font-bold uppercase tracking-wider text-indigo-400">
            <IconTrophy className="h-3.5 w-3.5" />
            <span>Assessment</span>
          </div>
          <h2 className="mt-3 text-2xl font-black tracking-tight text-white sm:text-3xl">Kỳ thi lập trình</h2>
          <p className="mt-1.5 max-w-2xl text-xs text-slate-400 leading-relaxed">
            Multiple Choice được chấm tự động ngay sau khi nộp; Coding được lưu lại cho ban giám khảo chấm.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setTeacher(true)}
          className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-slate-900/80 px-4 py-2 text-xs font-bold text-slate-300 shadow-sm backdrop-blur-md transition hover:border-indigo-500/50 hover:bg-slate-800 hover:text-white"
        >
          <IconSettings className="h-3.5 w-3.5 text-indigo-400" />
          <span>Teacher Studio</span>
        </button>
      </div>

      {message ? (
        <div className="mt-6 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-5 text-xs font-bold text-amber-300">
          {message}
        </div>
      ) : null}

      <div className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {exams.map((exam) => (
          <article
            key={exam.id}
            className="group pro-card flex min-h-64 flex-col rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-xl shadow-black/30 backdrop-blur-md transition-all hover:border-indigo-500/50 hover:bg-slate-900 hover:shadow-indigo-500/10"
          >
            <div className="flex items-center justify-between">
              <span className="rounded-lg border border-indigo-500/30 bg-indigo-500/10 px-2.5 py-0.5 font-mono text-[10px] font-bold text-indigo-400">
                EXAM
              </span>
              <span className="flex items-center gap-1.5 rounded-lg border border-white/5 bg-slate-800/60 px-2.5 py-0.5 font-mono text-[10px] font-semibold text-slate-300">
                <IconClock className="h-3 w-3 text-indigo-400" />
                <span>{exam.durationMinutes} phút</span>
              </span>
            </div>

            <h3 className="mt-4 text-lg font-bold text-white transition-colors group-hover:text-indigo-400">
              {exam.title}
            </h3>
            <p className="mt-2 line-clamp-3 text-xs leading-6 text-slate-400">
              {exam.description}
            </p>

            <div className="mt-4 flex items-center gap-2 text-xs text-slate-400">
              <IconCalendar className="h-3.5 w-3.5 text-slate-500" />
              <span>Bắt đầu: {new Date(exam.scheduledAt).toLocaleString('vi-VN')}</span>
            </div>

            <button
              type="button"
              onClick={() => void join(exam.slug)}
              className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 via-blue-600 to-indigo-600 px-4 py-2.5 text-xs font-bold text-white shadow-lg shadow-indigo-600/25 ring-1 ring-white/20 transition hover:from-indigo-500 hover:to-blue-500"
            >
              <span>Tham gia kỳ thi</span>
              <IconArrowRight className="h-3.5 w-3.5" />
            </button>
          </article>
        ))}

        {exams.length === 0 && !message ? (
          <div className="col-span-full rounded-3xl border border-dashed border-white/10 bg-slate-900/50 p-12 text-center text-xs font-medium text-slate-400">
            Hiện chưa có kỳ thi nào đang mở.
          </div>
        ) : null}
      </div>
    </section>
  )
}
