import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { setPendingFlash } from '../../../shared/flashMessage'
import { IconArrowLeft, IconSave } from '../../../shared/components/Icons'
import {
  getLearningDocument,
  getLearningDocumentEditorAccess,
  updateLearningDocument,
} from '../api/learningDocumentsApi'
import type { UpdateLearningDocumentInput } from '../types/learningDocument'

interface DocumentForm {
  title: string
  summary: string
  content: string
  tags: string
  readingMinutes: string
  priority: string
}

export function EditLearningDocumentPage({ slug }: { slug: string }) {
  const [form, setForm] = useState<DocumentForm | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    const user = getStoredUser()
    if (!user || (user.role !== 'ADMIN' && user.role !== 'TEACHER')) {
      setError('Bạn không có quyền chỉnh sửa tài liệu.')
      setLoading(false)
      return
    }
    void Promise.all([getLearningDocument(slug), getLearningDocumentEditorAccess()])
      .then(([document, access]) => {
        if (ignore) return
        if (!access.canEdit) throw new Error('Bạn chưa được ADMIN cấp quyền chỉnh sửa tài liệu.')
        setForm({
          title: document.title,
          summary: document.summary,
          content: document.content,
          tags: document.tags.join(', '),
          readingMinutes: String(document.readingMinutes),
          priority: String(document.priority),
        })
      })
      .catch(reason => { if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải tài liệu.') })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [slug])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!form || saving) return
    setSaving(true)
    setError('')
    const input: UpdateLearningDocumentInput = {
      title: form.title,
      summary: form.summary,
      content: form.content,
      tags: form.tags.split(',').map(tag => tag.trim()).filter(Boolean),
      readingMinutes: Number(form.readingMinutes),
      priority: Number(form.priority),
    }
    try {
      await updateLearningDocument(slug, input)
      setPendingFlash('Đã cập nhật tài liệu')
      window.location.href = `/documents/${encodeURIComponent(slug)}`
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể cập nhật tài liệu.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="mx-auto max-w-5xl animate-pulse"><div className="h-9 w-72 rounded bg-slate-100" /><div className="mt-8 h-[560px] rounded-2xl bg-slate-100" /></div>
  if (!form) return <div className="mx-auto grid min-h-[420px] max-w-4xl place-items-center text-center"><div><p className="font-semibold text-red-700">{error || 'Không thể mở trình chỉnh sửa.'}</p><a href={`/documents/${encodeURIComponent(slug)}`} className="ui-button-secondary mt-4">Quay lại tài liệu</a></div></div>

  return (
    <section className="mx-auto w-full max-w-6xl">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <a href={`/documents/${encodeURIComponent(slug)}`} className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 transition hover:text-blue-700"><IconArrowLeft className="h-4 w-4" /> Quay lại tài liệu</a>
          <h1 className="mt-4 font-display text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">Chỉnh sửa tài liệu</h1>
        </div>
        <button type="submit" form="document-editor-form" disabled={saving} className="ui-button-primary gap-2"><IconSave className="h-4 w-4" />{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
      </div>

      <form id="document-editor-form" onSubmit={event => void submit(event)} className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
        <div className="space-y-5">
          <Field label="Tiêu đề"><input required maxLength={220} value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} className="ui-control mt-2" /></Field>
          <Field label="Mô tả ngắn"><textarea required maxLength={600} rows={3} value={form.summary} onChange={event => setForm({ ...form, summary: event.target.value })} className="ui-control mt-2 resize-y py-3" /></Field>
          <Field label="Nội dung">
            <textarea required minLength={20} maxLength={100000} rows={24} value={form.content} onChange={event => setForm({ ...form, content: event.target.value })} className="ui-control mt-2 min-h-[560px] resize-y py-4 font-mono text-sm leading-7" spellCheck={false} />
          </Field>
        </div>

        <aside className="space-y-5 lg:sticky lg:top-24 lg:self-start">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <h2 className="font-bold text-slate-950">Cấu trúc nội dung</h2>
            <div className="mt-3 space-y-2 font-mono text-xs leading-5 text-slate-600">
              <p><code>## Tiêu đề mục</code></p>
              <p><code>### Tiêu đề con</code></p>
              <p><code>- Danh sách</code></p>
              <p><code>1. Danh sách số</code></p>
              <p><code>**Chữ đậm**</code></p>
              <p><code>`mã nguồn`</code></p>
            </div>
            <p className="mt-3 text-xs leading-5 text-slate-500">Các tiêu đề sẽ tự động xuất hiện trong mục lục bên trái trang đọc.</p>
          </div>
          <Field label="Chủ đề"><input required value={form.tags} onChange={event => setForm({ ...form, tags: event.target.value })} placeholder="JAVA, OOP" className="ui-control mt-2" /><span className="mt-1 block text-xs font-normal text-slate-500">Phân tách bằng dấu phẩy.</span></Field>
          <Field label="Thời gian đọc (phút)"><input required type="number" min={1} max={240} value={form.readingMinutes} onChange={event => setForm({ ...form, readingMinutes: event.target.value })} className="ui-control mt-2" /></Field>
          <Field label="Độ ưu tiên"><input required type="number" min={0} max={1000} value={form.priority} onChange={event => setForm({ ...form, priority: event.target.value })} className="ui-control mt-2" /></Field>
        </aside>

        {error ? <p className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-semibold text-red-700 lg:col-span-2">{error}</p> : null}
        <div className="flex flex-col-reverse gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end lg:col-span-2">
          <a href={`/documents/${encodeURIComponent(slug)}`} className="ui-button-secondary">Hủy</a>
          <button type="submit" disabled={saving} className="ui-button-primary gap-2"><IconSave className="h-4 w-4" />{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
        </div>
      </form>
    </section>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return <label className="block text-sm font-semibold text-slate-700">{label}{children}</label>
}
