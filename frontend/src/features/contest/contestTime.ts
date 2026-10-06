import type { ContestStatus } from './types/contest'

export function contestStatus(contest: { startsAt: string; endsAt: string }, now: number): ContestStatus {
  if (now < Date.parse(contest.startsAt)) return 'UPCOMING'
  return now < Date.parse(contest.endsAt) ? 'ONGOING' : 'FINISHED'
}
export function countdown(milliseconds: number): string {
  const seconds = Math.max(0, Math.ceil(milliseconds / 1000))
  const hours = Math.floor(seconds / 3600)
  return [hours, Math.floor(seconds / 60) % 60, seconds % 60].map(n => String(n).padStart(2, '0')).join(':')
}
export function startsIn(milliseconds: number): string {
  const minutes = Math.max(0, Math.ceil(milliseconds / 60000))
  return `${Math.floor(minutes / 1440)}d ${String(Math.floor(minutes / 60) % 24).padStart(2, '0')}h ${String(minutes % 60).padStart(2, '0')}m`
}
export function endedAgo(milliseconds: number): string {
  const minutes = Math.max(0, Math.floor(milliseconds / 60000))
  if (minutes < 1) return 'Ended just now'
  if (minutes < 60) return `Ended ${minutes} min ago`
  if (minutes < 1440) return `Ended ${Math.floor(minutes / 60)} hours ago`
  return `Ended ${Math.floor(minutes / 1440)} days ago`
}
export function formatStart(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
export function durationLabel(minutes: number) {
  return `${Math.floor(minutes / 60) ? `${Math.floor(minutes / 60)}h ` : ''}${minutes % 60 ? `${minutes % 60}m` : ''}`.trim()
}
