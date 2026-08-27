import { useCallback, useEffect, useState } from 'react'
import { getCourse, getCourses } from '../api/courseLearningApi'
import type { CourseDetail, CourseSummary } from '../types/courseLearning'
import { LessonViewer } from './LessonViewer'
import { TeacherCourseStudio } from './TeacherCourseStudio'
import {
  IconArrowLeft,
  IconArrowRight,
  IconBookOpen,
  IconPlay,
  IconSettings,
} from '../../../shared/components/Icons'

export function CourseLearning() {
  const [courses, setCourses] = useState<CourseSummary[]>([])
  const [selected, setSelected] = useState<CourseDetail | null>(null)
  const [lessonId, setLessonId] = useState<string | null>(null)
  const [studio, setStudio] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  const load = useCallback(() => {
    setError('')
    void getCourses().then(setCourses).catch((e: unknown) =>
      setError(e instanceof Error ? e.message : 'Không thể tải môn học.')
    )
  }, [])

  useEffect(load, [load, refreshKey])

  const openCourse = async (slug: string) => {
    setError('')
    try {
      setSelected(await getCourse(slug))
      setLessonId(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể tải môn học.')
    }
  }

  if (lessonId) return <LessonViewer lessonId={lessonId} onBack={() => setLessonId(null)} />

  return (
    <section>
      {/* Header section */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="inline-flex items-center gap-2 rounded-xl border border-blue-500/30 bg-blue-500/10 px-3 py-1 font-mono text-[11px] font-bold uppercase tracking-wider text-blue-400">
            <IconBookOpen className="h-3.5 w-3.5" />
            <span>Learning paths</span>
          </div>
          <h2 className="mt-3 text-2xl font-black tracking-tight text-white sm:text-3xl">Khóa học & Bài giảng</h2>
          <p className="mt-1.5 max-w-2xl text-xs text-slate-400 leading-relaxed">
            Học theo lộ trình chuyên sâu từng chủ đề, xem video minh họa và lưu lại tiến độ học tập.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setStudio((value) => !value)}
          className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-slate-900/80 px-4 py-2 text-xs font-bold text-slate-300 shadow-sm backdrop-blur-md transition hover:border-blue-500/50 hover:bg-slate-800 hover:text-white"
        >
          <IconSettings className="h-3.5 w-3.5 text-blue-400" />
          <span>{studio ? 'Xem danh mục khóa học' : 'Teacher Studio'}</span>
        </button>
      </div>

      {error ? (
        <div className="mt-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-5 text-xs font-bold text-rose-300">
          {error}
        </div>
      ) : null}

      {studio ? (
        <div className="mt-6">
          <TeacherCourseStudio onChanged={() => setRefreshKey((key) => key + 1)} />
        </div>
      ) : selected ? (
        <div className="mt-6 rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/40 backdrop-blur-md sm:p-8">
          <button
            type="button"
            onClick={() => setSelected(null)}
            className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-slate-800/60 px-3.5 py-1.5 text-xs font-bold text-slate-300 transition hover:border-white/20 hover:bg-slate-700/60 hover:text-white"
          >
            <IconArrowLeft className="h-3.5 w-3.5" />
            <span>Tất cả khóa học</span>
          </button>
          <div className="mt-5">
            <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-[10px] font-bold text-blue-400">
              COURSE
            </span>
            <h3 className="mt-3 text-2xl font-black tracking-tight text-white">{selected.title}</h3>
            <p className="mt-2 text-xs leading-6 text-slate-400">{selected.description}</p>
          </div>

          <div className="mt-8 space-y-4">
            {selected.topics.map((topic) => (
              <div key={topic.id} className="rounded-2xl border border-white/10 bg-slate-950/60 p-5">
                <div className="flex items-center gap-3">
                  <span className="grid h-8 w-8 place-items-center rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 font-mono text-xs font-black text-white shadow-md shadow-blue-500/25 ring-1 ring-white/20">
                    {String(topic.position).padStart(2, '0')}
                  </span>
                  <h4 className="text-base font-bold text-white">{topic.title}</h4>
                </div>
                <div className="mt-4 space-y-2.5">
                  {topic.lessons.map((lesson) => (
                    <button
                      key={lesson.id}
                      type="button"
                      onClick={() => setLessonId(lesson.id)}
                      className="group pro-card flex w-full items-center justify-between rounded-xl border border-white/10 bg-slate-900/90 px-4 py-3.5 text-left shadow-sm transition hover:border-blue-500/50 hover:bg-slate-800"
                    >
                      <div className="flex items-center gap-3">
                        <span className="grid h-6 w-6 place-items-center rounded-full bg-slate-800 font-mono text-[10px] font-bold text-slate-400 group-hover:bg-blue-500/20 group-hover:text-blue-300">
                          {lesson.position}
                        </span>
                        <span className="text-xs font-semibold text-slate-200 group-hover:text-white">
                          {lesson.title}
                        </span>
                      </div>
                      <div className="flex items-center gap-2.5">
                        {lesson.hasVideo ? (
                          <span className="flex items-center gap-1 rounded-md border border-rose-500/30 bg-rose-500/10 px-2 py-0.5 font-mono text-[10px] font-bold text-rose-400">
                            <IconPlay className="h-2.5 w-2.5" /> Video
                          </span>
                        ) : null}
                        <span className="flex items-center gap-1 text-xs font-bold text-blue-400 opacity-80 group-hover:opacity-100">
                          <span>Học bài</span>
                          <IconArrowRight className="h-3 w-3 transition-transform group-hover:translate-x-0.5" />
                        </span>
                      </div>
                    </button>
                  ))}
                  {topic.lessons.length === 0 ? (
                    <p className="py-2 text-xs text-slate-500">Chưa có bài học nào trong chủ đề này.</p>
                  ) : null}
                </div>
              </div>
            ))}
            {selected.topics.length === 0 ? (
              <p className="py-8 text-center text-xs font-medium text-slate-400">Khóa học chưa có chủ đề.</p>
            ) : null}
          </div>
        </div>
      ) : (
        <div className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {courses.map((course, index) => (
            <button
              key={course.id}
              type="button"
              onClick={() => void openCourse(course.slug)}
              className="group pro-card flex min-h-60 flex-col rounded-3xl border border-white/10 bg-slate-900/80 p-6 text-left shadow-xl shadow-black/30 backdrop-blur-md transition-all hover:border-blue-500/50 hover:bg-slate-900 hover:shadow-blue-500/10 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
            >
              <div className="flex items-center justify-between">
                <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-[10px] font-bold text-blue-400">
                  COURSE
                </span>
                <span className="font-mono text-xs font-bold text-slate-500 group-hover:text-blue-400">
                  #{String(index + 1).padStart(2, '0')}
                </span>
              </div>
              <h3 className="mt-4 text-lg font-bold text-white transition-colors group-hover:text-blue-400">
                {course.title}
              </h3>
              <p className="mt-2 line-clamp-3 text-xs leading-6 text-slate-400">
                {course.description}
              </p>
              <div className="mt-auto pt-5">
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-blue-400 group-hover:text-cyan-300">
                  <span>Khám phá khóa học</span>
                  <IconArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-1" />
                </span>
              </div>
            </button>
          ))}
          {courses.length === 0 && !error ? (
            <div className="col-span-full rounded-3xl border border-dashed border-white/10 bg-slate-900/50 p-12 text-center text-xs font-medium text-slate-400">
              Chưa có khóa học. Giáo viên có thể tạo nội dung trong Teacher Studio.
            </div>
          ) : null}
        </div>
      )}
    </section>
  )
}
