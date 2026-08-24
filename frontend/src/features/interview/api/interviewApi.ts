import type { InterviewDifficulty, InterviewQuestionDetail, InterviewQuestionSummary, InterviewTopic } from '../types/interview'

function accessToken(): string {
  const token = localStorage.getItem('devedu.accessToken') ?? localStorage.getItem('accessToken')
  if (!token) throw new Error('AUTHENTICATION_REQUIRED')
  return token
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(path, { headers: { Authorization: `Bearer ${accessToken()}` } })
  if (response.status === 401) throw new Error('AUTHENTICATION_REQUIRED')
  if (response.status === 403) throw new Error('STUDENT_ROLE_REQUIRED')
  if (!response.ok) throw new Error('Không thể tải câu hỏi phỏng vấn.')
  return response.json() as Promise<T>
}

export function getInterviewQuestions(topic?: InterviewTopic, difficulty?: InterviewDifficulty) {
  const query = new URLSearchParams()
  if (topic) query.set('topic', topic)
  if (difficulty) query.set('difficulty', difficulty)
  const suffix = query.size ? `?${query.toString()}` : ''
  return get<InterviewQuestionSummary[]>(`/api/interview/questions${suffix}`)
}

export const getInterviewQuestion = (id: string) =>
  get<InterviewQuestionDetail>(`/api/interview/questions/${encodeURIComponent(id)}`)
