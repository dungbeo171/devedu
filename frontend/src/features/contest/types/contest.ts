import type { ProblemDifficulty, ProblemSubmission, SubmissionLanguage } from '../../programming-problems/types/programmingProblem'

export type ContestStatus = 'UPCOMING' | 'ONGOING' | 'FINISHED'
export type ContestType = 'WEEKLY' | 'PRACTICE' | 'CUSTOM'
export type ContestDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
export type ContestVisibility = 'PUBLIC' | 'PRIVATE'
export type ContestParticipationMode = 'OPEN' | 'RESTRICTED'
export interface ContestSummary {
  id: string
  name: string
  type: ContestType
  startsAt: string
  endsAt: string
  durationMinutes: number
  problemCount: number
  participants: number
  status: ContestStatus
  difficulty: ContestDifficulty
  description: string
  monitoringEnabled: boolean
  createdAt: string
  participationMode: ContestParticipationMode
}
export interface ContestProblem {
  id: string
  slug: string | null
  title: string
  difficulty: ProblemDifficulty | null
  letter: string
  points: number
  solved: boolean
  submissions: number
  available: boolean
  contestOwned: boolean
}
export interface ContestStanding {
  rank: number
  userId: number
  name: string
  score: number
  solved: number
  timeSeconds: number
  problems: ContestProblemScore[]
}
export interface ContestProblemScore {
  problemId: string
  solved: boolean
  score: number
  attempts: number
  wrongAttempts: number
  solvedAtSeconds: number | null
}
export interface ContestScoringRules {
  name: string
  scoring: string
  penalty: string
  ranking: string
  wrongAttemptPenaltySeconds: number
  maxAttempts: number | null
  resubmissionAllowed: boolean
}
export interface ContestSubmission extends Omit<ProblemSubmission, 'diagnostic'> {
  letter: string
  title: string
  score: number
  maxScore: number
}
export interface ContestDetail {
  contest: ContestSummary
  serverTime: string
  registered: boolean
  myAttempt: { startsAt: string; endsAt: string } | null
  problems: ContestProblem[]
  leaderboard: ContestStanding[]
  myResult: ContestStanding | null
  mySubmissions: ContestSubmission[]
  scoringRules: ContestScoringRules
}
export interface ContestCatalog { contests: ContestSummary[]; serverTime: string }
export interface ManagedContestSummary extends ContestSummary {
  published: boolean
  visibility: ContestVisibility
  editable: boolean
}
export interface ManagedContestCatalog { contests: ManagedContestSummary[]; serverTime: string }
export interface CreateContest {
  name: string
  type: ContestType
  startsAt: string
  endsAt: string
  description: string
  monitoringEnabled: boolean
  durationMinutes: number
  problems?: { problemId: string; points: number }[]
  difficulty: ContestDifficulty
}
export interface ContestUserOption { id: number; name: string; email: string; role: 'STUDENT' | 'TEACHER'; code: string | null }
export interface ContestManage {
  id: string
  name: string
  type: ContestType
  startsAt: string
  endsAt: string
  durationMinutes: number
  difficulty: ContestDifficulty
  description: string
  monitoringEnabled: boolean
  published: boolean
  visibility: ContestVisibility
  participationMode: ContestParticipationMode
  problems: { problemId: string; points: number }[]
  proctors: ContestUserOption[]
  allowedEmails: string[]
  editable: boolean
  serverTime: string
  localProblems: { id: string; slug: string; title: string; difficulty: ProblemDifficulty }[]
}
export interface ContestResult { detail: ContestDetail; serverTime: string; finalized: boolean; wrongAttempts: number; averageSolveSeconds: number }
export interface VirtualSession { session: { id: string; contestId: string; startsAt: string; endsAt: string }; detail: ContestDetail; serverTime: string }
export interface ContestHistory { contestId: string; name: string; endedAt: string; rank: number; score: number; solved: number; timeSeconds: number }
export interface ContestProfile {
  userId: number; name: string; handle: string; serverTime: string;
  participated: number; wins: number; top10: number; top100: number;
  bestRank: number | null; averageRank: number; virtualContests: number; history: ContestHistory[];
  virtualHistory: { id: string; contestId: string; name: string; startsAt: string; endsAt: string }[];
}
export type ContestSubmit = (slug: string, language: SubmissionLanguage, sourceCode: string) => Promise<ProblemSubmission>
