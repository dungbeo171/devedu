import { useMemo, useRef, useState, type ChangeEvent } from 'react'
import { createProgrammingProblem } from '../api/programmingProblemsApi'
import type {
  CreateProgrammingProblem,
  ProblemDifficulty,
  ProblemTopic,
  SubmissionLanguage,
} from '../types/programmingProblem'

const topics: ProblemTopic[] = [
  'INTRODUCTION', 'CPP', 'JAVA', 'PYTHON', 'OOP', 'DATA_STRUCTURES', 'ALGORITHMS', 'SQL',
]
const difficulties: ProblemDifficulty[] = ['EASY', 'MEDIUM', 'HARD']
const languages: SubmissionLanguage[] = ['CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL']
const maximumFileBytes = 2 * 1024 * 1024
const maximumBatchSize = 100

const sampleJson = JSON.stringify([
  {
    slug: 'tong-ba-so',
    title: 'Tổng ba số',
    summary: 'Tính tổng của ba số nguyên.',
    description: 'Tính tổng của ba số nguyên.',
    inputDescription: 'Dòng đầu chứa ba số nguyên a, b, c.',
    outputDescription: 'In ra tổng a + b + c trên một dòng.',
    sampleInput: '1 2 3',
    sampleOutput: '6',
    topic: 'INTRODUCTION',
    difficulty: 'EASY',
    allowedLanguages: ['CPP', 'JAVA', 'PYTHON'],
    starterCodes: {
      CPP: '#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}',
      JAVA: 'public class Main {\n    public static void main(String[] args) {\n    }\n}',
      PYTHON: 'def solve():\n    pass\n\nsolve()',
    },
    testCases: [
      { input: '1 2 3', expectedOutput: '6', timeLimitMillis: 1000 },
      { input: '-5 2 8', expectedOutput: '5', timeLimitMillis: 1000 },
      { input: '0 0 0', expectedOutput: '0', timeLimitMillis: 1000 },
    ],
  },
], null, 2)

interface ParseResult {
  problems: CreateProgrammingProblem[]
  error: string
}

