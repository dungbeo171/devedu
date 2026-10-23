import type { UserRole } from '../../auth/types/auth'

export type LeaderboardPeriod = 'WEEK' | 'MONTH' | 'YEAR'

export interface ProblemLeaderboardEntry {
  rank: number
  userId: number
  name: string
  role: UserRole
  score: number
  solved: number
  easySolved: number
  mediumSolved: number
  hardSolved: number
}

export interface ProblemLeaderboard {
  period: LeaderboardPeriod
  startsAt: string
  endsAt: string
  generatedAt: string
  scoring: {
    easy: number
    medium: number
    hard: number
  }
  entries: ProblemLeaderboardEntry[]
}
