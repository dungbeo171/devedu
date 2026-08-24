import { useEffect, useState } from 'react'
import { completeLesson, getLesson } from '../api/courseLearningApi'
import type { Lesson } from '../types/courseLearning'

interface LessonViewerProps { lessonId: string; onBack: () => void }

export function LessonViewer({ lessonId, onBack }: LessonViewerProps) {
  const [lesson, setLesson] = useState<Lesson | null>(null)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    void getLesson(lessonId).then(setLesson).catch((error: unknown) =>
      setMessage(error instanceof Error ? error.message : 'Không thể tải lesson.'),
    ).finally(() => setLoading(false))
  }, [lessonId])

  const markComplete = async () => {
    setMessage('')
    try {
      await completeLesson(lessonId)
      setMessage('Đã đánh dấu lesson hoàn thành.')
    } catch (error) {
      const code = error instanceof Error ? error.message : ''
      setMessage(code === 'AUTHENTICATION_REQUIRED' ? 'Hãy đăng nhập bằng tài khoản sinh viên.' :
        code === 'ROLE_REQUIRED' ? 'Chức năng này chỉ dành cho sinh viên.' : code)
    }
  }

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 sm:p-7">
      <button type="button" onClick={onBack} className="text-sm font-medium text-cyan-400 hover:text-cyan-300">← Quay lại môn học</button>
      {loading ? <div className="mt-6 h-72 animate-pulse rounded-xl bg-slate-800/60" /> : null}
      {!loading && lesson ? <article className="mt-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div><p className="font-mono text-xs uppercase tracking-[0.2em] text-cyan-400">Lesson {lesson.position}</p>
            <h3 className="mt-2 text-2xl font-bold text-white">{lesson.title}</h3></div>
          <button type="button" onClick={() => void markComplete()} className="rounded-lg bg-cyan-400 px-4 py-2.5 text-sm font-bold text-slate-950 transition hover:bg-cyan-300">Đánh dấu đã học</button>
        </div>
        {lesson.videoUrl ? <div className="mt-7 overflow-hidden rounded-xl border border-slate-700 bg-black">
          <video className="aspect-video w-full" controls preload="metadata" src={lesson.videoUrl}>Trình duyệt không hỗ trợ video.</video>
          <a className="block border-t border-slate-800 px-4 py-3 text-xs text-cyan-400 hover:text-cyan-300" href={lesson.videoUrl} target="_blank" rel="noreferrer">Mở video trong tab mới ↗</a>
        </div> : <div className="mt-7 rounded-xl border border-dashed border-slate-700 p-8 text-center text-sm text-slate-500">Lesson này chưa có video.</div>}
        <div className="mt-7 whitespace-pre-wrap text-sm leading-7 text-slate-300">{lesson.content}</div>
        {message ? <p className="mt-5 rounded-lg border border-cyan-400/20 bg-cyan-400/5 px-4 py-3 text-sm text-cyan-200">{message}</p> : null}
      </article> : null}
      {!loading && !lesson && message ? <p className="mt-6 text-sm text-rose-300">{message}</p> : null}
    </div>
  )
}
