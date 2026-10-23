import type { LeaderboardPeriod, ProblemLeaderboard } from '../types/problemLeaderboard'

export async function getProblemLeaderboard(period: LeaderboardPeriod): Promise<ProblemLeaderboard> {
  const response = await fetch(`/api/leaderboards/problems?period=${encodeURIComponent(period)}`)
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message ?? 'Không thể tải bảng xếp hạng.')
  }
  return response.json() as Promise<ProblemLeaderboard>
}
