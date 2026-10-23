import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { getProgrammingProblems } from '../../programming-problems/api/programmingProblemsApi'
import type { CreateProgrammingProblem, ProgrammingProblemDetail, ProgrammingProblemSummary } from '../../programming-problems/types/programmingProblem'
import { ProblemCreator } from '../../programming-problems/components/ProblemCreator'
import { AiProblemGenerator } from '../../programming-problems/components/AiProblemGenerator'
import { FlashToast } from '../../../shared/components/FlashToast'
import { contestSchedule } from '../contestSchedule'
import { difficultyLabel } from '../contestPresentation'
import { createContestProblem, deleteContest, getManagedContest, searchContestUsers, updateContestDetails, updateContestProblems, updateContestSettings } from '../api/contestApi'
import type { ContestDifficulty, ContestManage, ContestParticipationMode, ContestUserOption, ContestVisibility } from '../types/contest'
import { ContestError, ContestLoading } from './ContestStates'

type Tab = 'details' | 'problems' | 'settings'
const tabs: { id: Tab; label: string }[] = [{ id: 'details', label: 'Chi tiết' }, { id: 'problems', label: 'Bài tập' }, { id: 'settings', label: 'Cài đặt' }]

export function ContestEditPage({ contestId }: { contestId: string }) {
  const [data, setData] = useState<ContestManage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [deleting, setDeleting] = useState(false)
  const initialTab = new URLSearchParams(window.location.search).get('tab') as Tab | null
  const [tab, setTab] = useState<Tab>(tabs.some(item => item.id === initialTab) ? initialTab! : 'details')
  const user = getStoredUser()

  async function load() {
    setLoading(true); setError('')
    try { setData(await getManagedContest(contestId)) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Không thể tải cấu hình cuộc thi.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [contestId])

  function selectTab(next: Tab) {
    setTab(next)
    const url = new URL(window.location.href); url.searchParams.set('tab', next)
    window.history.replaceState({}, '', url)
  }
  async function removeContest() {
    if (!data || deleting || !window.confirm(`Xóa kỳ thi "${data.name}"? Kỳ thi sẽ biến mất khỏi danh sách nhưng lịch sử dữ liệu vẫn được giữ an toàn.`)) return
    setDeleting(true); setError('')
    try { await deleteContest(data.id); window.location.assign('/admin') }
    catch (reason) { setError(errorMessage(reason)); setDeleting(false) }
  }
  if (!user || user.role !== 'ADMIN' && user.role !== 'TEACHER') return <ContestError message="Bạn không có quyền quản lý cuộc thi." onRetry={() => window.location.assign('/contests')} />
  if (loading) return <ContestLoading />
  if (error && !data) return <ContestError message={error} onRetry={() => void load()} />
  if (!data) return null

  return <section className="mx-auto max-w-6xl">
    <FlashToast message={flash} onDismiss={() => setFlash('')} />
    <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <a href="/contests" className="text-sm font-medium text-slate-500 hover:text-blue-600">← Cuộc thi</a>
        <div className="mt-3 flex flex-wrap items-center gap-2"><h1 className="break-words text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{data.name}</h1><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${data.published && data.visibility === 'PUBLIC' ? 'bg-blue-50 text-blue-700' : 'bg-slate-100 text-slate-700'}`}>{data.published && data.visibility === 'PUBLIC' ? 'Công khai' : 'Riêng tư'}</span></div>
        <p className="mt-2 text-sm text-slate-500">Hoàn thiện thông tin, bài tập và cài đặt của cuộc thi.</p>
      </div>
      <div className="flex flex-wrap gap-2">
        {data.published && <a href={`/contests/${data.id}`} className="ui-button-secondary">Xem cuộc thi</a>}
        <button type="button" disabled={deleting} onClick={() => void removeContest()} className="ui-button-secondary !border-red-200 !text-red-700 hover:!bg-red-50">{deleting ? 'Đang xóa…' : 'Xóa kỳ thi'}</button>
      </div>
    </header>
    <nav className="mt-5 flex gap-1 overflow-x-auto border-b border-slate-200" aria-label="Chỉnh sửa cuộc thi">
      {tabs.map(item => <button key={item.id} type="button" onClick={() => selectTab(item.id)} className={`shrink-0 cursor-pointer border-b-2 px-4 py-3 text-sm font-semibold ${tab === item.id ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-900'}`}>{item.label}</button>)}
    </nav>
    {error && <p role="alert" className="mt-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
    <div className="mt-6">
      {tab === 'details' && <DetailsTab data={data} disabled={!data.editable} onSaved={async () => { setFlash('Đã lưu chi tiết cuộc thi'); await load() }} onError={setError} />}
      {tab === 'problems' && <ProblemsTab data={data} disabled={!data.editable} onSaved={async () => { setFlash('Đã lưu danh sách bài tập'); await load() }} onError={setError} />}
      {tab === 'settings' && <SettingsTab data={data} disabled={!data.editable} onSaved={async () => { setFlash('Đã lưu cài đặt cuộc thi'); await load() }} onError={setError} />}
    </div>
  </section>
}

function DetailsTab({ data, disabled, onSaved, onError }: TabProps) {
  const [name, setName] = useState(data.name), [description, setDescription] = useState(data.description)
  const [startsAt, setStartsAt] = useState(localDateTime(data.startsAt)), [endsAt, setEndsAt] = useState(localDateTime(data.endsAt))
  const [durationMinutes, setDurationMinutes] = useState(data.durationMinutes)
  const [difficulty, setDifficulty] = useState<ContestDifficulty>(data.difficulty)
  const [monitoringEnabled, setMonitoringEnabled] = useState(data.monitoringEnabled), [saving, setSaving] = useState(false)
  async function save(event: FormEvent) {
    event.preventDefault(); setSaving(true); onError('')
    try { await updateContestDetails(data.id, { name: name.trim(), description: description.trim(), type: data.type, difficulty, monitoringEnabled, ...contestSchedule(startsAt, endsAt, durationMinutes) }); await onSaved() }
    catch (reason) { onError(errorMessage(reason)) } finally { setSaving(false) }
  }
  return <form onSubmit={save} className="ui-panel space-y-5 p-5 sm:p-7"><fieldset disabled={disabled || saving} className="space-y-5">
    <label className="block text-sm font-semibold">Tên cuộc thi *<input required maxLength={180} value={name} onChange={e => setName(e.target.value)} className="ui-control mt-2" /></label>
    <label className="block text-sm font-semibold">Mô tả<textarea maxLength={5000} value={description} onChange={e => setDescription(e.target.value)} className="ui-control mt-2 min-h-28 resize-y" /></label>
    <div className="grid gap-4 md:grid-cols-2"><label className="text-sm font-semibold">Bắt đầu *<input required type="datetime-local" value={startsAt} onChange={e => setStartsAt(e.target.value)} className="ui-control mt-2" /></label><label className="text-sm font-semibold">Kết thúc *<input required type="datetime-local" value={endsAt} onChange={e => setEndsAt(e.target.value)} className="ui-control mt-2" /></label></div>
    <div className="grid gap-4 md:grid-cols-2"><label className="text-sm font-semibold">Thời gian làm (phút) *<input required type="number" min={1} max={10080} value={durationMinutes} onChange={e => setDurationMinutes(Number(e.target.value))} className="ui-control mt-2" /></label><label className="text-sm font-semibold">Độ khó<select value={difficulty} onChange={e => setDifficulty(e.target.value as ContestDifficulty)} className="ui-control mt-2"><option value="BEGINNER">Dễ</option><option value="INTERMEDIATE">Trung bình</option><option value="ADVANCED">Khó</option></select></label></div>
    <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-slate-200 p-4"><input type="checkbox" checked={monitoringEnabled} onChange={e => setMonitoringEnabled(e.target.checked)} className="mt-0.5 accent-blue-600" /><span><b className="block text-sm">Chế độ giám sát</b><span className="mt-1 block text-xs text-slate-500">Bật camera trong lượt thi chính thức.</span></span></label>
  </fieldset><div className="flex justify-end border-t border-slate-100 pt-5"><button disabled={disabled || saving} className="ui-button-primary">{saving ? 'Đang lưu…' : 'Lưu chi tiết'}</button></div></form>
}

function ProblemsTab({ data, disabled, onSaved, onError }: TabProps) {
  const [catalog, setCatalog] = useState<ProgrammingProblemSummary[]>([])
  const [local, setLocal] = useState<ProgrammingProblemSummary[]>(() => data.localProblems.map(problem => ({ ...problem, summary: 'Bài riêng của kỳ thi', topic: 'ALGORITHMS', allowedLanguages: [], acceptanceRate: 0 })))
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [mode, setMode] = useState<'catalog' | 'manual' | 'ai'>('catalog')
  const [selected, setSelected] = useState(() => normalizeContestPoints(data.problems))
  const [saving, setSaving] = useState(false)
  useEffect(() => { getProgrammingProblems().then(setCatalog).catch(reason => onError(errorMessage(reason))).finally(() => setLoading(false)) }, [])
  const allProblems = useMemo(() => [...local, ...catalog], [local, catalog])
  const matches = useMemo(() => {
    const q = search.trim().toLocaleLowerCase('vi')
    return allProblems.filter(item => !q || item.title.toLocaleLowerCase('vi').includes(q) || item.slug.includes(q))
  }, [allProblems, search])
  const totalPoints = selected.reduce((total, item) => total + item.points, 0)
  const validTotal = selected.length === 0 || totalPoints === 10
  function toggle(id: string) { setSelected(items => items.some(item => item.problemId === id) ? items.filter(item => item.problemId !== id) : items.length < 10 ? [...items, { problemId: id, points: 1 }] : items) }
  function changePoints(id: string, delta: -1 | 1) { setSelected(items => items.map(item => item.problemId === id ? { ...item, points: Math.min(10, Math.max(1, item.points + delta)) } : item)) }
  function move(index: number, offset: number) { setSelected(items => { const next = [...items], target = index + offset; if (target < 0 || target >= next.length) return items; [next[index], next[target]] = [next[target], next[index]]; return next }) }
  async function save() { setSaving(true); onError(''); try { await updateContestProblems(data.id, selected); await onSaved() } catch (reason) { onError(errorMessage(reason)) } finally { setSaving(false) } }
  async function saveLocalProblem(problem: CreateProgrammingProblem) {
    return createContestProblem(data.id, problem)
  }
  function localCreated(problem: ProgrammingProblemDetail) {
    const summary: ProgrammingProblemSummary = { id: problem.id, slug: problem.slug, title: problem.title, summary: problem.summary, topic: problem.topic, difficulty: problem.difficulty, allowedLanguages: problem.allowedLanguages, acceptanceRate: 0 }
    setLocal(items => items.some(item => item.id === problem.id) ? items : [summary, ...items])
    setSelected(items => items.some(item => item.problemId === problem.id) || items.length >= 10 ? items : [...items, { problemId: problem.id, points: 1 }])
    setMode('catalog')
  }
  return <div className="space-y-5">
    <div className="flex flex-wrap gap-2 rounded-xl border border-slate-200 bg-white p-2">
      <button type="button" onClick={() => setMode('catalog')} className={mode === 'catalog' ? 'ui-button-primary' : 'ui-button-ghost'}>Chọn từ kho</button>
      <button type="button" disabled={disabled} onClick={() => setMode('manual')} className={mode === 'manual' ? 'ui-button-primary' : 'ui-button-ghost'}>Tự điền bài tập</button>
      <button type="button" disabled={disabled} onClick={() => setMode('ai')} className={mode === 'ai' ? 'ui-button-primary' : 'ui-button-ghost'}>Tạo bằng AI</button>
    </div>
    {mode === 'manual' && <ProblemCreator standalone saveProblem={saveLocalProblem} scopeLabel="Chỉ lưu trong kỳ thi này, không đưa vào kho Bài tập" onCancel={() => setMode('catalog')} onCreated={localCreated} />}
    {mode === 'ai' && <AiProblemGenerator saveProblem={saveLocalProblem} scopeLabel="Bản nháp sau khi duyệt chỉ được lưu trong kỳ thi này" onCreated={localCreated} />}
    {mode === 'catalog' && <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(320px,.72fr)]">
      <section className="ui-panel p-5"><div className="flex items-center justify-between gap-3"><h2 className="text-base font-bold">Bài tập cho kỳ thi</h2><span className="text-xs text-slate-500">{selected.length}/10 đã chọn</span></div><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Tìm tên hoặc slug bài tập…" className="ui-control mt-4" />
        <div className="mt-3 max-h-[520px] overflow-y-auto rounded-lg border border-slate-200">{loading ? <p className="p-4 text-sm text-slate-500">Đang tải bài tập…</p> : matches.map(problem => { const checked = selected.some(item => item.problemId === problem.id); const contestOwned = local.some(item => item.id === problem.id); return <label key={problem.id} className="flex cursor-pointer items-start gap-3 border-b border-slate-100 px-4 py-3 last:border-0 hover:bg-slate-50"><input type="checkbox" disabled={disabled || !checked && selected.length >= 10} checked={checked} onChange={() => toggle(problem.id)} className="mt-1 accent-blue-600" /><span className="min-w-0"><b className="block break-words text-sm text-slate-800">{problem.title}</b><span className="text-xs text-slate-500">{difficultyLabel(problem.difficulty)} · {problem.slug}</span>{contestOwned && <span className="mt-1 block text-xs font-semibold text-blue-600">Bài riêng của kỳ thi</span>}</span></label> })}</div>
      </section>
      <section className="ui-panel p-5"><div className="flex items-center justify-between gap-3"><h2 className="text-base font-bold">Thứ tự và điểm</h2><span className={`rounded-full px-2.5 py-1 text-xs font-bold ${totalPoints === 10 ? 'bg-blue-50 text-blue-700' : 'bg-red-50 text-red-700'}`}>{totalPoints}/10 điểm</span></div>{!selected.length ? <p className="mt-4 rounded-lg bg-slate-50 p-5 text-center text-sm text-slate-500">Chưa chọn bài tập.</p> : <div className="mt-4 space-y-2">{selected.map((item, index) => <div key={item.problemId} className="flex items-center gap-2 rounded-lg border border-slate-200 p-3"><span className="w-6 font-mono font-bold text-blue-600">{String.fromCharCode(65 + index)}</span><span className="min-w-0 flex-1 truncate text-sm font-medium">{allProblems.find(problem => problem.id === item.problemId)?.title ?? item.problemId}</span><div className="flex shrink-0 items-center rounded-lg border border-slate-200"><button type="button" disabled={disabled || item.points <= 1} onClick={() => changePoints(item.problemId, -1)} className="h-9 w-9 cursor-pointer text-lg font-semibold disabled:cursor-not-allowed disabled:opacity-40">−</button><output className="w-8 text-center text-sm font-bold tabular-nums">{item.points}</output><button type="button" disabled={disabled || item.points >= 10 || totalPoints >= 10} onClick={() => changePoints(item.problemId, 1)} className="h-9 w-9 cursor-pointer text-lg font-semibold disabled:cursor-not-allowed disabled:opacity-40">+</button></div><button type="button" disabled={disabled || index === 0} onClick={() => move(index, -1)} className="ui-button-ghost !px-2">↑</button><button type="button" disabled={disabled || index === selected.length - 1} onClick={() => move(index, 1)} className="ui-button-ghost !px-2">↓</button></div>)}</div>}{!validTotal && <p className="mt-3 text-xs font-semibold text-red-700">Tổng điểm của các bài phải bằng đúng 10.</p>}<button type="button" disabled={disabled || saving || !validTotal} onClick={() => void save()} className="ui-button-primary mt-5 w-full justify-center">{saving ? 'Đang lưu…' : 'Lưu bài tập'}</button></section>
    </div>}
  </div>
}

function SettingsTab({ data, disabled, onSaved, onError }: TabProps) {
  const [visibility, setVisibility] = useState<ContestVisibility>(data.visibility), [mode, setMode] = useState<ContestParticipationMode>(data.participationMode)
  const [emails, setEmails] = useState(data.allowedEmails), [proctors, setProctors] = useState(data.proctors)
  const [studentQuery, setStudentQuery] = useState(''), [teacherQuery, setTeacherQuery] = useState('')
  const [students, setStudents] = useState<ContestUserOption[]>([]), [teachers, setTeachers] = useState<ContestUserOption[]>([])
  const [emailInput, setEmailInput] = useState(''), [saving, setSaving] = useState(false)
  useEffect(() => { const timer = window.setTimeout(() => { if (mode === 'RESTRICTED') void searchContestUsers('STUDENT', studentQuery).then(setStudents).catch(reason => onError(errorMessage(reason))) }, 250); return () => clearTimeout(timer) }, [studentQuery, mode])
  useEffect(() => { const timer = window.setTimeout(() => { void searchContestUsers('TEACHER', teacherQuery).then(setTeachers).catch(reason => onError(errorMessage(reason))) }, 250); return () => clearTimeout(timer) }, [teacherQuery])
  function addEmail(raw: string) { const email = raw.trim().toLocaleLowerCase(); if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setEmails(values => values.includes(email) ? values : [...values, email]); setEmailInput('') } }
  async function save() { setSaving(true); onError(''); try { await updateContestSettings(data.id, { visibility, participationMode: mode, allowedEmails: emails, proctorPublicIds: proctors.map(item => item.id) }); await onSaved() } catch (reason) { onError(errorMessage(reason)) } finally { setSaving(false) } }
  return <div className="space-y-5"><section className="ui-panel p-5 sm:p-7"><h2 className="text-base font-bold">Hiển thị và quyền tham gia</h2><label className="mt-5 block max-w-sm text-sm font-semibold">Trạng thái<select disabled={disabled} value={visibility} onChange={event => setVisibility(event.target.value as ContestVisibility)} className="ui-control mt-2"><option value="PRIVATE">Riêng tư</option><option value="PUBLIC">Công khai</option></select><span className="mt-2 block text-xs font-normal leading-5 text-slate-500">Chọn Công khai và lưu cài đặt để đưa cuộc thi lên danh sách.</span></label><div className="mt-5 grid gap-4 md:grid-cols-2">
    <Choice checked={mode === 'OPEN'} disabled={disabled} onChange={() => setMode('OPEN')} title="Tự do tham gia" text="Mọi tài khoản đã đăng nhập đều có thể đăng ký." /><Choice checked={mode === 'RESTRICTED'} disabled={disabled} onChange={() => setMode('RESTRICTED')} title="Giới hạn thí sinh" text="Chỉ email được chọn mới có thể đăng ký." />
  </div>
  {mode === 'RESTRICTED' && <div className="mt-6 border-t border-slate-100 pt-5"><h3 className="text-sm font-bold">Email thí sinh được phép</h3><div className="mt-3 flex gap-2"><input disabled={disabled} value={emailInput} onChange={e => setEmailInput(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addEmail(emailInput) } }} placeholder="student@example.com" className="ui-control" /><button type="button" disabled={disabled} onClick={() => addEmail(emailInput)} className="ui-button-secondary shrink-0">Thêm</button></div><input disabled={disabled} value={studentQuery} onChange={e => setStudentQuery(e.target.value)} placeholder="Tìm sinh viên theo tên hoặc email…" className="ui-control mt-3" /><Picker users={students} selected={new Set(emails)} keyOf={item => item.email} disabled={disabled} onToggle={item => setEmails(values => values.includes(item.email) ? values.filter(email => email !== item.email) : [...values, item.email])} />
    {!!emails.length && <div className="mt-3 flex flex-wrap gap-2">{emails.map(email => <button type="button" disabled={disabled} onClick={() => setEmails(values => values.filter(value => value !== email))} key={email} className="rounded-full bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-100">{email} ×</button>)}</div>}</div>}
  </section>
  <section className="ui-panel p-5 sm:p-7"><h2 className="text-base font-bold">Giáo viên giám sát</h2><p className="mt-1 text-xs leading-5 text-slate-500">Giáo viên được chọn có quyền xem báo cáo giám sát camera. Người tạo cuộc thi và admin luôn có quyền này.</p><input disabled={disabled} value={teacherQuery} onChange={e => setTeacherQuery(e.target.value)} placeholder="Tìm giáo viên theo tên hoặc email…" className="ui-control mt-4" /><Picker users={teachers} selected={new Set(proctors.map(item => String(item.id)))} keyOf={item => String(item.id)} disabled={disabled} onToggle={item => setProctors(values => values.some(value => value.id === item.id) ? values.filter(value => value.id !== item.id) : [...values, item])} />
    {!!proctors.length && <div className="mt-3 flex flex-wrap gap-2">{proctors.map(item => <button type="button" disabled={disabled} onClick={() => setProctors(values => values.filter(value => value.id !== item.id))} key={item.id} className="rounded-full bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-100">{item.name} ×</button>)}</div>}
  </section><div className="flex justify-end"><button type="button" disabled={disabled || saving || mode === 'RESTRICTED' && !emails.length} onClick={() => void save()} className="ui-button-primary">{saving ? 'Đang lưu…' : 'Lưu cài đặt'}</button></div></div>
}

