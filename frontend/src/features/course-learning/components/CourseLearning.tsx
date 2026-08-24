import { useCallback, useEffect, useState } from 'react'
import { getCourse, getCourses } from '../api/courseLearningApi'
import type { CourseDetail, CourseSummary } from '../types/courseLearning'
import { LessonViewer } from './LessonViewer'
import { TeacherCourseStudio } from './TeacherCourseStudio'

export function CourseLearning() {
  const [courses, setCourses] = useState<CourseSummary[]>([])
  const [selected, setSelected] = useState<CourseDetail | null>(null)
  const [lessonId, setLessonId] = useState<string | null>(null)
  const [studio, setStudio] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const load = useCallback(() => { setError(''); void getCourses().then(setCourses).catch((e: unknown) => setError(e instanceof Error ? e.message : 'Không thể tải môn học.')) }, [])
  useEffect(load, [load, refreshKey])

  const openCourse = async (slug: string) => {
    setError('')
    try { setSelected(await getCourse(slug)); setLessonId(null) }
    catch (e) { setError(e instanceof Error ? e.message : 'Không thể tải môn học.') }
  }

  if (lessonId) return <LessonViewer lessonId={lessonId} onBack={() => setLessonId(null)} />

  return <section>
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><p className="font-mono text-xs uppercase tracking-[0.2em] text-cyan-400">Learning paths</p><h2 className="mt-2 text-2xl font-bold text-white sm:text-3xl">Course & Lesson</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">Học theo từng chủ đề, xem video và lưu lại tiến độ lesson của bạn.</p></div>
      <button type="button" onClick={() => setStudio((value) => !value)} className="rounded-lg border border-slate-700 px-4 py-2.5 text-sm font-semibold text-slate-300 hover:border-cyan-400/50 hover:text-cyan-300">{studio ? 'Xem catalog' : 'Teacher Studio'}</button>
    </div>
    {error ? <p className="mt-5 rounded-xl border border-rose-400/20 bg-rose-400/5 p-4 text-sm text-rose-200">{error}</p> : null}
    {studio ? <div className="mt-6"><TeacherCourseStudio onChanged={() => setRefreshKey((key) => key + 1)} /></div> :
      selected ? <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/60 p-5 sm:p-7">
        <button type="button" onClick={() => setSelected(null)} className="text-sm text-cyan-400">← Tất cả môn học</button>
        <h3 className="mt-5 text-2xl font-bold text-white">{selected.title}</h3><p className="mt-2 text-sm leading-6 text-slate-400">{selected.description}</p>
        <div className="mt-7 space-y-4">{selected.topics.map((topic) => <div key={topic.id} className="rounded-xl border border-slate-800 bg-slate-950/50 p-4"><div className="flex items-center gap-3"><span className="font-mono text-xs text-cyan-400">{String(topic.position).padStart(2, '0')}</span><h4 className="font-semibold text-white">{topic.title}</h4></div><div className="mt-3 space-y-2">{topic.lessons.map((lesson) => <button key={lesson.id} type="button" onClick={() => setLessonId(lesson.id)} className="flex w-full items-center justify-between rounded-lg border border-transparent bg-slate-900 px-4 py-3 text-left text-sm text-slate-300 hover:border-cyan-400/30 hover:text-white"><span>{lesson.position}. {lesson.title}</span><span className="text-xs text-slate-500">{lesson.hasVideo ? 'Video · ' : ''}Mở →</span></button>)}{topic.lessons.length === 0 ? <p className="text-xs text-slate-600">Chưa có lesson.</p> : null}</div></div>)}{selected.topics.length === 0 ? <p className="text-sm text-slate-500">Môn học chưa có chủ đề.</p> : null}</div>
      </div> : <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">{courses.map((course, index) => <button key={course.id} type="button" onClick={() => void openCourse(course.slug)} className="group min-h-52 rounded-2xl border border-slate-800 bg-slate-900/60 p-5 text-left transition hover:-translate-y-0.5 hover:border-cyan-400/40"><div className="flex justify-between"><span className="rounded-full bg-cyan-400/10 px-2.5 py-1 font-mono text-[11px] text-cyan-300">COURSE</span><span className="font-mono text-xs text-slate-700">{String(index + 1).padStart(2, '0')}</span></div><h3 className="mt-5 text-lg font-bold text-white group-hover:text-cyan-200">{course.title}</h3><p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-500">{course.description}</p><span className="mt-5 block text-xs font-semibold text-cyan-400">Xem nội dung →</span></button>)}{courses.length === 0 && !error ? <div className="col-span-full rounded-xl border border-dashed border-slate-700 p-10 text-center text-sm text-slate-500">Chưa có môn học. Giáo viên có thể tạo nội dung trong Teacher Studio.</div> : null}</div>}
  </section>
}
