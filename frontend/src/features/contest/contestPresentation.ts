export function verdictLabel(status: string): string {
  const labels: Record<string, string> = {
    ACCEPTED: 'Chính xác', WRONG_ANSWER: 'Sai kết quả', TIME_LIMIT: 'Quá thời gian',
    MEMORY_LIMIT: 'Vượt giới hạn bộ nhớ', RUNTIME_ERROR: 'Lỗi thực thi', COMPILE_ERROR: 'Lỗi biên dịch',
    NOT_JUDGED: 'Chưa chấm',
  }
  return labels[status] ?? status
}
export function languageLabel(language: string): string {
  return ({ CPP: 'C++', JAVA: 'Java', PYTHON: 'Python', HTML: 'Web', MYSQL: 'MySQL' } as Record<string, string>)[language] ?? language
}
export function contestErrorLabel(message: string): string {
  const labels: Record<string, string> = {
    'Contest not found': 'Không tìm thấy cuộc thi.',
    'User not found': 'Không tìm thấy người dùng.',
    'Sign in to participate': 'Vui lòng đăng nhập để tham gia.',
    'Only teachers and admins can create contests': 'Chỉ giáo viên và quản trị viên được tạo cuộc thi.',
    'Contest is required': 'Vui lòng nhập thông tin cuộc thi.',
    'Start time must be in the future': 'Thời gian bắt đầu phải ở tương lai.',
    'One or more problems are unavailable': 'Một hoặc nhiều bài tập không còn khả dụng.',
    'Contest has ended': 'Cuộc thi đã kết thúc.',
    'Contest is not accepting submissions': 'Cuộc thi hiện không nhận bài nộp.',
    'Register for this contest before submitting': 'Vui lòng đăng ký cuộc thi trước khi nộp bài.',
    'Problem is not part of this contest': 'Bài tập không thuộc cuộc thi này.',
    'Problem is unavailable': 'Bài tập không khả dụng.',
    'Problem unavailable': 'Bài tập không khả dụng.',
    'Language and code (up to 100000 characters) are required': 'Chọn ngôn ngữ và nhập mã nguồn, tối đa 100.000 ký tự.',
    'Invalid code or language': 'Mã nguồn hoặc ngôn ngữ không hợp lệ.',
    'Invalid sort': 'Lựa chọn sắp xếp không hợp lệ.',
    'Invalid page': 'Số trang không hợp lệ.',
    'Virtual is available only after the official contest ends': 'Chỉ có thể thi thử sau khi cuộc thi chính thức kết thúc.',
    'No virtual attempt yet': 'Bạn chưa bắt đầu lượt thi thử nào.',
    'Virtual attempt not found': 'Không tìm thấy lượt thi thử.',
    'Virtual contest has ended': 'Lượt thi thử đã kết thúc.',
    'Camera events require a live official contest': 'Chỉ ghi nhận camera khi cuộc thi chính thức đang diễn ra.',
    'Register before enabling the camera': 'Đăng ký cuộc thi trước khi bật camera.',
    'Only the contest owner or an admin can view camera reports': 'Chỉ giáo viên tạo cuộc thi hoặc quản trị viên được xem báo cáo camera.',
    'Sign in to use contest camera features': 'Đăng nhập để dùng camera trong cuộc thi.',
    'Event ID already used': 'Mã sự kiện đã được sử dụng.',
    'Camera event limit reached; wait before retrying': 'Đã đạt giới hạn sự kiện camera. Vui lòng chờ trước khi thử lại.',
    'Forbidden': 'Bạn không có quyền thực hiện thao tác này.',
  }
  return labels[message] ?? message
}
export type SubmissionFilter = 'All' | 'Accepted' | 'Wrong Answer' | 'Other'
export const submissionFilterLabels: Record<SubmissionFilter, string> = {
  All: 'Tất cả', Accepted: 'Chính xác', 'Wrong Answer': 'Sai kết quả', Other: 'Khác',
}
export function difficultyLabel(difficulty: string | null): string {
  return ({ BEGINNER: 'Dễ', INTERMEDIATE: 'Trung bình', ADVANCED: 'Khó', EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' } as Record<string, string>)[difficulty ?? ''] ?? 'Chưa xác định'
}
export function scoringRuleLabel(value: string): string {
  // Translate known descriptions; preserve any future backend rule unchanged.
  return ({
    'Mỗi bài Accepted được cộng toàn bộ điểm một lần.': 'Mỗi bài giải đúng được cộng toàn bộ điểm một lần.',
    'Không cộng thời gian phạt cho lượt sai. Cột Time là thời gian giải bài cuối cùng kể từ lúc bắt đầu.': 'Không cộng thời gian phạt cho lượt sai. Cột Thời gian là thời gian giải bài cuối cùng kể từ lúc bắt đầu.',
  } as Record<string, string>)[value] ?? value
}
export function matchesSubmissionFilter(status: string, filter: SubmissionFilter): boolean {
  return filter === 'All' || (filter === 'Accepted' ? status === 'ACCEPTED'
    : filter === 'Wrong Answer' ? status === 'WRONG_ANSWER' : !['ACCEPTED', 'WRONG_ANSWER'].includes(status))
}
