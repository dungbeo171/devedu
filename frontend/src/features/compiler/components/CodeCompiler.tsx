import { useState } from 'react'
import { SmartCodeEditor } from '../../../shared/components/SmartCodeEditor'
import { executeCode } from '../api/executeCode'
import type { CodeLanguage } from '../types/codeExecution'
import { IconChevronDown, IconPlay, IconTerminal } from '../../../shared/components/Icons'

interface LanguageOption {
  value: CodeLanguage
  label: string
  extension: string
  sample: string
  defaultInput: string
}

const languages: LanguageOption[] = [
  {
    value: 'CPP',
    label: 'C++',
    extension: 'main.cpp',
    defaultInput: 'DevEdu',
    sample: `#include <iostream>
using namespace std;

int main() {
    string name;
    cin >> name;
    cout << "Hello, " << name << "!" << endl;
    return 0;
}`,
  },
  {
    value: 'JAVA',
    label: 'Java',
    extension: 'Main.java',
    defaultInput: 'DevEdu',
    sample: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        System.out.println("Hello, " + name + "!");
    }
}`,
  },
  {
    value: 'PYTHON',
    label: 'Python',
    extension: 'main.py',
    defaultInput: 'DevEdu',
    sample: `name = input()
print(f"Hello, {name}!")`,
  },
  {
    value: 'HTML',
    label: 'HTML',
    extension: 'index.html',
    defaultInput: '',
    sample: `<!doctype html>
<html lang="vi">
  <head>
    <meta charset="UTF-8" />
    <title>Hello DevEdu</title>
  </head>
  <body>
    <h1>Hello, world!</h1>
  </body>
