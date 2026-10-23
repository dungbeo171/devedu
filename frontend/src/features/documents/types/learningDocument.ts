export interface LearningDocumentSummary {
  slug: string
  title: string
  summary: string
  content: null
  tags: string[]
  readingMinutes: number
  priority: number
  createdAt: string
  recommendationReason: string | null
}

export interface LearningDocumentDetail extends Omit<LearningDocumentSummary, 'content'> {
  content: string
}

export interface LearningDocumentEditorAccess {
  canEdit: boolean
}

export interface LearningDocumentEditorPermission {
  userId: number
  name: string
  email: string
  enabled: boolean
}

export interface UpdateLearningDocumentInput {
  title: string
  summary: string
  content: string
  tags: string[]
  readingMinutes: number
  priority: number
}
