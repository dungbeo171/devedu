import type { CourseDetail, CourseSummary, Lesson, LessonProgress } from '../types/courseLearning'

function accessToken(): string {
  const token = localStorage.getItem('devedu.accessToken') ?? localStorage.getItem('accessToken')
  if (!token) throw new Error('AUTHENTICATION_REQUIRED')
  return token
}

async function parseError(response: Response, fallback: string): Promise<never> {
  if (response.status === 401) throw new Error('AUTHENTICATION_REQUIRED')
  if (response.status === 403) throw new Error('ROLE_REQUIRED')
  const body = (await response.json().catch(() => null)) as { message?: string } | null
  throw new Error(body?.message ?? fallback)
}

export async function getCourses(): Promise<CourseSummary[]> {
  const response = await fetch('/api/courses')
  if (!response.ok) return parseError(response, 'Không thể tải danh sách môn học.')
  return response.json() as Promise<CourseSummary[]>
}

export async function getCourse(slug: string): Promise<CourseDetail> {
  const response = await fetch(`/api/courses/${encodeURIComponent(slug)}`)
  if (!response.ok) return parseError(response, 'Không thể tải môn học.')
  return response.json() as Promise<CourseDetail>
}

export async function getLesson(lessonId: string): Promise<Lesson> {
  const response = await fetch(`/api/lessons/${encodeURIComponent(lessonId)}`)
  if (!response.ok) return parseError(response, 'Không thể tải lesson.')
  return response.json() as Promise<Lesson>
}

export async function completeLesson(lessonId: string): Promise<LessonProgress> {
  const response = await fetch(`/api/student/lessons/${encodeURIComponent(lessonId)}/complete`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken()}` },
  })
  if (!response.ok) return parseError(response, 'Không thể cập nhật tiến độ.')
  return response.json() as Promise<LessonProgress>
}

async function teacherRequest<T>(path: string, method: 'POST' | 'PUT', body: object): Promise<T> {
  const response = await fetch(path, {
    method,
    headers: { Authorization: `Bearer ${accessToken()}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) return parseError(response, 'Không thể lưu nội dung khóa học.')
  return response.json() as Promise<T>
}

export const createCourse = (body: { slug: string; title: string; description: string }) =>
  teacherRequest<CourseSummary>('/api/teacher/courses', 'POST', body)

export const createTopic = (courseId: string, body: { title: string; position: number }) =>
  teacherRequest<{ id: string }>(`/api/teacher/courses/${courseId}/topics`, 'POST', body)

export const createLesson = (topicId: string, body: { title: string; content: string; position: number }) =>
  teacherRequest<Lesson>(`/api/teacher/topics/${topicId}/lessons`, 'POST', body)

export const setLessonVideo = (lessonId: string, videoUrl: string) =>
  teacherRequest<Lesson>(`/api/teacher/lessons/${lessonId}/video`, 'PUT', { videoUrl })
