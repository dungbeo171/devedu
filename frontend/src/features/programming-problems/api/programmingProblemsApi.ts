import type {
  ProblemSubmission,
  ProblemTopic,
  ProgrammingProblemDetail,
  ProgrammingProblemSummary,
  SubmissionLanguage,
} from '../types/programmingProblem'

export async function getProgrammingProblems(
  topic?: ProblemTopic,
): Promise<ProgrammingProblemSummary[]> {
  const query = topic ? `?topic=${encodeURIComponent(topic)}` : ''
  const response = await fetch(`/api/problems${query}`)
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

export async function submitProgrammingProblem(
  slug: string,
  language: SubmissionLanguage,
  sourceCode: string,
): Promise<ProblemSubmission> {
  const accessToken =
    window.localStorage.getItem('devedu.accessToken') ??
    window.localStorage.getItem('accessToken')

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
