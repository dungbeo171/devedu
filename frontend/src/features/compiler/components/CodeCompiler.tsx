import { useState } from 'react'
import { SmartCodeEditor } from '../../../shared/components/SmartCodeEditor'
import { executeCode } from '../api/executeCode'
import type { CodeLanguage } from '../types/codeExecution'

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

const initialOutput = 'Nhấn Run để biên dịch và chạy code trong Docker sandbox.'

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
    setOutput('Đang gửi yêu cầu...')
    try {
      const effectiveInput = input.trim() ? input : selectedLanguage.defaultInput
      const result = await executeCode({ language, code, input: effectiveInput })
      setOutput(`[${result.status}]\n${result.output}`)
    } catch (error) {
      setOutput(error instanceof Error ? error.message : 'Đã có lỗi xảy ra.')
    } finally {
      setIsRunning(false)
    }
  }

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/80 shadow-2xl shadow-slate-950/50">
      <div className="flex flex-col gap-4 border-b border-slate-800 bg-slate-900 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-center gap-3">
          <div className="hidden gap-1.5 sm:flex" aria-hidden="true">
            <span className="h-2.5 w-2.5 rounded-full bg-rose-400/80" />
            <span className="h-2.5 w-2.5 rounded-full bg-amber-300/80" />
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-400/80" />
          </div>
          <span className="hidden h-5 w-px bg-slate-700 sm:block" />
          <span className="truncate font-mono text-xs text-slate-400">
            workspace / <span className="text-slate-200">{selectedLanguage.extension}</span>
          </span>
        </div>

        <div className="flex items-center gap-2">
          <label className="sr-only" htmlFor="language">Chọn ngôn ngữ</label>
          <select
            id="language"
            value={language}
            onChange={(event) => changeLanguage(event.target.value as CodeLanguage)}
            className="min-w-28 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm font-medium text-slate-100 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/15"
          >
            {languages.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
          <button
            type="button"
            onClick={() => void runCode()}
            disabled={!code.trim() || isRunning}
            className="inline-flex min-w-24 items-center justify-center gap-2 rounded-lg bg-cyan-400 px-4 py-2 text-sm font-bold text-slate-950 transition hover:bg-cyan-300 focus:outline-none focus:ring-2 focus:ring-cyan-300 focus:ring-offset-2 focus:ring-offset-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <svg aria-hidden="true" className="h-3.5 w-3.5" viewBox="0 0 16 16" fill="currentColor">
              <path d="M3.5 2.8a1 1 0 0 1 1.52-.85l8 5.2a1 1 0 0 1 0 1.7l-8 5.2A1 1 0 0 1 3.5 13.2V2.8Z" />
            </svg>
            {isRunning ? 'Đang chạy' : 'Run'}
          </button>
        </div>
      </div>

      <div className="grid min-h-[560px] lg:grid-cols-[minmax(0,1.65fr)_minmax(300px,0.75fr)]">
        <div className="flex min-h-[420px] flex-col border-b border-slate-800 lg:border-r lg:border-b-0">
          <div className="flex items-center justify-between gap-3 border-b border-slate-800/80 px-4 py-2.5">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Code editor</p>
            <div className="flex items-center gap-2">
              <span className="hidden text-[10px] text-slate-600 sm:inline">Tab: autocomplete · Enter: auto-indent · Ctrl+Z: hoàn tác</span>
              <span className="rounded bg-slate-800 px-2 py-1 font-mono text-[11px] text-slate-500">{selectedLanguage.label}</span>
            </div>
          </div>
          <SmartCodeEditor key={language} language={language} value={code} onChange={setCode} />
        </div>

        <div className="grid min-h-[360px] grid-rows-2 bg-slate-950/50">
          <div className="flex min-h-0 flex-col border-b border-slate-800">
            <div className="flex items-center justify-between px-4 py-2.5">
              <label className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400" htmlFor="program-input">
                Input (tùy chọn)
              </label>
              <span className="font-mono text-[11px] text-slate-600">
                {input.trim() ? 'stdin tùy chỉnh' : selectedLanguage.defaultInput ? `mặc định: ${selectedLanguage.defaultInput}` : 'mặc định: trống'}
              </span>
            </div>
            <textarea
              id="program-input"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder={selectedLanguage.defaultInput
                ? `Để trống để dùng input hệ thống: ${selectedLanguage.defaultInput}`
                : 'Không bắt buộc nhập dữ liệu đầu vào'}
              spellCheck={false}
              className="min-h-0 flex-1 resize-none border-t border-slate-800/70 bg-[#0b1018] p-4 font-mono text-sm leading-6 text-slate-300 outline-none placeholder:text-slate-700 focus:bg-slate-950"
            />
          </div>

          <div className="flex min-h-0 flex-col">
            <div className="flex items-center justify-between px-4 py-2.5">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Output</p>
              <span className="flex items-center gap-1.5 text-[11px] text-amber-300/80">
                <span className="h-1.5 w-1.5 rounded-full bg-amber-300" />
                Docker sandbox
              </span>
            </div>
            <pre aria-live="polite" className="min-h-0 flex-1 overflow-auto whitespace-pre-wrap border-t border-slate-800/70 bg-[#080c12] p-4 font-mono text-sm leading-6 text-slate-400">
              {output}
            </pre>
          </div>
        </div>
      </div>
    </section>
  )
}
