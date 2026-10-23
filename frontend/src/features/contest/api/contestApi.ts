import { getStoredAccessToken, getStoredUser } from '../../auth/api/authApi'
import type { CreateProgrammingProblem, ProblemSubmission, ProblemTestRun, ProgrammingProblemDetail, SubmissionLanguage } from '../../programming-problems/types/programmingProblem'
import type { ContestManage, ContestParticipationMode, ContestUserOption, ContestVisibility, CreateContest, ManagedContestCatalog } from '../types/contest'
import { contestErrorLabel } from '../contestPresentation'
export class ContestRequestError extends Error {
  readonly status: number
  constructor(message: string, status: number) { super(message); this.status = status }
}

export async function contestRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredAccessToken()
  const response = await fetch(path, {
    ...options,
    cache: 'no-store',
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  })
  if (!response.ok) {
    if (response.status === 401) throw new ContestRequestError('Vui lòng đăng nhập để tham gia cuộc thi.', 401)
    const error = await response.json().catch(() => null) as { message?: string } | null
    throw new ContestRequestError(contestErrorLabel(error?.message ?? 'Không thể tải cuộc thi. Vui lòng thử lại.'), response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
export const createContest = (body: CreateContest) =>
  contestRequest<{ id: string }>('/api/teacher/contests', { method: 'POST', body: JSON.stringify(body) })
export const getManagedContest = (id: string) =>
  contestRequest<ContestManage>(`/api/teacher/contests/${encodeURIComponent(id)}/manage`)
export const getCreatedContests = () =>
  contestRequest<ManagedContestCatalog>('/api/teacher/contests/mine')
export const getSupervisedContests = () =>
  contestRequest<ManagedContestCatalog>('/api/teacher/contests/supervised')
export const searchContestUsers = (role: 'STUDENT' | 'TEACHER', query: string) =>
  contestRequest<ContestUserOption[]>(`/api/teacher/contest-users?role=${role}&query=${encodeURIComponent(query)}`)
export const updateContestDetails = (id: string, body: Omit<CreateContest, 'problems'>) =>
  contestRequest<void>(`/api/teacher/contests/${encodeURIComponent(id)}/details`, { method: 'PUT', body: JSON.stringify(body) })
export const updateContestProblems = (id: string, problems: { problemId: string; points: number }[]) =>
  contestRequest<void>(`/api/teacher/contests/${encodeURIComponent(id)}/problems`, { method: 'PUT', body: JSON.stringify({ problems }) })
export const updateContestSettings = (id: string, body: { visibility: ContestVisibility; participationMode: ContestParticipationMode; allowedEmails: string[]; proctorPublicIds: number[] }) =>
  contestRequest<void>(`/api/teacher/contests/${encodeURIComponent(id)}/settings`, { method: 'PUT', body: JSON.stringify(body) })
export const publishContest = (id: string) =>
  contestRequest<void>(`/api/teacher/contests/${encodeURIComponent(id)}/publish`, { method: 'POST' })
export const createContestProblem = (id: string, problem: CreateProgrammingProblem) =>
  contestRequest<ProgrammingProblemDetail>(`/api/teacher/contests/${encodeURIComponent(id)}/local-problems`, { method: 'POST', body: JSON.stringify(problem) })
export const deleteContest = (id: string) =>
  contestRequest<void>(`/api/teacher/contests/${encodeURIComponent(id)}`, { method: 'DELETE' })
export const getContestProblem = (id: string, problemId: string) =>
  contestRequest<ProgrammingProblemDetail>(`/api/contests/${encodeURIComponent(id)}/problems/${encodeURIComponent(problemId)}`)
export const runContestProblem = (id: string, problemId: string, language: SubmissionLanguage, sourceCode: string, virtualId?: string) => {
  const scope = virtualId ? `/virtual/${encodeURIComponent(virtualId)}` : ''
  return contestRequest<ProblemTestRun>(`/api/contests/${encodeURIComponent(id)}${scope}/problems/${encodeURIComponent(problemId)}/runs`, { method: 'POST', body: JSON.stringify({ language, sourceCode }) })
}
export const registerContest = (id: string) =>
  contestRequest<void>(`/api/contests/${encodeURIComponent(id)}/registration`, { method: 'POST' })
export const startContestAttempt = (id: string) =>
  contestRequest<{ startsAt: string; endsAt: string }>(`/api/contests/${encodeURIComponent(id)}/attempt`, { method: 'POST' })
export async function submitContestProblem(id: string, problemId: string, language: SubmissionLanguage, sourceCode: string, virtualId?: string) {
  const path = `/api/contests/${encodeURIComponent(id)}${virtualId ? '/virtual/' + encodeURIComponent(virtualId) : ''}/problems/${encodeURIComponent(problemId)}/submissions`
  const body = JSON.stringify({ language, sourceCode })
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(body))
  const hash = [...new Uint8Array(digest)].map(v => v.toString(16).padStart(2, '0')).join('')
  const key = `devedu.contest.request.${getStoredUser()?.publicId}.${path}.${hash}`
  let requestId: string = crypto.randomUUID()
  try { requestId = sessionStorage.getItem(key) ?? requestId; sessionStorage.setItem(key, requestId) } catch { /* Backend still deduplicates this request's retries. */ }
  const result = await contestRequest<ProblemSubmission>(path, { method: 'POST', body, headers: { 'Idempotency-Key': requestId } })
  try { sessionStorage.removeItem(key) } catch { /* No storage access. */ }
  return result
}
