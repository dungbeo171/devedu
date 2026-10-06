import { getStoredAccessToken, getStoredUser } from '../../auth/api/authApi'
import type { ProblemSubmission, SubmissionLanguage } from '../../programming-problems/types/programmingProblem'
import type { CreateContest } from '../types/contest'
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
    if (response.status === 401) throw new ContestRequestError('Vui lòng đăng nhập để tham gia Contest.', 401)
    const error = await response.json().catch(() => null) as { message?: string } | null
    throw new ContestRequestError(error?.message ?? 'Không thể tải Contest. Vui lòng thử lại.', response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
export const createContest = (body: CreateContest) =>
  contestRequest<{ id: string }>('/api/teacher/contests', { method: 'POST', body: JSON.stringify(body) })
export const registerContest = (id: string) =>
  contestRequest<void>(`/api/contests/${encodeURIComponent(id)}/registration`, { method: 'POST' })
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
