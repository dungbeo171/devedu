export interface CourseSummary {
  id: string
  slug: string
  title: string
  description: string
  createdAt: string
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
