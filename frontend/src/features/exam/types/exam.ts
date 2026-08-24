export type ExamQuestionType = 'MULTIPLE_CHOICE' | 'CODING'
export type CodeLanguage = 'CPP' | 'JAVA' | 'PYTHON' | 'HTML' | 'MYSQL'
export type ExamAttemptStatus = 'IN_PROGRESS' | 'SUBMITTED'

export interface ExamSummary { id: string; slug: string; title: string; description: string; scheduledAt: string; durationMinutes: number; createdAt: string }
export interface ExamQuestion { id: string; type: ExamQuestionType; prompt: string; options: string[]; codingLanguage: CodeLanguage | null; points: number; position: number }
export interface ExamDetail extends Omit<ExamSummary, 'createdAt'> { questions: ExamQuestion[] }
export interface ExamAnswer { id: string; questionId: string; selectedOptionIndex: number | null; sourceCode: string | null; answeredAt: string }
export interface ExamSession { attemptId: string; status: ExamAttemptStatus; startedAt: string; expiresAt: string; exam: ExamDetail; answers: ExamAnswer[] }
export interface ExamResult { attemptId: string; examId: string; submittedAt: string; automaticScore: number; automaticMaxScore: number; pendingCodingQuestions: number; answers: ExamAnswer[] }
export interface TeacherExamResult { attemptId: string; studentId: string; status: ExamAttemptStatus; startedAt: string; submittedAt: string | null; automaticScore: number; automaticMaxScore: number; pendingCodingQuestions: number; answers: ExamAnswer[] }
