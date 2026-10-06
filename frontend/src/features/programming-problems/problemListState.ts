import type { ProblemDifficulty, ProblemTopic, SubmissionLanguage } from './types/programmingProblem'

export type ProgressFilter = '' | 'SOLVED' | 'UNSOLVED'
export interface ProblemListState {
  topic: ProblemTopic | null
  difficulty: ProblemDifficulty | ''
  language: SubmissionLanguage | ''
  progress: ProgressFilter
  search: string
  page: number
}

function option<T extends string>(value: string | null, allowed: readonly T[]): T | undefined {
  return allowed.find(item => item === value)
}

export function readProblemListState(query: string): ProblemListState {
  const params = new URLSearchParams(query)
  const page = Number(params.get('page'))
  return {
    topic: option(params.get('topic'), ['INTRODUCTION', 'CPP', 'JAVA', 'PYTHON', 'OOP', 'DATA_STRUCTURES', 'ALGORITHMS', 'SQL'] as const) ?? null,
    difficulty: option(params.get('difficulty'), ['EASY', 'MEDIUM', 'HARD'] as const) ?? '',
    language: option(params.get('language'), ['CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL'] as const) ?? '',
    progress: option(params.get('progress'), ['SOLVED', 'UNSOLVED'] as const) ?? '',
    search: params.get('q') ?? '',
    page: Number.isSafeInteger(page) && page > 0 ? page : 1,
  }
}

export function problemListUrl(state: ProblemListState): string {
  const params = new URLSearchParams()
  if (state.topic) params.set('topic', state.topic)
  if (state.difficulty) params.set('difficulty', state.difficulty)
  if (state.language) params.set('language', state.language)
  if (state.progress) params.set('progress', state.progress)
  if (state.search) params.set('q', state.search)
  if (state.page > 1) params.set('page', String(state.page))
  const query = params.toString()
  return `/problems${query ? `?${query}` : ''}`
}
