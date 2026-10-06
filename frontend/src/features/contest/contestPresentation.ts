export function verdictLabel(status: string): string {
  const labels: Record<string, string> = {
    ACCEPTED: 'Accepted', WRONG_ANSWER: 'Wrong Answer', TIME_LIMIT: 'Time Limit Exceeded',
    MEMORY_LIMIT: 'Memory Limit Exceeded', RUNTIME_ERROR: 'Runtime Error', COMPILE_ERROR: 'Compilation Error',
    NOT_JUDGED: 'Not judged',
  }
  return labels[status] ?? status
}
export function languageLabel(language: string): string {
  return ({ CPP: 'C++', JAVA: 'Java', PYTHON: 'Python', HTML: 'Web', MYSQL: 'MySQL' } as Record<string, string>)[language] ?? language
}
export type SubmissionFilter = 'All' | 'Accepted' | 'Wrong Answer' | 'Other'
export function matchesSubmissionFilter(status: string, filter: SubmissionFilter): boolean {
  return filter === 'All' || (filter === 'Accepted' ? status === 'ACCEPTED'
    : filter === 'Wrong Answer' ? status === 'WRONG_ANSWER' : !['ACCEPTED', 'WRONG_ANSWER'].includes(status))
}
