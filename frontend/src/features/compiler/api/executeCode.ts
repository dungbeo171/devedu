import type {
  CodeExecutionRequest,
  CodeExecutionResponse,
} from '../types/codeExecution'

export async function executeCode(
  request: CodeExecutionRequest,
): Promise<CodeExecutionResponse> {
  const response = await fetch('/api/code/execute', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(error?.message ?? 'Không thể kết nối tới Code Execution API.')
  }

  return response.json() as Promise<CodeExecutionResponse>
}