</html>`,
  },
  {
    value: 'MYSQL',
    label: 'MySQL',
    extension: 'query.sql',
    defaultInput: '',
    sample: `SELECT
    'Hello, DevEdu!' AS message,
    CURRENT_DATE AS today;`,
  },
]

const initialOutput = 'Nhấn Chạy Code để thực thi chương trình.'

export function CodeCompiler() {
  const [language, setLanguage] = useState<CodeLanguage>('CPP')
  const [code, setCode] = useState(languages[0].sample)
  const [input, setInput] = useState('')
  const [output, setOutput] = useState(initialOutput)
  const [isRunning, setIsRunning] = useState(false)

  const selectedLanguage = languages.find((item) => item.value === language) ?? languages[0]

  function changeLanguage(nextLanguage: CodeLanguage) {
    const nextOption = languages.find((item) => item.value === nextLanguage)
    if (!nextOption) return

    setLanguage(nextLanguage)
    setCode(nextOption.sample)
    setInput('')
    setOutput(initialOutput)
  }

  async function runCode() {
    if (!code.trim() || isRunning) return

    setIsRunning(true)
    setOutput('Đang thực thi chương trình...')
    try {
      const effectiveInput = input.trim() ? input : selectedLanguage.defaultInput
      const result = await executeCode({ language, code, input: effectiveInput })
      setOutput(`[${result.status}]\n${result.output}`)
    } catch (error) {
      setOutput(error instanceof Error ? error.message : 'Đã có lỗi xảy ra khi thực thi.')
    } finally {
      setIsRunning(false)
    }
  }

  return (
    <section className="overflow-hidden rounded-3xl border border-blue-100 bg-white text-slate-900 shadow-xl shadow-blue-900/10">
      {/* IDE Top Window Bar */}
      <div className="flex flex-col gap-3 border-b border-blue-100 bg-blue-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-center gap-3">
          {/* macOS Style Traffic Lights */}
          <div className="flex items-center gap-1.5" aria-hidden="true">
            <span className="h-3 w-3 rounded-full bg-blue-300" />
            <span className="h-3 w-3 rounded-full bg-blue-500" />
            <span className="h-3 w-3 rounded-full bg-blue-700" />
          </div>
          <span className="hidden h-4 w-px bg-blue-200 sm:block" />
          <div className="flex items-center gap-2 rounded-xl border border-blue-100 bg-white px-3 py-1 text-xs">
            <IconTerminal className="h-3.5 w-3.5 text-blue-600" />
            <span className="truncate font-mono text-slate-500">
              workspace / <span className="font-semibold text-blue-700">{selectedLanguage.extension}</span>
            </span>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2.5">
          <div className="relative">
            <label className="sr-only" htmlFor="language">Chọn ngôn ngữ</label>
            <select
              id="language"
              value={language}
              onChange={(event) => changeLanguage(event.target.value as CodeLanguage)}
              className="appearance-none rounded-xl border border-blue-200 bg-white py-1.5 pl-3.5 pr-8 font-mono text-xs font-bold text-blue-700 outline-none transition hover:border-blue-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
            >
              {languages.map((item) => (
                <option key={item.value} value={item.value} className="bg-white text-blue-700">
                  {item.label}
                </option>
              ))}
            </select>
            <IconChevronDown className="pointer-events-none absolute right-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-blue-600" />
          </div>

          <button
            type="button"
            onClick={() => void runCode()}
            disabled={!code.trim() || isRunning}
            className="inline-flex min-w-32 items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-1.5 text-xs font-bold text-white shadow-md shadow-blue-600/20 transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-400 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isRunning ? (
              <>
                <svg className="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                <span>Đang chạy...</span>
              </>
            ) : (
              <>
                <IconPlay className="h-3.5 w-3.5" />
                <span>Chạy Code</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Editor & Terminal Layout */}
      <div className="grid min-h-[600px] lg:grid-cols-[minmax(0,1.65fr)_minmax(320px,0.85fr)]">
        {/* Left: Code Editor Panel */}
        <div className="flex min-h-[460px] flex-col border-b border-blue-100 lg:border-r lg:border-b-0">
          <div className="flex items-center justify-between gap-3 border-b border-blue-100 bg-white px-4 py-2 text-xs">
            <div className="flex items-center gap-2">
              <span className="font-mono text-[11px] font-bold uppercase tracking-wider text-slate-600">Editor</span>
              <span className="rounded-md bg-blue-50 px-2 py-0.5 font-mono text-[10px] font-bold text-blue-700 ring-1 ring-blue-100">
                {selectedLanguage.label}
              </span>
            </div>
          </div>
          <SmartCodeEditor key={language} language={language} value={code} onChange={setCode} />
        </div>

        {/* Right: Standard Input & Terminal Output */}
        <div className="grid min-h-[400px] grid-rows-2 bg-blue-950 dark-scroll">
          {/* Input Panel */}
          <div className="flex min-h-0 flex-col border-b border-blue-800">
            <div className="flex items-center justify-between border-b border-blue-800 bg-blue-900 px-4 py-2 text-xs">
              <label className="font-mono text-[11px] font-bold uppercase tracking-wider text-blue-100" htmlFor="program-input">
                Standard Input (stdin)
              </label>
              <span className="font-mono text-[10px] text-blue-200">
                {input.trim() ? 'tùy chỉnh' : selectedLanguage.defaultInput ? `mẫu: ${selectedLanguage.defaultInput}` : 'trống'}
              </span>
            </div>
            <textarea
              id="program-input"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder={selectedLanguage.defaultInput
                ? `Để trống để dùng input mặc định: ${selectedLanguage.defaultInput}`
                : 'Không bắt buộc nhập dữ liệu đầu vào'}
              spellCheck={false}
              className="min-h-0 flex-1 resize-none bg-blue-950 p-4 font-mono text-xs leading-6 text-white outline-none placeholder:text-blue-300 focus:bg-blue-900"
            />
          </div>

          {/* Terminal Output Panel */}
          <div className="flex min-h-0 flex-col bg-blue-950">
            <div className="flex items-center justify-between border-b border-blue-800 bg-blue-900 px-4 py-2 text-xs">
              <div className="flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-blue-400 shadow-sm shadow-blue-400/50" />
                <span className="font-mono text-[11px] font-bold uppercase tracking-wider text-blue-100">Terminal Output</span>
              </div>
            </div>
            <pre
              aria-live="polite"
              className="min-h-0 flex-1 overflow-auto whitespace-pre-wrap p-4 font-mono text-xs leading-6 text-white selection:bg-blue-600/30"
            >
              {output}
            </pre>
          </div>
        </div>
      </div>
    </section>
  )
}
