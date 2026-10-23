import { useState, type FormEvent } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { setPendingFlash } from '../../../shared/flashMessage'
import { createContest } from '../api/contestApi'
import { contestSchedule } from '../contestSchedule'
import type { ContestDifficulty } from '../types/contest'

export function CreateContestPage() {
  const user = getStoredUser()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [durationMinutes, setDurationMinutes] = useState(120)
  const [difficulty, setDifficulty] = useState<ContestDifficulty>('BEGINNER')
  const [monitoringEnabled, setMonitoringEnabled] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  if (!user) return <AccessState message="Vui lòng đăng nhập để tạo cuộc thi." action="/login" />
  if (user.role !== 'TEACHER' && user.role !== 'ADMIN') return <AccessState message="Chỉ giáo viên và quản trị viên được tạo cuộc thi." action="/contests" />

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (saving) return
    setSaving(true); setError('')
    try {
      const schedule = contestSchedule(startsAt, endsAt, durationMinutes)
      const result = await createContest({ name: name.trim(), description: description.trim(), type: 'CUSTOM',
        difficulty, monitoringEnabled, ...schedule, problems: [] })
      setPendingFlash('Đã tạo bản nháp cuộc thi')
      window.location.assign(`/contests/${result.id}/edit`)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể tạo cuộc thi.')
    } finally { setSaving(false) }
  }

  return <section className="mx-auto max-w-3xl">
    <header className="mb-7">
      <a href="/contests" className="text-sm font-medium text-slate-500 hover:text-blue-600">← Cuộc thi</a>
      <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950">Tạo cuộc thi</h1>
      <p className="mt-2 text-sm text-slate-600">Tạo thông tin cơ bản trước. Bạn sẽ thêm bài tập và cấu hình quyền tham gia ở bước tiếp theo.</p>
    </header>
    <form onSubmit={submit} className="ui-panel space-y-5 p-5 sm:p-7">
      <fieldset disabled={saving} className="space-y-5">
        <label className="block text-sm font-semibold text-slate-800">Tên cuộc thi *
          <input autoFocus required maxLength={180} value={name} onChange={e => setName(e.target.value)} className="ui-control mt-2" placeholder="Ví dụ: DevEdu Weekly Contest #1" />
        </label>
        <label className="block text-sm font-semibold text-slate-800">Mô tả
          <textarea maxLength={5000} value={description} onChange={e => setDescription(e.target.value)} className="ui-control mt-2 min-h-28 resize-y" placeholder="Mục tiêu và nội dung chính của cuộc thi" />
        </label>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-semibold text-slate-800">Thời gian bắt đầu *<input required type="datetime-local" value={startsAt} onChange={e => setStartsAt(e.target.value)} className="ui-control mt-2" /></label>
          <label className="block text-sm font-semibold text-slate-800">Thời gian kết thúc *<input required type="datetime-local" value={endsAt} onChange={e => setEndsAt(e.target.value)} className="ui-control mt-2" /></label>
        </div>
        <p className="-mt-3 text-xs text-slate-500">Thời gian bắt đầu có thể ở quá khứ. Cuộc thi chỉ có thể công khai khi thời gian kết thúc vẫn còn hiệu lực.</p>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-semibold text-slate-800">Thời gian làm bài (phút) *<input required type="number" min={1} max={10080} value={durationMinutes} onChange={e => setDurationMinutes(Number(e.target.value))} className="ui-control mt-2" /></label>
          <label className="block text-sm font-semibold text-slate-800">Độ khó<select value={difficulty} onChange={e => setDifficulty(e.target.value as ContestDifficulty)} className="ui-control mt-2"><option value="BEGINNER">Dễ</option><option value="INTERMEDIATE">Trung bình</option><option value="ADVANCED">Khó</option></select></label>
        </div>
        <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-slate-200 p-4">
          <input type="checkbox" checked={monitoringEnabled} onChange={e => setMonitoringEnabled(e.target.checked)} className="mt-0.5 h-4 w-4 accent-blue-600" />
          <span><span className="block text-sm font-semibold text-slate-800">Chế độ giám sát</span><span className="mt-1 block text-xs leading-5 text-slate-500">Yêu cầu camera trong lượt thi chính thức. Có thể thay đổi trước khi cuộc thi bắt đầu.</span></span>
        </label>
      </fieldset>
      {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      <div className="flex flex-col-reverse justify-end gap-2 border-t border-slate-100 pt-5 sm:flex-row">
        <a href="/contests" className="ui-button-secondary justify-center">Hủy</a>
        <button disabled={saving} className="ui-button-primary justify-center">{saving ? 'Đang tạo…' : 'Tạo bản nháp'}</button>
      </div>
    </form>
  </section>
}

function AccessState({ message, action }: { message: string; action: string }) {
  return <div className="ui-panel mx-auto max-w-lg p-8 text-center"><h1 className="text-xl font-bold">Không thể tạo cuộc thi</h1><p className="mt-2 text-sm text-slate-600">{message}</p><a href={action} className="ui-button-primary mt-5">Tiếp tục</a></div>
}
