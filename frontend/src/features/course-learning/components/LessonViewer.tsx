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
    <div className="ui-panel p-6 sm:p-8">
      <button
        type="button"
        onClick={onBack}
        className="ui-button-ghost px-0 hover:bg-transparent hover:text-blue-700"
      >
        <IconArrowLeft className="h-3.5 w-3.5" />
        <span>Quay lại khóa học</span>
      </button>

      {loading ? (
        <div className="ui-skeleton mt-6 h-72 rounded-xl" />
      ) : null}

      {!loading && lesson ? (
        <article className="mt-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="inline-flex items-center gap-2 rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-xs font-bold text-blue-400">
                LESSON {lesson.position}
              </div>
              <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">{lesson.title}</h1>
            </div>
            <button
              type="button"
              onClick={() => void markComplete()}
              className="ui-button-primary"
            >
              <IconCheck className="h-4 w-4" />
              <span>Đánh dấu đã học</span>
            </button>
          </div>

          {lesson.videoUrl ? (
            <div className="mt-8 overflow-hidden rounded-xl border border-slate-300 bg-slate-950 shadow-lg">
              <video className="aspect-video w-full bg-black" controls preload="metadata" src={lesson.videoUrl}>
                Trình duyệt không hỗ trợ phát video này.
              </video>
              <div className="flex items-center justify-between border-t border-slate-800 bg-slate-900 px-4 py-2.5 text-xs text-slate-300">
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
            <div className="ui-state mt-8 min-h-32 text-sm font-medium">
              Bài học này không có video đính kèm.
            </div>
          )}

          <div className="mt-8 rounded-xl border border-slate-200 bg-slate-50 p-6">
            <h2 className="text-base font-bold text-slate-900">Nội dung bài học</h2>
            <div className="mt-3 whitespace-pre-line text-sm leading-7 text-slate-700">{lesson.content}</div>
          </div>

          {message ? (
            <div className="mt-6 rounded-xl border border-blue-200 bg-blue-50 p-4 text-sm font-semibold text-blue-800">
              {message}
            </div>
          ) : null}
        </article>
      ) : null}

      {!loading && !lesson && message ? (
        <div className="mt-6 rounded-xl border border-red-200 bg-red-50 p-5 text-sm font-semibold text-red-700">
          {message}
        </div>
      ) : null}
    </div>
  )
}
