export type InterviewTopic = 'JAVA' | 'PYTHON' | 'CPP' | 'OOP' | 'SQL' | 'DATABASE' | 'DATA_STRUCTURES' | 'ALGORITHMS' | 'WEB'
export type InterviewDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface InterviewQuestionSummary { id: string; question: string; difficulty: InterviewDifficulty; topic: InterviewTopic }
export interface InterviewQuestionDetail extends InterviewQuestionSummary { answer: string; explanation: string; createdAt: string }

export const interviewTopicLabels: Record<InterviewTopic, string> = {
  JAVA: 'Java', PYTHON: 'Python', CPP: 'C++', OOP: 'OOP', SQL: 'SQL', DATABASE: 'Database',
  DATA_STRUCTURES: 'Data Structures', ALGORITHMS: 'Algorithms', WEB: 'Web',
}
export const difficultyLabels: Record<InterviewDifficulty, string> = { EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' }
