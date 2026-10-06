import { useEffect, useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { useContestResource } from '../hooks/useContestResource'
import { contestRequest } from '../api/contestApi'
import type { VirtualSession } from '../types/contest'
import { ContestExperience } from './ContestDetailPage'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'

export function VirtualContestPage({ contestId }: { contestId: string }) {
  const [sessionId, setSessionId] = useState(() => new URLSearchParams(window.location.search).get('session'))
  const resource = useContestResource<VirtualSession>(`/api/contests/${encodeURIComponent(contestId)}/virtual${sessionId ? '?sessionId=' + encodeURIComponent(sessionId) : ''}`, 5000)
  const [starting, setStarting] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    const update = () => setSessionId(new URLSearchParams(window.location.search).get('session'))
    window.addEventListener('popstate', update)
    return () => window.removeEventListener('popstate', update)
  }, [])
  useEffect(() => {
    if (!sessionId && resource.data) {
      const params = new URLSearchParams(window.location.search)
      params.set('session', resource.data.session.id)
      window.history.replaceState(null, '', window.location.pathname + '?' + params)
      setSessionId(resource.data.session.id)
    }
  }, [sessionId, resource.data])
  async function start() {
    if (starting) return
    setStarting(true); setError('')
    try {
      const result = await contestRequest<VirtualSession>(`/api/contests/${encodeURIComponent(contestId)}/virtual`, { method: 'POST' })
      window.history.replaceState(null, '', `/contests/${contestId}/virtual?session=${result.session.id}&enter=1`)
      if (sessionId === result.session.id) await resource.reload(); else setSessionId(result.session.id)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Không thể bắt đầu Virtual Contest.') }
    finally { setStarting(false) }
  }
  if (!getStoredUser()) return <ContestEmpty title="Đăng nhập để tham gia Virtual Contest"><a href="/login" className="ui-button-primary">Đăng nhập</a></ContestEmpty>
  if (resource.loading) return <ContestLoading />
  if (!resource.data) return <section><a className="ui-button-ghost" href={`/contests/${contestId}`}>← Official Contest</a>
    {resource.errorStatus !== 404 && resource.error && <ContestError message={resource.error} onRetry={() => void resource.reload()} />}
    <ContestEmpty title="Virtual Contest"><p className="mb-4">Bắt đầu lượt thi riêng với đủ thời lượng. Không ảnh hưởng leaderboard hoặc rating chính thức. Chỉ khả dụng sau khi Contest kết thúc.</p>
      <button type="button" className="ui-button-primary" disabled={starting} onClick={() => void start()}>{starting ? 'Starting…' : 'Start Virtual Contest'}</button></ContestEmpty>
    {error && <ContestError message={error} onRetry={() => setError('')} />}</section>
  const ended = resource.now >= Date.parse(resource.data.session.endsAt)
  return <>
    {ended && <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-md border border-blue-100 bg-blue-50 p-3 text-sm"><span>Virtual attempt ended · Official rating không thay đổi.</span><button className="ui-button-secondary" disabled={starting} onClick={() => void start()}>{starting ? 'Starting…' : 'Start new virtual attempt'}</button></div>}
    {error && <ContestError message={error} onRetry={() => setError('')} />}
    <ContestExperience key={resource.data.session.id} contestId={contestId} virtualId={resource.data.session.id} resource={{ ...resource, data: resource.data.detail }} />
  </>
}
