import { useCallback, useMemo } from 'react'
import type { WorkspaceDraft } from '../../programming-problems/components/ProblemWorkspace'

export function useContestDrafts(namespace: string) {
  const key = 'devedu.contest.drafts.' + namespace
  const drafts = useMemo(() => {
    const map = new Map<string, WorkspaceDraft>()
    try {
      const rows: unknown = JSON.parse(sessionStorage.getItem(key) ?? '[]')
      if (Array.isArray(rows)) for (const row of rows) {
        if (Array.isArray(row) && typeof row[0] === 'string' && row[1] && typeof row[1].sourceCode === 'string'
          && typeof row[1].input === 'string' && ['CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL'].includes(row[1].language)) map.set(row[0], row[1])
      }
    } catch { /* Storage can be disabled; the in-memory draft still works. */ }
    return map
  }, [key])
  const persist = useCallback(() => {
    try { sessionStorage.setItem(key, JSON.stringify([...drafts])) } catch { /* Keep the in-memory draft. */ }
  }, [key, drafts])
  return { drafts, persist }
}
