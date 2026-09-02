import type { CourseDetail, CourseSummary, CourseMaterial, CourseProblem, CourseStudent, CourseStudentCandidate, Lesson, LessonProgress, ManagedCourse, StudentCourse, StudentCourseDetails } from '../types/courseLearning'

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

async function teacherRequest<T>(path: string, method: 'GET' | 'POST' | 'PUT' | 'DELETE', body?: object): Promise<T> {
  const response = await fetch(path, {
    method,
    headers: { Authorization: `Bearer ${accessToken()}`, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) return parseError(response, 'Không thể lưu nội dung khóa học.')
  return response.json() as Promise<T>
}

export const createCourse = (body: { slug: string; title: string; description: string; startDate: string | null; endDate: string | null }) =>
  teacherRequest<CourseSummary>('/api/teacher/courses', 'POST', body)

export const getManagedCourses = () =>
  teacherRequest<ManagedCourse[]>('/api/teacher/courses', 'GET')

export const createTopic = (courseId: string, body: { title: string; position: number }) =>
  teacherRequest<{ id: string }>(`/api/teacher/courses/${courseId}/topics`, 'POST', body)

export const createLesson = (topicId: string, body: { title: string; content: string; position: number }) =>
  teacherRequest<Lesson>(`/api/teacher/topics/${topicId}/lessons`, 'POST', body)

export const setLessonVideo = (lessonId: string, videoUrl: string) =>
  teacherRequest<Lesson>(`/api/teacher/lessons/${lessonId}/video`, 'PUT', { videoUrl })

export const enrollStudent = (courseId: string, studentCode: string) =>
  teacherRequest<CourseStudent[]>(`/api/teacher/courses/${courseId}/students`, 'POST', { studentCode })

export const getCourseStudents = (courseId: string) =>
  teacherRequest<CourseStudent[]>(`/api/teacher/courses/${courseId}/students`, 'GET')

export const searchCourseStudentCandidates = (courseId: string, query: string) =>
  teacherRequest<CourseStudentCandidate[]>(`/api/teacher/courses/${courseId}/student-candidates?q=${encodeURIComponent(query)}`, 'GET')

export const addCourseStudents = (courseId: string, studentIds: number[]) =>
  teacherRequest<CourseStudent[]>(`/api/teacher/courses/${courseId}/students/bulk`, 'POST', { studentIds })

export const removeCourseStudents = (courseId: string, studentIds: number[]) =>
  teacherRequest<CourseStudent[]>(`/api/teacher/courses/${courseId}/students`, 'DELETE', { studentIds })

export const updateCourseStudent = (courseId: string, studentId: number, displayName: string) =>
  teacherRequest<CourseStudent[]>(`/api/teacher/courses/${courseId}/students/${studentId}`, 'PUT', { displayName })

export const getTeacherCourseProblems = (courseId: string) =>
  teacherRequest<CourseProblem[]>(`/api/teacher/courses/${courseId}/problems`, 'GET')

export const assignTeacherCourseProblem = (courseId: string, problemId: string) =>
  teacherRequest<CourseProblem[]>(`/api/teacher/courses/${courseId}/problems`, 'POST', { problemId })

export const removeTeacherCourseProblem = (courseId: string, problemId: string) =>
  teacherRequest<CourseProblem[]>(`/api/teacher/courses/${courseId}/problems/${problemId}`, 'DELETE')

export const getStudentCourses = () =>
  teacherRequest<StudentCourse[]>('/api/student/courses', 'GET')

export const getStudentCourse = (courseId: string) =>
  teacherRequest<StudentCourseDetails>(`/api/student/courses/${courseId}`, 'GET')

async function uploadForm<T>(path: string, form: FormData): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken()}` },
    body: form,
  })
  if (!response.ok) return parseError(response, 'Không thể tải file lên.')
  return response.json() as Promise<T>
}

export function importCourseStudents(courseId: string, file: File): Promise<CourseStudent[]> {
  const form = new FormData()
  form.append('file', file)
  return uploadForm(`/api/teacher/courses/${courseId}/students/import`, form)
}

export function uploadCourseMaterial(courseId: string, title: string, file: File): Promise<CourseMaterial> {
  const form = new FormData()
  form.append('title', title)
  form.append('file', file)
  return uploadForm(`/api/teacher/courses/${courseId}/materials`, form)
}

export async function getCourseMaterials(courseId: string): Promise<CourseMaterial[]> {
  const response = await fetch(`/api/courses/${courseId}/materials`, {
    headers: { Authorization: `Bearer ${accessToken()}` },
  })
  if (!response.ok) return parseError(response, 'Không thể tải tài liệu lớp học.')
  return response.json() as Promise<CourseMaterial[]>
}

export async function openCourseMaterial(material: CourseMaterial): Promise<void> {
  const response = await fetch(`/api/course-materials/${material.id}/content`, {
    headers: { Authorization: `Bearer ${accessToken()}` },
  })
  if (!response.ok) return parseError(response, 'Không thể mở tài liệu.')
  const blobUrl = URL.createObjectURL(await response.blob())
  window.open(blobUrl, '_blank', 'noopener,noreferrer')
  window.setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000)
}