function Choice({ checked, disabled, onChange, title, text }: { checked: boolean; disabled: boolean; onChange: () => void; title: string; text: string }) { return <label className={`flex cursor-pointer gap-3 rounded-lg border p-4 ${checked ? 'border-blue-500 bg-blue-50/60' : 'border-slate-200'}`}><input type="radio" checked={checked} disabled={disabled} onChange={onChange} className="mt-0.5 accent-blue-600" /><span><b className="block text-sm">{title}</b><span className="mt-1 block text-xs leading-5 text-slate-500">{text}</span></span></label> }
function Picker({ users, selected, keyOf, disabled, onToggle }: { users: ContestUserOption[]; selected: Set<string>; keyOf: (item: ContestUserOption) => string; disabled: boolean; onToggle: (item: ContestUserOption) => void }) { return <div className="mt-2 max-h-52 overflow-y-auto rounded-lg border border-slate-200">{users.length ? users.map(item => <label key={item.id} className="flex cursor-pointer items-center gap-3 border-b border-slate-100 px-3 py-2.5 last:border-0 hover:bg-slate-50"><input type="checkbox" disabled={disabled} checked={selected.has(keyOf(item))} onChange={() => onToggle(item)} className="accent-blue-600" /><span className="min-w-0"><b className="block truncate text-sm">{item.name}</b><span className="block truncate text-xs text-slate-500">{item.email}{item.code ? ` · ${item.code}` : ''}</span></span></label>) : <p className="p-4 text-sm text-slate-500">Không tìm thấy tài khoản phù hợp.</p>}</div> }
interface TabProps { data: ContestManage; disabled: boolean; onSaved: () => Promise<void>; onError: (message: string) => void }
function normalizeContestPoints(problems: ContestManage['problems']) {
  if (!problems.length) return problems
  if (problems.length <= 10 && problems.reduce((total, item) => total + item.points, 0) === 10 && problems.every(item => item.points >= 1 && item.points <= 10)) return problems
  return problems.map((item, index) => ({ ...item, points: index === 0 && problems.length <= 10 ? 11 - problems.length : 1 }))
}
function localDateTime(value: string) { const date = new Date(value); const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000); return local.toISOString().slice(0, 16) }
function errorMessage(reason: unknown) { return reason instanceof Error ? reason.message : 'Không thể lưu thay đổi.' }
