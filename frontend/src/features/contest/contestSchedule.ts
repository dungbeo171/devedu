export function contestSchedule(startsAt: string, endsAt: string, durationMinutes: number) {
  const start = new Date(startsAt), end = new Date(endsAt)
  if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime())) {
    throw new Error('Vui lòng nhập thời gian bắt đầu và kết thúc hợp lệ.')
  }
  if (end.getTime() <= start.getTime()) throw new Error('Thời gian kết thúc phải sau thời gian bắt đầu.')
  if (!Number.isInteger(durationMinutes) || durationMinutes < 1 || durationMinutes > 10080) {
    throw new Error('Thời gian làm phải là số nguyên từ 1 đến 10.080 phút.')
  }
  return { startsAt: start.toISOString(), endsAt: end.toISOString(), durationMinutes }
}
