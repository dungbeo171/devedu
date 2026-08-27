import { useEffect, useState } from 'react'
import { completeLesson, getLesson } from '../api/courseLearningApi'
import type { Lesson } from '../types/courseLearning'
import {
  IconArrowLeft,
  IconCheck,
  IconExternalLink,
  IconPlay,
} from '../../../shared/components/Icons'

interface LessonViewerProps {
  lessonId: string
  onBack: () => void
}

export function LessonViewer({ lessonId, onBack }: LessonViewerProps) {
  const [lesson, setLesson] = useState<Lesson | null>(null)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    void getLesson(lessonId).then(setLesson).catch((error: unknown) =>
      setMessage(error instanceof Error ? error.message : 'Không thể tải bài học.'),
    ).finally(() => setLoading(false))
  }, [lessonId])

  const markComplete = async () => {
    setMessage('')
    try {
      await completeLesson(lessonId)
      setMessage('Đã đánh dấu bài học hoàn thành.')
    } catch (error) {
      const code = error instanceof Error ? error.message : ''
      setMessage(code === 'AUTHENTICATION_REQUIRED' ? 'Hãy đăng nhập bằng tài khoản sinh viên.' :
        code === 'ROLE_REQUIRED' ? 'Chức năng này chỉ dành cho sinh viên.' : code)
    }
  }

  return (
    <div className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/40 backdrop-blur-md sm:p-8">
      <button
        type="button"
        onClick={onBack}
        className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-slate-800/60 px-3.5 py-1.5 text-xs font-bold text-slate-300 transition hover:border-white/20 hover:bg-slate-700/60 hover:text-white"
      >
        <IconArrowLeft className="h-3.5 w-3.5" />
        <span>Quay lại khóa học</span>
      </button>

      {loading ? (
        <div className="mt-6 h-72 animate-pulse rounded-2xl border border-white/5 bg-slate-950/60" />
      ) : null}

      {!loading && lesson ? (
        <article className="mt-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="inline-flex items-center gap-2 rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-xs font-bold text-blue-400">
                LESSON {lesson.position}
              </div>
              <h3 className="mt-2 text-2xl font-black tracking-tight text-white">{lesson.title}</h3>
            </div>
            <button
              type="button"
              onClick={() => void markComplete()}
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 px-4 py-2.5 text-xs font-bold text-white shadow-lg shadow-emerald-900/30 ring-1 ring-white/20 transition hover:from-emerald-400 hover:to-teal-500"
            >
              <IconCheck className="h-4 w-4" />
              <span>Đánh dấu đã học</span>
            </button>
          </div>

          {lesson.videoUrl ? (
            <div className="mt-8 overflow-hidden rounded-2xl border border-white/10 bg-slate-950 shadow-2xl ring-1 ring-white/5">
              <video className="aspect-video w-full bg-black" controls preload="metadata" src={lesson.videoUrl}>
                Trình duyệt không hỗ trợ phát video này.
              </video>
              <div className="flex items-center justify-between border-t border-white/10 bg-slate-900/90 px-4 py-2.5 text-xs text-slate-400">
                <div className="flex items-center gap-2">
                  <IconPlay className="h-3.5 w-3.5 text-rose-400" />
                  <span>Video bài giảng</span>
                </div>
                <a
                  className="inline-flex items-center gap-1 font-semibold text-blue-400 hover:text-cyan-300"
                  href={lesson.videoUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  <span>Mở tab mới</span>
                  <IconExternalLink className="h-3.5 w-3.5" />
                </a>
              </div>
            </div>
          ) : (
            <div className="mt-8 rounded-2xl border border-dashed border-white/10 bg-slate-950/40 p-8 text-center text-xs font-medium text-slate-500">
              Bài học này không có video đính kèm.
            </div>
          )}

          <div className="mt-8 rounded-2xl border border-white/10 bg-slate-950/60 p-6">
            <h4 className="font-mono text-[11px] font-bold uppercase tracking-wider text-slate-400">Nội dung bài học</h4>
            <div className="mt-3 whitespace-pre-line text-xs leading-7 text-slate-300">{lesson.content}</div>
          </div>

          {message ? (
            <div className="mt-6 rounded-2xl border border-blue-500/30 bg-blue-500/10 p-4 text-xs font-bold text-blue-300">
              {message}
            </div>
          ) : null}
        </article>
      ) : null}

      {!loading && !lesson && message ? (
        <div className="mt-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-5 text-xs font-bold text-rose-300">
          {message}
        </div>
      ) : null}
    </div>
  )
}
