export type CodeLanguage = 'CPP' | 'JAVA' | 'PYTHON' | 'HTML' | 'MYSQL'

export interface CodeExecutionRequest {
  language: CodeLanguage
  code: string
  input: string
}

export interface CodeExecutionResponse {
  language: CodeLanguage
  status: 'SUCCESS' | 'COMPILE_ERROR' | 'RUNTIME_ERROR' | 'TIME_LIMIT'
  output: string
}
