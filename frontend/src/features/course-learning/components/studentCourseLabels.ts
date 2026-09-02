import type { ProblemDifficulty, ProblemTopic } from '../../programming-problems/types/programmingProblem'

export const difficultyLabels: Record<ProblemDifficulty, string> = { EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' }
export const topicLabels: Record<ProblemTopic, string> = {
  INTRODUCTION: 'Nhập môn', CPP: 'C++', JAVA: 'Java', PYTHON: 'Python', OOP: 'OOP',
  DATA_STRUCTURES: 'Cấu trúc dữ liệu', ALGORITHMS: 'Thuật toán', SQL: 'SQL',
}
