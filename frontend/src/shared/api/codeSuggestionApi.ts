type CodeSuggestionLanguage = 'CPP' | 'JAVA' | 'PYTHON' | 'HTML' | 'MYSQL' | 'CSS' | 'JAVASCRIPT'

interface CodeSuggestionResponse {
  suggestion: string
}

export async function requestCodeSuggestion(
  language: CodeSuggestionLanguage,
  source: string,
  cursor: number,
  context: string,
  signal: AbortSignal,
): Promise<string> {
  const accessToken = localStorage.getItem('devedu.accessToken') ?? localStorage.getItem('accessToken')
  if (!accessToken) throw new Error('Hãy đăng nhập để dùng AI gợi ý.')

  const response = await fetch('/api/code/suggestions', {
    method: 'POST',
    signal,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ language, source, cursor, context }),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    if (response.status === 401) throw new Error('Phiên đăng nhập đã hết hạn.')
    throw new Error(body?.message ?? 'AI chưa thể tạo gợi ý lúc này.')
  }
  const body = await response.json() as CodeSuggestionResponse
  return typeof body.suggestion === 'string' ? body.suggestion : ''
}
