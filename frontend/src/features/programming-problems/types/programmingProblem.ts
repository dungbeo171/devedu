export type ProblemTopic =
  | 'INTRODUCTION'
  | 'CPP'
  | 'JAVA'
  | 'PYTHON'
  | 'OOP'
  | 'DATA_STRUCTURES'
  | 'ALGORITHMS'
  | 'SQL'

export type SubmissionLanguage = 'CPP' | 'JAVA' | 'PYTHON' | 'HTML' | 'MYSQL'
export type ProblemDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface ProgrammingProblemSummary {
  id: string
  slug: string
  title: string
  summary: string
  topic: ProblemTopic
  difficulty: ProblemDifficulty
  allowedLanguages: SubmissionLanguage[]
}

export interface ProgrammingProblemDetail extends ProgrammingProblemSummary {
  description: string
  sampleInput: string
  sampleOutput: string
  createdAt: string
}

export interface ProblemCodeExecution {
  language: SubmissionLanguage
  status: 'SUCCESS' | 'COMPILE_ERROR' | 'RUNTIME_ERROR' | 'TIME_LIMIT'
  output: string
}

export interface ProblemSubmission {
  id: string
  problemId: string
  language: SubmissionLanguage
  status: 'ACCEPTED' | 'WRONG_ANSWER' | 'COMPILE_ERROR' | 'RUNTIME_ERROR' | 'TIME_LIMIT'
  diagnostic: string
  passedTests: number
  totalTests: number
  executionTimeMillis: number
  submittedAt: string
}

export interface ProgrammingProblemDraft {
  problemId: string
  language: SubmissionLanguage
  sourceCode: string
  input: string
  updatedAt: string
}

export const topicLabels: Record<ProblemTopic, string> = {
  INTRODUCTION: 'Nhập môn lập trình',
  CPP: 'C++',
  JAVA: 'Java',
  PYTHON: 'Python',
  OOP: 'OOP',
  DATA_STRUCTURES: 'Data Structures',
  ALGORITHMS: 'Algorithms',
  SQL: 'SQL',
}