export function ProblemJsonImporter({ onImported }: { onImported: (count: number) => void }) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [json, setJson] = useState('')
  const [fileName, setFileName] = useState('')
  const [importing, setImporting] = useState(false)
  const [requestError, setRequestError] = useState('')
  const parsed = useMemo(() => parseProblems(json), [json])

  async function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setRequestError('')
    if (!file.name.toLowerCase().endsWith('.json')) {
      setRequestError('Chỉ chấp nhận file có định dạng .json.')
      return
    }
    if (file.size > maximumFileBytes) {
      setRequestError('File JSON không được vượt quá 2 MB.')
      return
    }
    try {
      setJson(await file.text())
      setFileName(file.name)
    } catch {
      setRequestError('Không thể đọc file JSON đã chọn.')
    }
  }

  async function importProblems() {
    setRequestError('')
    const result = parseProblems(json)
    if (result.error) {
      setRequestError(result.error)
      return
    }
    setImporting(true)
    let imported = 0
    try {
      for (const problem of result.problems) {
        try {
          await createProgrammingProblem(problem)
          imported++
        } catch (reason) {
          const message = reason instanceof Error ? reason.message : 'Không thể thêm bài tập.'
          setJson(JSON.stringify(result.problems.slice(imported), null, 2))
          setFileName('')
          throw new Error(`Đã thêm ${imported}/${result.problems.length} bài. Dừng tại “${problem.title}”: ${message}`)
        }
      }
      onImported(imported)
    } catch (reason) {
      setRequestError(reason instanceof Error ? reason.message : 'Không thể nhập danh sách bài tập.')
    } finally {
      setImporting(false)
    }
  }

  function downloadSample() {
    const url = URL.createObjectURL(new Blob([sampleJson], { type: 'application/json;charset=utf-8' }))
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'devedu-bai-tap-mau.json'
    anchor.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="ui-panel p-5 sm:p-7">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">Nhập bài tập bằng JSON</h1>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600">
            Dán một bài, một mảng bài tập hoặc chọn file JSON. Tối đa {maximumBatchSize} bài mỗi lần nhập.
          </p>
        </div>
        <button type="button" onClick={downloadSample} className="ui-button-secondary shrink-0">
          Tải JSON mẫu
        </button>
      </div>

      <div className="mt-5 rounded-xl border border-dashed border-blue-300 bg-blue-50/60 p-4">
        <input ref={fileInputRef} type="file" accept="application/json,.json" onChange={(event) => void chooseFile(event)} className="sr-only" />
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="text-sm font-bold text-slate-900">{fileName || 'Chọn file JSON từ máy'}</p>
            <p className="mt-1 text-xs text-slate-600">Định dạng .json, dung lượng tối đa 2 MB.</p>
          </div>
          <button type="button" onClick={() => fileInputRef.current?.click()} className="ui-button-primary shrink-0">
            Chọn file JSON
          </button>
        </div>
      </div>

      <label className="mt-5 block text-sm font-semibold text-slate-700">
        Nội dung JSON
        <textarea
          value={json}
          onChange={(event) => { setJson(event.target.value); setFileName(''); setRequestError('') }}
          rows={18}
          spellCheck={false}
          placeholder={sampleJson}
          className="ui-control mt-2 resize-y font-mono text-xs leading-5"
        />
      </label>

      {json.trim() && !parsed.error ? (
        <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 p-4">
          <p className="text-sm font-bold text-blue-800">Sẵn sàng nhập {parsed.problems.length} bài tập</p>
          <div className="mt-2 flex flex-wrap gap-2">
            {parsed.problems.slice(0, 8).map((problem) => (
              <span key={problem.slug} className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 ring-1 ring-blue-100">
                {problem.title}
              </span>
            ))}
            {parsed.problems.length > 8 ? <span className="px-2 py-1 text-xs font-semibold text-blue-700">+{parsed.problems.length - 8} bài</span> : null}
          </div>
        </div>
      ) : null}

      {(requestError || (json.trim() ? parsed.error : '')) ? (
        <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-semibold text-red-700">
          {requestError || parsed.error}
        </p>
      ) : null}

      <div className="mt-6 flex justify-end border-t border-slate-200 pt-5">
        <button
          type="button"
          onClick={() => void importProblems()}
          disabled={importing || !json.trim() || Boolean(parsed.error)}
          className="ui-button-primary"
        >
          {importing
            ? 'Đang nhập bài tập...'
            : parsed.problems.length ? `Nhập ${parsed.problems.length} bài tập` : 'Nhập bài tập'}
        </button>
      </div>
    </div>
  )
}

function parseProblems(source: string): ParseResult {
  if (!source.trim()) return { problems: [], error: '' }
  let value: unknown
  try {
    value = JSON.parse(source) as unknown
  } catch (reason) {
    const detail = reason instanceof SyntaxError ? reason.message : 'JSON không hợp lệ.'
    return { problems: [], error: `Không thể đọc JSON: ${detail}` }
  }

  const candidates = extractCandidates(value)
  if (!candidates) {
    return { problems: [], error: 'JSON phải là một bài tập, một mảng bài tập hoặc object có trường "problems".' }
  }
  if (candidates.length === 0) return { problems: [], error: 'Danh sách JSON chưa có bài tập.' }
  if (candidates.length > maximumBatchSize) return { problems: [], error: `Mỗi lần chỉ được nhập tối đa ${maximumBatchSize} bài tập.` }

  const problems: CreateProgrammingProblem[] = []
  const slugs = new Set<string>()
  for (let index = 0; index < candidates.length; index++) {
    try {
      const problem = parseProblem(candidates[index], index)
      if (slugs.has(problem.slug)) throw new Error(`slug “${problem.slug}” bị trùng trong file`)
      slugs.add(problem.slug)
      problems.push(problem)
    } catch (reason) {
      return { problems: [], error: reason instanceof Error ? reason.message : `Bài #${index + 1} không hợp lệ.` }
    }
  }
  return { problems, error: '' }
}

