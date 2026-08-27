import { useState, type FormEvent } from 'react'
import { createCourse, createLesson, createTopic, setLessonVideo } from '../api/courseLearningApi'
import { IconSave } from '../../../shared/components/Icons'

type StudioAction = 'course' | 'topic' | 'lesson' | 'video'
const actions: { value: StudioAction; label: string }[] = [
  { value: 'course', label: 'Môn học' },
  { value: 'topic', label: 'Chủ đề' },
  { value: 'lesson', label: 'Lesson' },
  { value: 'video', label: 'Video URL' },
]

export function TeacherCourseStudio({ onChanged }: { onChanged: () => void }) {
  const [action, setAction] = useState<StudioAction>('course')
  const [values, setValues] = useState<Record<string, string>>({ position: '1' })
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const update = (key: string, value: string) => setValues((current) => ({ ...current, [key]: value }))

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    setMessage('')
    try {
      let createdId = ''
      if (action === 'course') {
        const result = await createCourse({
          slug: values.slug ?? '',
          title: values.title ?? '',
          description: values.description ?? '',
        })
        createdId = result.id
        update('courseId', result.id)
      }
      if (action === 'topic') {
        const result = await createTopic(values.courseId ?? '', {
          title: values.title ?? '',
          position: Number(values.position),
        })
        createdId = result.id
        update('topicId', result.id)
      }
      if (action === 'lesson') {
        const result = await createLesson(values.topicId ?? '', {
          title: values.title ?? '',
          content: values.content ?? '',
          position: Number(values.position),
        })
        createdId = result.id
        update('lessonId', result.id)
      }
      if (action === 'video') {
        const result = await setLessonVideo(values.lessonId ?? '', values.videoUrl ?? '')
        createdId = result.id
      }
      setMessage(`Đã lưu thành công. ID: ${createdId}`)
      onChanged()
    } catch (error) {
      const code = error instanceof Error ? error.message : 'Không thể lưu.'
      setMessage(
        code === 'AUTHENTICATION_REQUIRED'
          ? 'Cần JWT của giáo viên trong localStorage.'
          : code === 'ROLE_REQUIRED'
          ? 'Chỉ giáo viên hoặc admin được quản lý khóa học.'
          : code
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/40 backdrop-blur-md sm:p-8">
      <div className="flex flex-wrap gap-2">
        {actions.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => {
              setAction(item.value)
              setMessage('')
            }}
            className={`rounded-xl px-4 py-2 text-xs font-bold transition-all ${
              action === item.value
                ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-md shadow-blue-600/30 ring-1 ring-white/20'
                : 'border border-white/10 bg-slate-800/60 text-slate-300 hover:bg-slate-700 hover:text-white'
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      <form onSubmit={(event) => void submit(event)} className="mt-6 grid gap-5 sm:grid-cols-2">
        {action === 'course' ? (
          <>
            <Field
              label="Slug URL"
              value={values.slug}
              onChange={(v) => update('slug', v)}
              placeholder="lap-trinh-java"
            />
            <Field
              label="Tên môn học"
              value={values.title}
              onChange={(v) => update('title', v)}
              placeholder="Lập trình Java từ cơ bản"
            />
          </>
        ) : null}

        {action === 'topic' ? (
          <Field
            label="Course ID"
            value={values.courseId}
            onChange={(v) => update('courseId', v)}
            placeholder="UUID của Course"
          />
        ) : null}

        {action === 'lesson' ? (
          <Field
            label="Topic ID"
            value={values.topicId}
            onChange={(v) => update('topicId', v)}
            placeholder="UUID của Topic"
          />
        ) : null}

        {action === 'video' ? (
          <>
            <Field
              label="Lesson ID"
              value={values.lessonId}
              onChange={(v) => update('lessonId', v)}
              placeholder="UUID của Lesson"
            />
            <Field
              label="Video URL (HTTP/HTTPS)"
              value={values.videoUrl}
              onChange={(v) => update('videoUrl', v)}
              placeholder="https://..."
            />
          </>
        ) : null}

        {action === 'topic' || action === 'lesson' ? (
          <>
            <Field
              label="Tiêu đề"
              value={values.title}
              onChange={(v) => update('title', v)}
              placeholder="Tiêu đề chủ đề hoặc bài học"
            />
            <Field
              label="Thứ tự hiển thị"
              type="number"
              value={values.position}
              onChange={(v) => update('position', v)}
            />
          </>
        ) : null}

        {action === 'course' ? (
          <TextArea
            label="Mô tả khóa học"
            value={values.description}
            onChange={(v) => update('description', v)}
            placeholder="Giới thiệu tổng quan về khóa học..."
          />
        ) : null}

        {action === 'lesson' ? (
          <TextArea
            label="Nội dung bài học (Markdown / Text)"
            value={values.content}
            onChange={(v) => update('content', v)}
            placeholder="Nội dung chi tiết của bài học..."
          />
        ) : null}

        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={saving}
            className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-blue-600/25 ring-1 ring-white/20 transition hover:from-blue-500 hover:to-indigo-500 disabled:opacity-50"
          >
            <IconSave className="h-3.5 w-3.5" />
            <span>{saving ? 'Đang lưu nội dung...' : 'Lưu nội dung'}</span>
          </button>
        </div>
      </form>

      {message ? (
        <div className="mt-5 rounded-2xl border border-blue-500/30 bg-blue-500/10 p-4 text-xs font-semibold text-blue-300">
          {message}
        </div>
      ) : null}
    </div>
  )
}

function Field({
  label,
  value = '',
  onChange,
  placeholder,
  type = 'text',
}: {
  label: string
  value?: string
  onChange: (value: string) => void
  placeholder?: string
  type?: string
}) {
  return (
    <label className="block text-xs font-bold text-slate-300">
      {label}
      <input
        required
        type={type}
        min={type === 'number' ? 1 : undefined}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="mt-2 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3.5 py-2.5 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
      />
    </label>
  )
}

function TextArea({
  label,
  value = '',
  onChange,
  placeholder,
}: {
  label: string
  value?: string
  onChange: (value: string) => void
  placeholder?: string
}) {
  return (
    <label className="block text-xs font-bold text-slate-300 sm:col-span-2">
      {label}
      <textarea
        required
        rows={4}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="mt-2 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3.5 py-2.5 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
      />
    </label>
  )
}
