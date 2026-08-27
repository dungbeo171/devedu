import type {
  ProblemSubmission,
  ProgrammingProblemDraft,
  ProblemCodeExecution,
  ProblemTopic,
  ProblemDifficulty,
  ProgrammingProblemDetail,
  ProgrammingProblemSummary,
  SubmissionLanguage,
} from '../types/programmingProblem'

function storedAccessToken(): string | null {
  return window.localStorage.getItem('devedu.accessToken') ?? window.localStorage.getItem('accessToken')
}

export async function runProgrammingProblemCode(
  language: SubmissionLanguage,
  code: string,
  input: string,
): Promise<ProblemCodeExecution> {
  const response = await fetch('/api/code/execute', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ language, code, input }),
  })
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(body?.message ?? 'Không thể chạy thử code lúc này.')
  }
  return response.json() as Promise<ProblemCodeExecution>
}

export async function getProgrammingProblems(
  filters?: { topic?: ProblemTopic; difficulty?: ProblemDifficulty; language?: SubmissionLanguage },
): Promise<ProgrammingProblemSummary[]> {
  const query = new URLSearchParams()
  if (filters?.topic) query.set('topic', filters.topic)
  if (filters?.difficulty) query.set('difficulty', filters.difficulty)
  if (filters?.language) query.set('language', filters.language)
  const response = await fetch(`/api/problems${query.size ? `?${query.toString()}` : ''}`)
  if (!response.ok) {
    throw new Error('Không thể tải danh sách bài tập.')
  }
  return response.json() as Promise<ProgrammingProblemSummary[]>
}

export async function getProgrammingProblem(
  slug: string,
): Promise<ProgrammingProblemDetail> {
  const response = await fetch(`/api/problems/${encodeURIComponent(slug)}`)
  if (!response.ok) {
    throw new Error(response.status === 404 ? 'Không tìm thấy bài tập.' : 'Không thể tải đề bài.')
  }
  return response.json() as Promise<ProgrammingProblemDetail>
}

export async function getSolvedProgrammingProblemIds(): Promise<string[]> {
  const accessToken = storedAccessToken()
  if (!accessToken) return []

  const response = await fetch('/api/student/problem-progress', {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (response.status === 401 || response.status === 403) return []
  if (!response.ok) throw new Error('Không thể tải tiến độ bài tập.')
  const result = await response.json() as { problemIds: string[] }
  return result.problemIds
}

export async function getProgrammingProblemDraft(slug: string): Promise<ProgrammingProblemDraft | null> {
  const accessToken = storedAccessToken()
  if (!accessToken) return null
  const response = await fetch(`/api/student/problems/${encodeURIComponent(slug)}/draft`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (response.status === 204 || response.status === 401 || response.status === 403) return null
  if (!response.ok) throw new Error('Không thể tải bản nháp.')
  return response.json() as Promise<ProgrammingProblemDraft>
}

export async function saveProgrammingProblemDraft(
  slug: string,
  language: SubmissionLanguage,
  sourceCode: string,
  input: string,
): Promise<ProgrammingProblemDraft | null> {
  const accessToken = storedAccessToken()
  if (!accessToken) return null
  const response = await fetch(`/api/student/problems/${encodeURIComponent(slug)}/draft`, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ language, sourceCode, input }),
  })
  if (response.status === 401 || response.status === 403) return null
  if (!response.ok) throw new Error('Không thể lưu bản nháp.')
  return response.json() as Promise<ProgrammingProblemDraft>
}

export async function submitProgrammingProblem(
  slug: string,
  language: SubmissionLanguage,
  sourceCode: string,
): Promise<ProblemSubmission> {
  const accessToken = storedAccessToken()

  if (!accessToken) {
    throw new Error('AUTHENTICATION_REQUIRED')
  }

  const response = await fetch(`/api/problems/${encodeURIComponent(slug)}/submissions`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ language, sourceCode }),
  })

  if (response.status === 401) {
    throw new Error('AUTHENTICATION_REQUIRED')
  }
  if (response.status === 403) {
    throw new Error('STUDENT_ROLE_REQUIRED')
  }
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(body?.message ?? 'Không thể gửi bài lúc này.')
  }
  return response.json() as Promise<ProblemSubmission>
}
