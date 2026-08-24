import type { CodeLanguage, ExamAnswer, ExamQuestionType, ExamResult, ExamSession, ExamSummary, TeacherExamResult } from '../types/exam'

function token(): string {
  const value = localStorage.getItem('devedu.accessToken') ?? localStorage.getItem('accessToken')
  if (!value) throw new Error('AUTHENTICATION_REQUIRED')
  return value
}
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, headers: { Authorization: `Bearer ${token()}`, ...(init?.headers ?? {}) } })
  if (response.status === 401) throw new Error('AUTHENTICATION_REQUIRED')
  if (response.status === 403) throw new Error('ROLE_REQUIRED')
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(body?.message ?? 'Không thể xử lý yêu cầu kỳ thi.')
  }
  return response.json() as Promise<T>
}
const json = (method: string, body?: object): RequestInit => ({ method, headers: { 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined })

export const getExams = () => request<ExamSummary[]>('/api/exams')
export const startExam = (slug: string) => request<ExamSession>(`/api/exams/${encodeURIComponent(slug)}/attempts`, { method: 'POST' })
export const saveExamAnswer = (attemptId: string, questionId: string, body: { selectedOptionIndex?: number; sourceCode?: string }) => request<ExamAnswer>(`/api/exams/attempts/${attemptId}/answers/${questionId}`, json('PUT', body))
export const submitExam = (attemptId: string) => request<ExamResult>(`/api/exams/attempts/${attemptId}/submit`, { method: 'POST' })
export const getExamResult = (attemptId: string) => request<ExamResult>(`/api/exams/attempts/${attemptId}/result`)

export const getManagedExams = () => request<ExamSummary[]>('/api/teacher/exams')
export const createExam = (body: { slug: string; title: string; description: string; scheduledAt: string; durationMinutes: number }) => request<ExamSummary>('/api/teacher/exams', json('POST', body))
export const addExamQuestion = (examId: string, body: { type: ExamQuestionType; prompt: string; options: string[]; correctOptionIndex?: number; codingLanguage?: CodeLanguage; points: number; position: number }) => request<{ id: string }>(`/api/teacher/exams/${examId}/questions`, json('POST', body))
export const getExamResults = (examId: string) => request<TeacherExamResult[]>(`/api/teacher/exams/${examId}/results`)
