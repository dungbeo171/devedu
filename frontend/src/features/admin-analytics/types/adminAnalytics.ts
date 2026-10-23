export interface AdminAnalytics {
  generatedAt: string
  users: { total: number; students: number; teachers: number; admins: number; newLast7Days: number }
  problems: { active: number; deleted: number; easy: number; medium: number; hard: number }
  activity: {
    submissions: number; accepted: number; uniqueSubmitters: number; submissionsLast24Hours: number
    submissionsLast7Days: number; averageRuntimeMillis: number; testRuns: number; successfulTestRuns: number
    drafts: number; uniqueSolvers: number
  }
  contests: {
    total: number; publicContests: number; privateContests: number; drafts: number; upcoming: number
    ongoing: number; finished: number; monitored: number; registrations: number; officialSubmissions: number
    virtualContests: number; uniqueParticipants: number
  }
  monitoring: {
    behaviorEvents: number; lookingAway: number; tabHidden: number; problemCopied: number; contentPasted: number
    screenshotAttempts: number; activeCameras: number
  }
  dailyActivity: { date: string; newUsers: number; submissions: number; accepted: number }[]
  verdicts: Metric[]
  languages: Metric[]
  topics: Metric[]
  topProblems: { slug: string; title: string; submissions: number; accepted: number; acceptanceRate: number }[]
}

export interface Metric { key: string; value: number }
