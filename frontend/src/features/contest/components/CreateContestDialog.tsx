import { useEffect, useId, useState, type FormEvent } from 'react'
import { ModalDialog } from '../../../shared/components/ModalDialog'
import { getProgrammingProblems } from '../../programming-problems/api/programmingProblemsApi'
import type { ProgrammingProblemSummary } from '../../programming-problems/types/programmingProblem'
import { createContest } from '../api/contestApi'
import type { ContestType, ContestDifficulty } from '../types/contest'
import { ContestError } from './ContestStates'

export function CreateContestDialog({ onClose, onCreated }: { onClose: () => void; onCreated: (id: string) => void }) {
  const formId = useId()
  const [catalog, setCatalog] = useState<ProgrammingProblemSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [retry, setRetry] = useState(0)
  const [selected, setSelected] = useState<{ problemId: string; points: number }[]>([])
  const [search, setSearch] = useState('')
  const [name, setName] = useState('')
  const [type, setType] = useState<ContestType>('WEEKLY')
  const [rated, setRated] = useState(false)
  const [difficulty, setDifficulty] = useState<ContestDifficulty>('BEGINNER')
  const [startsAt, setStartsAt] = useState('')
  const [duration, setDuration] = useState(120)
  const [rules, setRules] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    setLoading(true); setLoadError('')
    getProgrammingProblems().then(value => { if (!ignore) setCatalog(value) })
      .catch(reason => { if (!ignore) setLoadError(reason instanceof Error ? reason.message : 'Không thể tải bài tập.') })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [retry])

  function toggle(problemId: string) {
    setSelected(current => current.some(p => p.problemId === problemId)
      ? current.filter(p => p.problemId !== problemId)
      : current.length < 26 ? [...current, { problemId, points: 100 }] : current)
  }
  function move(index: number, direction: number) {
    setSelected(current => {
      const result = [...current]
      const target = index + direction
      if (target < 0 || target >= result.length) return current
      ;[result[index], result[target]] = [result[target], result[index]]
      return result
    })
  }
  async function save(event: FormEvent) {
    event.preventDefault()
    if (saving) return
    if (!selected.length) { setError('Chọn ít nhất một bài tập.'); return }
    const time = new Date(startsAt)
    if (!Number.isFinite(time.getTime()) || time.getTime() <= Date.now()) { setError('Ngày bắt đầu phải ở tương lai.'); return }
    setSaving(true); setError('')
    try {
      const result = await createContest({ name: name.trim(), type, startsAt: time.toISOString(), durationMinutes: duration, rules, problems: selected, rated, difficulty })
      onCreated(result.id)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể tạo Contest.')
    } finally { setSaving(false) }
  }
  const keyword = search.trim().toLocaleLowerCase('vi')
  const matches = catalog.filter(p => p.title.toLocaleLowerCase('vi').includes(keyword))
  return <ModalDialog title="Create Contest" onClose={() => { if (!saving) onClose() }} maxWidth="max-w-2xl"
    footer={<div className="flex flex-wrap items-center justify-end gap-2">
      <span className="mr-auto text-xs text-slate-500">{selected.length}/26 bài đã chọn</span>
      <button type="button" disabled={saving} onClick={onClose} className="ui-button-secondary">Cancel</button>
      <button type="submit" form={formId} disabled={saving || loading || !!loadError || !selected.length} className="ui-button-primary">{saving ? 'Creating…' : 'Create Contest'}</button>
    </div>}>
    <form id={formId} onSubmit={save} className="space-y-5 p-5 sm:p-6">
      <fieldset disabled={saving} className="min-w-0 space-y-4">
        <label className="block text-sm font-medium">Contest name<input autoFocus required maxLength={180} value={name} onChange={e => setName(e.target.value)} className="ui-control mt-1" /></label>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-medium">Type<select value={type} onChange={e => setType(e.target.value as ContestType)} className="ui-control mt-1">
            <option value="WEEKLY">Weekly</option><option value="PRACTICE">Practice</option><option value="CUSTOM">Custom</option>
          </select></label>
          <label className="text-sm font-medium">Duration (minutes)<input type="number" required min={1} max={10080} value={duration} onChange={e => setDuration(Number(e.target.value))} className="ui-control mt-1" /></label>
        </div>
        <label className="block text-sm font-medium">Start time (múi giờ của bạn)<input type="datetime-local" required value={startsAt} onChange={e => setStartsAt(e.target.value)} className="ui-control mt-1" /></label>
        <div className="grid gap-4 sm:grid-cols-2"><label className="text-sm font-medium">Rating<select className="ui-control mt-1" value={rated ? 'rated' : 'unrated'} onChange={e => setRated(e.target.value === 'rated')}><option value="unrated">Unrated</option><option value="rated">Rated</option></select></label>
          <label className="text-sm font-medium">Difficulty<select className="ui-control mt-1" value={difficulty} onChange={e => setDifficulty(e.target.value as ContestDifficulty)}><option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option></select></label></div>
        <label className="block text-sm font-medium">Rules<textarea maxLength={10000} value={rules} onChange={e => setRules(e.target.value)} className="ui-control mt-1 min-h-24" /></label>
        <section aria-label="Chọn bài tập có sẵn">
          <div className="mb-2 flex items-center justify-between"><h3 className="text-sm font-semibold">Existing problems</h3><span className="text-xs text-slate-500">{selected.length}/26 selected</span></div>
          <label className="sr-only" htmlFor="contest-problem-search">Tìm bài tập</label>
          <input id="contest-problem-search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Tìm theo tên bài tập…" className="ui-control" />
          {loadError && <ContestError message={loadError} onRetry={() => setRetry(v => v + 1)} />}
          <div className="mt-2 max-h-48 overflow-y-auto rounded-md border border-slate-200">
            {loading ? <p role="status" className="p-4 text-sm text-slate-500">Đang tải bài tập…</p> : matches.length ? matches.map(p => {
              const checked = selected.some(item => item.problemId === p.id)
              return <label key={p.id} className="flex cursor-pointer items-start gap-3 border-b border-slate-100 px-3 py-2.5 text-sm last:border-0 hover:bg-slate-50">
                <input type="checkbox" checked={checked} disabled={!checked && selected.length >= 26} onChange={() => toggle(p.id)} className="mt-1 accent-blue-600" />
                <span className="min-w-0 break-words">{p.title}<span className="ml-2 text-xs text-slate-400">{p.difficulty}</span></span>
              </label>
            }) : <p className="p-4 text-sm text-slate-500">Không tìm thấy bài tập.</p>}
          </div>
        </section>
        {selected.length > 0 && <section aria-label="Thứ tự và điểm bài tập" className="space-y-2">
          <h3 className="text-sm font-semibold">Problem order & points</h3>
          {selected.map((p, index) => <div key={p.problemId} className="flex flex-wrap items-center gap-2 rounded-md bg-slate-50 p-2 text-sm">
            <span className="font-mono font-bold text-blue-600">{String.fromCharCode(65 + index)}.</span>
            <span className="min-w-0 flex-1 break-words">{catalog.find(item => item.id === p.problemId)?.title}</span>
            <input aria-label={`Điểm bài ${String.fromCharCode(65 + index)}`} type="number" required min={1} max={10000} value={p.points}
              onChange={e => setSelected(items => items.map(item => item.problemId === p.problemId ? { ...item, points: Number(e.target.value) } : item))}
              className="ui-control !w-20" />
            <button type="button" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`Đưa bài ${index + 1} lên`} className="ui-button-ghost !px-2">↑</button>
            <button type="button" disabled={index === selected.length - 1} onClick={() => move(index, 1)} aria-label={`Đưa bài ${index + 1} xuống`} className="ui-button-ghost !px-2">↓</button>
          </div>)}
        </section>}
      </fieldset>
      {error && <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
    </form>
  </ModalDialog>
}
