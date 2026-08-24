import { useState, type FormEvent } from 'react'
import { createCourse, createLesson, createTopic, setLessonVideo } from '../api/courseLearningApi'

type StudioAction = 'course' | 'topic' | 'lesson' | 'video'
const actions: { value: StudioAction; label: string }[] = [
  { value: 'course', label: 'Môn học' }, { value: 'topic', label: 'Chủ đề' },
  { value: 'lesson', label: 'Lesson' }, { value: 'video', label: 'Video URL' },
]

export function TeacherCourseStudio({ onChanged }: { onChanged: () => void }) {
  const [action, setAction] = useState<StudioAction>('course')
  const [values, setValues] = useState<Record<string, string>>({ position: '1' })
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const update = (key: string, value: string) => setValues((current) => ({ ...current, [key]: value }))

  const submit = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setMessage('')
    try {
      let createdId = ''
      if (action === 'course') {
        const result = await createCourse({ slug: values.slug ?? '', title: values.title ?? '', description: values.description ?? '' })
        createdId = result.id; update('courseId', result.id)
      }
      if (action === 'topic') {
        const result = await createTopic(values.courseId ?? '', { title: values.title ?? '', position: Number(values.position) })
        createdId = result.id; update('topicId', result.id)
      }
      if (action === 'lesson') {
        const result = await createLesson(values.topicId ?? '', { title: values.title ?? '', content: values.content ?? '', position: Number(values.position) })
        createdId = result.id; update('lessonId', result.id)
      }
      if (action === 'video') {
        const result = await setLessonVideo(values.lessonId ?? '', values.videoUrl ?? '')
        createdId = result.id
      }
      setMessage(`Đã lưu thành công. ID: ${createdId}`)
      onChanged()
    } catch (error) {
      const code = error instanceof Error ? error.message : 'Không thể lưu.'
      setMessage(code === 'AUTHENTICATION_REQUIRED' ? 'Cần JWT của giáo viên trong localStorage.' : code === 'ROLE_REQUIRED' ? 'Chỉ giáo viên hoặc admin được quản lý khóa học.' : code)
    } finally { setSaving(false) }
  }

  return <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 sm:p-7">
    <div className="flex flex-wrap gap-2">{actions.map((item) => <button key={item.value} type="button" onClick={() => { setAction(item.value); setMessage('') }} className={`rounded-full border px-3 py-2 text-xs font-semibold ${action === item.value ? 'border-cyan-400 bg-cyan-400 text-slate-950' : 'border-slate-700 text-slate-400'}`}>{item.label}</button>)}</div>
    <form onSubmit={(event) => void submit(event)} className="mt-6 grid gap-4 sm:grid-cols-2">
      {action === 'course' ? <><Field label="Slug" value={values.slug} onChange={(v) => update('slug', v)} placeholder="lap-trinh-java" /><Field label="Tên môn học" value={values.title} onChange={(v) => update('title', v)} /></> : null}
      {action === 'topic' ? <Field label="Course ID" value={values.courseId} onChange={(v) => update('courseId', v)} /> : null}
      {action === 'lesson' ? <Field label="Topic ID" value={values.topicId} onChange={(v) => update('topicId', v)} /> : null}
      {action === 'video' ? <><Field label="Lesson ID" value={values.lessonId} onChange={(v) => update('lessonId', v)} /><Field label="Video URL (HTTP/HTTPS)" value={values.videoUrl} onChange={(v) => update('videoUrl', v)} /></> : null}
      {action === 'topic' || action === 'lesson' ? <><Field label="Tiêu đề" value={values.title} onChange={(v) => update('title', v)} /><Field label="Thứ tự" type="number" value={values.position} onChange={(v) => update('position', v)} /></> : null}
      {action === 'course' ? <TextArea label="Mô tả" value={values.description} onChange={(v) => update('description', v)} /> : null}
      {action === 'lesson' ? <TextArea label="Nội dung lesson" value={values.content} onChange={(v) => update('content', v)} /> : null}
      <div className="sm:col-span-2"><button disabled={saving} className="rounded-lg bg-cyan-400 px-5 py-2.5 text-sm font-bold text-slate-950 disabled:opacity-50">{saving ? 'Đang lưu...' : 'Lưu nội dung'}</button></div>
    </form>
    {message ? <p className="mt-4 text-sm text-slate-300">{message}</p> : null}
  </div>
}

function Field({ label, value = '', onChange, placeholder, type = 'text' }: { label: string; value?: string; onChange: (value: string) => void; placeholder?: string; type?: string }) {
  return <label className="text-xs font-medium text-slate-400">{label}<input required type={type} min={type === 'number' ? 1 : undefined} value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none focus:border-cyan-400" /></label>
}
function TextArea({ label, value = '', onChange }: { label: string; value?: string; onChange: (value: string) => void }) {
  return <label className="text-xs font-medium text-slate-400 sm:col-span-2">{label}<textarea required rows={4} value={value} onChange={(e) => onChange(e.target.value)} className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none focus:border-cyan-400" /></label>
}
