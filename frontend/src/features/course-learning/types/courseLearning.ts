export interface CourseSummary {
  id: string
  slug: string
  title: string
  description: string
  startDate: string
  endDate: string | null
  createdAt: string
}

export type CourseStatus = 'ACTIVE' | 'ENDED'

export interface ManagedCourse {
  id: string
  code: string
  title: string
  description: string
  teacherName: string
  studentCount: number
  startDate: string
  endDate: string | null
  status: CourseStatus
}

export interface LessonSummary {
  id: string
  title: string
  position: number
  hasVideo: boolean
}

export interface CourseTopic {
  id: string
  title: string
  position: number
  lessons: LessonSummary[]
}

export interface CourseDetail extends CourseSummary {
  topics: CourseTopic[]
}

export interface Lesson {
  id: string
  topicId: string
  title: string
  content: string
  videoUrl: string | null
  position: number
  createdAt: string
}

export interface LessonProgress {
  id: string
  lessonId: string
  completedAt: string
}

export interface CourseMaterial {
  id: string
  title: string
  fileName: string
  contentType: string
  sizeBytes: number
  uploadedAt: string
}

export interface CourseStudent {
  id: number
  studentCode: string
  name: string
  email: string
  joinedAt: string
  status: 'ACTIVE'
}

export interface CourseStudentCandidate {
  id: number
  studentCode: string
  name: string
  email: string
}

export interface CourseProblem {
  id: string
  slug: string
  title: string
  summary: string
  topic: import('../../programming-problems/types/programmingProblem').ProblemTopic
  difficulty: import('../../programming-problems/types/programmingProblem').ProblemDifficulty
  allowedLanguages: import('../../programming-problems/types/programmingProblem').SubmissionLanguage[]
  assignedAt: string
  solved: boolean
}

export interface StudentCourse {
  id: string
  code: string
  title: string
  description: string
  teacherName: string
  startDate: string
  endDate: string | null
  status: CourseStatus
  solvedProblems: number
  totalProblems: number
  progressPercent: number
}

export interface StudentCourseDetails {
  course: StudentCourse
  problems: CourseProblem[]
}
