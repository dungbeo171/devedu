import { getStoredAccessToken } from '../../auth/api/authApi'
import type {
  LearningDocumentDetail,
  LearningDocumentEditorAccess,
  LearningDocumentEditorPermission,
  LearningDocumentSummary,
  UpdateLearningDocumentInput,
} from '../types/learningDocument'

async function errorMessage(response: Response, fallback: string) {
  const body = await response.json().catch(() => null) as { message?: string } | null
  return body?.message ?? fallback
}

export async function getLearningDocuments(): Promise<LearningDocumentSummary[]> {
  const response = await fetch('/api/documents')
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể tải thư viện tài liệu.'))
  return response.json() as Promise<LearningDocumentSummary[]>
}

export async function getDocumentRecommendations(): Promise<LearningDocumentSummary[]> {
  const token = getStoredAccessToken()
  const response = await fetch('/api/documents/recommendations', {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể tải tài liệu đề xuất.'))
  return response.json() as Promise<LearningDocumentSummary[]>
}

export async function getLearningDocument(slug: string): Promise<LearningDocumentDetail> {
  const response = await fetch(`/api/documents/${encodeURIComponent(slug)}`)
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể tải tài liệu.'))
  return response.json() as Promise<LearningDocumentDetail>
}

export async function recordLearningDocumentView(slug: string): Promise<void> {
  const token = getStoredAccessToken()
  if (!token) return
  const response = await fetch(`/api/documents/${encodeURIComponent(slug)}/views`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể ghi nhận lượt xem tài liệu.'))
}

function authenticatedHeaders(json = false) {
  const token = getStoredAccessToken()
  if (!token) throw new Error('Bạn cần đăng nhập để thực hiện thao tác này.')
  return { Authorization: `Bearer ${token}`, ...(json ? { 'Content-Type': 'application/json' } : {}) }
}

export async function getLearningDocumentEditorAccess(): Promise<LearningDocumentEditorAccess> {
  const response = await fetch('/api/teacher/documents/access', { headers: authenticatedHeaders() })
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể kiểm tra quyền chỉnh sửa tài liệu.'))
  return response.json() as Promise<LearningDocumentEditorAccess>
}

export async function updateLearningDocument(slug: string, input: UpdateLearningDocumentInput): Promise<LearningDocumentDetail> {
  const response = await fetch(`/api/teacher/documents/${encodeURIComponent(slug)}`, {
    method: 'PUT',
    headers: authenticatedHeaders(true),
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể cập nhật tài liệu.'))
  return response.json() as Promise<LearningDocumentDetail>
}

export async function getLearningDocumentEditorPermissions(): Promise<LearningDocumentEditorPermission[]> {
  const response = await fetch('/api/admin/documents/editors', { headers: authenticatedHeaders() })
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể tải quyền chỉnh sửa tài liệu.'))
  return response.json() as Promise<LearningDocumentEditorPermission[]>
}

export async function setLearningDocumentEditorPermission(userId: number, enabled: boolean): Promise<LearningDocumentEditorPermission> {
  const response = await fetch(`/api/admin/documents/editors/${encodeURIComponent(userId)}`, {
    method: 'PUT',
    headers: authenticatedHeaders(true),
    body: JSON.stringify({ enabled }),
  })
  if (!response.ok) throw new Error(await errorMessage(response, 'Không thể cập nhật quyền chỉnh sửa tài liệu.'))
  return response.json() as Promise<LearningDocumentEditorPermission>
}