function extractCandidates(value: unknown): unknown[] | null {
  if (Array.isArray(value)) return value
  if (!isRecord(value)) return null
  if ('problems' in value) return Array.isArray(value.problems) ? value.problems : null
  return [value]
}

function parseProblem(value: unknown, index: number): CreateProgrammingProblem {
  const label = `Bài #${index + 1}`
  if (!isRecord(value)) throw new Error(`${label} phải là một object.`)
  const slug = requiredString(value.slug, `${label}: slug`)
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) throw new Error(`${label}: slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang.`)
  const topic = enumValue(value.topic, topics, `${label}: topic`)
  const difficulty = enumValue(value.difficulty, difficulties, `${label}: difficulty`)
  if (!Array.isArray(value.allowedLanguages) || value.allowedLanguages.length === 0) {
    throw new Error(`${label}: allowedLanguages phải có ít nhất một ngôn ngữ.`)
  }
  const allowedLanguages = [...new Set(value.allowedLanguages.map((language) =>
    enumValue(language, languages, `${label}: allowedLanguages`)))]
  if (!isRecord(value.starterCodes)) throw new Error(`${label}: starterCodes phải là một object.`)
  const starterCodes: Partial<Record<SubmissionLanguage, string>> = {}
  for (const language of allowedLanguages) {
    starterCodes[language] = requiredString(value.starterCodes[language], `${label}: starterCodes.${language}`)
  }
  if (!Array.isArray(value.testCases) || value.testCases.length < 3 || value.testCases.length > 50) {
    throw new Error(`${label}: testCases phải có từ 3 đến 50 phần tử.`)
  }
  const testCases = value.testCases.map((testCase, testIndex) => {
    if (!isRecord(testCase)) throw new Error(`${label}: testCases[${testIndex}] phải là một object.`)
    const timeLimitMillis = typeof testCase.timeLimitMillis === 'number' ? testCase.timeLimitMillis : 1000
    if (!Number.isInteger(timeLimitMillis) || timeLimitMillis < 100 || timeLimitMillis > 30000) {
      throw new Error(`${label}: testCases[${testIndex}].timeLimitMillis phải là số nguyên từ 100 đến 30000.`)
    }
    return {
      input: optionalString(testCase.input, `${label}: testCases[${testIndex}].input`),
      expectedOutput: optionalString(testCase.expectedOutput, `${label}: testCases[${testIndex}].expectedOutput`),
      timeLimitMillis,
    }
  })
  return {
    slug,
    title: requiredString(value.title, `${label}: title`),
    summary: requiredString(value.summary, `${label}: summary`),
    description: requiredString(value.description, `${label}: description`),
    inputDescription: requiredString(value.inputDescription, `${label}: inputDescription`),
    outputDescription: requiredString(value.outputDescription, `${label}: outputDescription`),
    sampleInput: optionalString(value.sampleInput, `${label}: sampleInput`),
    sampleOutput: optionalString(value.sampleOutput, `${label}: sampleOutput`),
    topic,
    difficulty,
    allowedLanguages,
    starterCodes,
    testCases,
  }
}

function requiredString(value: unknown, field: string): string {
  if (typeof value !== 'string' || !value.trim()) throw new Error(`${field} không được để trống.`)
  return value
}

function optionalString(value: unknown, field: string): string {
  if (value === undefined || value === null) return ''
  if (typeof value !== 'string') throw new Error(`${field} phải là chuỗi.`)
  return value
}

function enumValue<T extends string>(value: unknown, options: readonly T[], field: string): T {
  if (typeof value !== 'string' || !options.includes(value as T)) {
    throw new Error(`${field} không hợp lệ. Giá trị cho phép: ${options.join(', ')}.`)
  }
  return value as T
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
