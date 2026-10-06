import { useContestResource } from '../hooks/useContestResource'
import type { ContestResult } from '../types/contest'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { ContestResultSummary } from './ContestResultSummary'
import { ContestLeaderboard } from './ContestResults'
import { getStoredUser } from '../../auth/api/authApi'

export function ContestResultPage({ contestId }: { contestId: string }) {
  const { data, error, loading, reload } = useContestResource<ContestResult>(`/api/contests/${encodeURIComponent(contestId)}/results`, 5000)
  return <section><a href={`/contests/${contestId}`} className="ui-button-ghost mb-4">← Official Contest</a>
    {error && <ContestError message={error} onRetry={() => void reload()} />}
    {loading ? <ContestLoading /> : data && <>
      <header className="ui-page-header flex-wrap"><div><h1 className="ui-page-title">{data.detail.contest.name}</h1><p className="mt-2 text-sm text-slate-500">{data.finalized ? 'Contest Finished · Final results' : 'Results pending'}</p></div>
        {data.detail.contest.status === 'FINISHED' && <a className="ui-button-primary" href={`/contests/${contestId}/virtual`}>Start Virtual Contest</a>}
      </header>
      {!data.finalized ? <ContestEmpty title={data.detail.contest.status === 'FINISHED' ? 'Đang chốt kết quả' : 'Contest chưa kết thúc'}>Kết quả và rating được chốt sau khi các lượt đã nhận hoàn tất chấm. Trang tự cập nhật.</ContestEmpty> : <div className="mt-5 space-y-6"><ContestResultSummary data={data.detail} rating={data.rating} /><ContestLeaderboard data={data.detail} userId={getStoredUser()?.publicId} /></div>}
    </>}
  </section>
}
