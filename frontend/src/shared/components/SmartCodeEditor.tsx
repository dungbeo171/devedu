import { useEffect, useMemo, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'

export type EditorLanguage = 'CPP' | 'JAVA' | 'PYTHON' | 'HTML' | 'MYSQL'

interface SmartCodeEditorProps {
  editorId?: string
  language: EditorLanguage
  value: string
  onChange: (value: string) => void
}

interface Completion {
  label: string
  detail: string
  insertText: string
  cursorOffset?: number
  triggers?: string[]
}

interface HistoryEntry {
  value: string
  start: number
  end: number
}

const indent = '    '
const maxHistoryEntries = 200

const completions: Record<EditorLanguage, Completion[]> = {
  CPP: [
    { label: 'cout', detail: 'Standard output', insertText: 'cout << ;', cursorOffset: -1 },
    { label: 'cin', detail: 'Standard input', insertText: 'cin >> ;', cursorOffset: -1 },
    { label: '#include', detail: 'Include header', insertText: '#include <>', cursorOffset: -1, triggers: ['include'] },
    { label: 'int main', detail: 'Program entry point', insertText: 'int main() {\n    \n    return 0;\n}', cursorOffset: -18, triggers: ['main'] },
    { label: 'for', detail: 'For loop', insertText: 'for (int i = 0; i < n; i++) {\n    \n}', cursorOffset: -3 },
    { label: 'while', detail: 'While loop', insertText: 'while () {\n    \n}', cursorOffset: -10 },
  ],
  JAVA: [
    { label: 'System.out.println', detail: 'Print line', insertText: 'System.out.println();', cursorOffset: -2, triggers: ['sout', 'system'] },
    { label: 'public static void main', detail: 'Program entry point', insertText: 'public static void main(String[] args) {\n    \n}', cursorOffset: -3, triggers: ['main', 'public'] },
    { label: 'Scanner', detail: 'Read standard input', insertText: 'Scanner scanner = new Scanner(System.in);' },
    { label: 'for', detail: 'For loop', insertText: 'for (int i = 0; i < n; i++) {\n    \n}', cursorOffset: -3 },
    { label: 'if', detail: 'If statement', insertText: 'if () {\n    \n}', cursorOffset: -10 },
    { label: 'while', detail: 'While loop', insertText: 'while () {\n    \n}', cursorOffset: -10 },
  ],
  PYTHON: [
    { label: 'print', detail: 'Print a value', insertText: 'print()', cursorOffset: -1 },
    { label: 'input', detail: 'Read standard input', insertText: 'input()', cursorOffset: -1 },
    { label: 'range', detail: 'Create a number range', insertText: 'range()', cursorOffset: -1 },
    { label: 'len', detail: 'Get collection length', insertText: 'len()', cursorOffset: -1 },
    { label: 'def', detail: 'Define a function', insertText: 'def function_name():\n    pass', cursorOffset: -17 },
    { label: 'for', detail: 'For loop', insertText: 'for item in items:\n    ' },
    { label: 'if', detail: 'If statement', insertText: 'if condition:\n    ' },
    { label: 'while', detail: 'While loop', insertText: 'while condition:\n    ' },
    { label: 'import', detail: 'Import a module', insertText: 'import ' },
  ],
  HTML: [
    { label: 'div', detail: 'HTML container', insertText: '<div></div>', cursorOffset: -6 },
    { label: 'span', detail: 'Inline container', insertText: '<span></span>', cursorOffset: -7 },
    { label: 'button', detail: 'Button element', insertText: '<button type="button"></button>', cursorOffset: -9 },
    { label: 'input', detail: 'Input element', insertText: '<input type="text" />', cursorOffset: -3 },
    { label: 'script', detail: 'Script element', insertText: '<script></script>', cursorOffset: -9 },
    { label: 'section', detail: 'Section element', insertText: '<section></section>', cursorOffset: -10 },
  ],
  MYSQL: [
    { label: 'SELECT', detail: 'Read rows', insertText: 'SELECT  FROM ;', cursorOffset: -7 },
    { label: 'INSERT INTO', detail: 'Insert rows', insertText: 'INSERT INTO  () VALUES ();', cursorOffset: -13, triggers: ['insert'] },
    { label: 'UPDATE', detail: 'Update rows', insertText: 'UPDATE  SET  WHERE ;', cursorOffset: -13 },
    { label: 'DELETE FROM', detail: 'Delete rows', insertText: 'DELETE FROM  WHERE ;', cursorOffset: -8, triggers: ['delete'] },
    { label: 'CREATE TABLE', detail: 'Create a table', insertText: 'CREATE TABLE  (\n    \n);', cursorOffset: -8, triggers: ['create'] },
    { label: 'ORDER BY', detail: 'Sort result', insertText: 'ORDER BY  ASC', cursorOffset: -4, triggers: ['order'] },
  ],
}

export function SmartCodeEditor({ editorId = 'code-editor', language, value, onChange }: SmartCodeEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const historyRef = useRef<HistoryEntry[]>([{ value, start: 0, end: 0 }])
  const historyIndexRef = useRef(0)
  const [selection, setSelection] = useState({ start: 0, end: 0 })
  const [suggestionsOpen, setSuggestionsOpen] = useState(false)
  const [selectedSuggestion, setSelectedSuggestion] = useState(0)
  const [scroll, setScroll] = useState({ top: 0, left: 0 })

  const token = useMemo(() => tokenAt(value, selection.start), [value, selection.start])
  const availableCompletions = useMemo(
    () => mergeCompletions(declaredIdentifierCompletions(value, selection.start, language), completions[language]),
    [language, selection.start, value],
  )
  const suggestions = useMemo(() => {
    if (!suggestionsOpen || selection.start !== selection.end || token.query.length === 0) return []
    const query = token.query.toLowerCase()
    return availableCompletions
      .filter((completion) => {
        const candidates = [completion.label, ...(completion.triggers ?? [])]
        return candidates.some((candidate) => candidate.replace(/^#/, '').toLowerCase().startsWith(query))
          && completion.label.replace(/^#/, '').toLowerCase() !== query
      })
      .slice(0, 6)
  }, [availableCompletions, selection, suggestionsOpen, token.query])

  const lineNumbers = useMemo(
    () => Array.from({ length: value.split('\n').length }, (_, index) => index + 1),
    [value],
  )

  useEffect(() => {
    setSelectedSuggestion(0)
  }, [token.query, language])

  function updateSelection() {
    const editor = textareaRef.current
    if (!editor) return
    setSelection({ start: editor.selectionStart, end: editor.selectionEnd })
  }

  function applyValue(nextValue: string, cursorStart: number, cursorEnd = cursorStart, recordHistory = true) {
    if (recordHistory) {
      const currentEntry = historyRef.current[historyIndexRef.current]
      if (currentEntry?.value !== nextValue) {
        const nextHistory = historyRef.current.slice(0, historyIndexRef.current + 1)
        nextHistory.push({ value: nextValue, start: cursorStart, end: cursorEnd })
        if (nextHistory.length > maxHistoryEntries) nextHistory.shift()
        historyRef.current = nextHistory
        historyIndexRef.current = nextHistory.length - 1
      }
    }

    onChange(nextValue)
    setSelection({ start: cursorStart, end: cursorEnd })
    requestAnimationFrame(() => {
      textareaRef.current?.focus()
      textareaRef.current?.setSelectionRange(cursorStart, cursorEnd)
    })
  }

  function restoreHistory(direction: -1 | 1) {
    const nextIndex = historyIndexRef.current + direction
    const entry = historyRef.current[nextIndex]
    if (!entry) return
    historyIndexRef.current = nextIndex
    setSuggestionsOpen(false)
    applyValue(entry.value, entry.start, entry.end, false)
  }

  function acceptSuggestion(completion = suggestions[selectedSuggestion]) {
    if (!completion) return
    const nextValue = value.slice(0, token.start) + completion.insertText + value.slice(selection.end)
    const cursor = token.start + completion.insertText.length + (completion.cursorOffset ?? 0)
    setSuggestionsOpen(false)
    applyValue(nextValue, cursor)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    const shortcutKey = event.ctrlKey || event.metaKey
    if (shortcutKey && event.key.toLowerCase() === 'z') {
      event.preventDefault()
      restoreHistory(event.shiftKey ? 1 : -1)
      return
    }
    if (shortcutKey && event.key.toLowerCase() === 'y') {
      event.preventDefault()
      restoreHistory(1)
      return
    }
    if (shortcutKey && event.code === 'Space') {
      event.preventDefault()
      setSuggestionsOpen(true)
      return
    }

    if (suggestions.length > 0) {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault()
        const direction = event.key === 'ArrowDown' ? 1 : -1
        setSelectedSuggestion((current) => (current + direction + suggestions.length) % suggestions.length)
        return
      }
      if (event.key === 'Tab' && !event.shiftKey) {
        event.preventDefault()
        acceptSuggestion()
        return
      }
      if (event.key === 'Escape') {
        event.preventDefault()
        setSuggestionsOpen(false)
        return
      }
    }

    if (event.key === 'Tab') {
      event.preventDefault()
      indentSelection(event.shiftKey)
      return
    }
    if (event.key === 'Enter') {
      event.preventDefault()
      insertNewLine()
      return
    }
    if ((event.key === 'Backspace' || event.key === 'Delete') && !shortcutKey && !event.altKey) {
      event.preventDefault()
      deleteContent(event.key === 'Backspace')
      return
    }

    const pairs: Record<string, string> = { '(': ')', '[': ']', '{': '}', '"': '"', "'": "'", '`': '`' }
    const closingCharacters = Object.values(pairs)
    if (closingCharacters.includes(event.key) && value[selection.start] === event.key && selection.start === selection.end) {
      event.preventDefault()
      applyValue(value, selection.start + 1)
      return
    }
    const closing = pairs[event.key]
    if (closing && !event.ctrlKey && !event.metaKey && !event.altKey) {
      event.preventDefault()
      const selectedText = value.slice(selection.start, selection.end)
      const inserted = event.key + selectedText + closing
      const nextValue = value.slice(0, selection.start) + inserted + value.slice(selection.end)
      const start = selection.start + 1
      applyValue(nextValue, start, selectedText ? start + selectedText.length : start)
      setSuggestionsOpen(false)
    }
  }

  function indentSelection(removeIndent: boolean) {
    const lineStart = value.lastIndexOf('\n', Math.max(0, selection.start - 1)) + 1
    const lineEndIndex = value.indexOf('\n', selection.end)
    const lineEnd = lineEndIndex === -1 ? value.length : lineEndIndex
    const block = value.slice(lineStart, lineEnd)

    if (removeIndent) {
      const lines = block.split('\n')
      let removedBeforeStart = 0
      let removedTotal = 0
      const nextBlock = lines.map((line, index) => {
        const removed = line.match(/^ {1,4}/)?.[0].length ?? 0
        if (index === 0) removedBeforeStart = Math.min(removed, selection.start - lineStart)
        removedTotal += removed
        return line.slice(removed)
      }).join('\n')
      applyValue(
        value.slice(0, lineStart) + nextBlock + value.slice(lineEnd),
        Math.max(lineStart, selection.start - removedBeforeStart),
        Math.max(lineStart, selection.end - removedTotal),
      )
      return
    }

    if (selection.start !== selection.end) {
      const nextBlock = block.split('\n').map((line) => indent + line).join('\n')
      const lineCount = block.split('\n').length
      applyValue(
        value.slice(0, lineStart) + nextBlock + value.slice(lineEnd),
        selection.start + indent.length,
        selection.end + indent.length * lineCount,
      )
      return
    }

    const column = selection.start - lineStart
    const spaces = indent.length - (column % indent.length)
    const inserted = ' '.repeat(spaces)
    applyValue(value.slice(0, selection.start) + inserted + value.slice(selection.end), selection.start + spaces)
  }

  function insertNewLine() {
    const lineStart = value.lastIndexOf('\n', Math.max(0, selection.start - 1)) + 1
    const currentLine = value.slice(lineStart, selection.start)
    const baseIndent = currentLine.match(/^\s*/)?.[0] ?? ''
    const previousCharacter = value[selection.start - 1]
    const nextCharacter = value[selection.end]
    const matchingPair = (previousCharacter === '{' && nextCharacter === '}')
      || (previousCharacter === '[' && nextCharacter === ']')
      || (previousCharacter === '(' && nextCharacter === ')')

    if (matchingPair) {
      const inserted = `\n${baseIndent}${indent}\n${baseIndent}`
      const cursor = selection.start + 1 + baseIndent.length + indent.length
      applyValue(value.slice(0, selection.start) + inserted + value.slice(selection.end), cursor)
    } else {
      const increasesIndent = /[{[(]$/.test(currentLine.trimEnd())
        || (language === 'PYTHON' && currentLine.trimEnd().endsWith(':'))
      const inserted = `\n${baseIndent}${increasesIndent ? indent : ''}`
      applyValue(value.slice(0, selection.start) + inserted + value.slice(selection.end), selection.start + inserted.length)
    }
    setSuggestionsOpen(false)
  }

  function deleteContent(backward: boolean) {
    if (selection.start !== selection.end) {
      applyValue(value.slice(0, selection.start) + value.slice(selection.end), selection.start)
      setSuggestionsOpen(false)
      return
    }

    if (backward && selection.start === 0) return
    if (!backward && selection.start === value.length) return

    const pairs: Record<string, string> = { '(': ')', '[': ']', '{': '}', '"': '"', "'": "'", '`': '`' }
    const previousCharacter = value[selection.start - 1]
    const nextCharacter = value[selection.start]
    if (backward && pairs[previousCharacter] === nextCharacter) {
      applyValue(value.slice(0, selection.start - 1) + value.slice(selection.start + 1), selection.start - 1)
    } else if (backward) {
      const lineStart = value.lastIndexOf('\n', selection.start - 1) + 1
      const leadingWhitespace = value.slice(lineStart, selection.start)

      if (/^ +$/.test(leadingWhitespace)) {
        const spacesToPreviousTabStop = leadingWhitespace.length % indent.length || indent.length
        const deleteFrom = selection.start - spacesToPreviousTabStop
        applyValue(value.slice(0, deleteFrom) + value.slice(selection.start), deleteFrom)
      } else {
        applyValue(value.slice(0, selection.start - 1) + value.slice(selection.start), selection.start - 1)
      }
    } else {
      applyValue(value.slice(0, selection.start) + value.slice(selection.start + 1), selection.start)
    }
    setSuggestionsOpen(false)
  }

  const suggestionPosition = caretPosition(value, selection.start, scroll.top, scroll.left)
  const suggestionsId = `${editorId}-suggestions`

  return (
    <div className="grid flex-1 grid-cols-[3.25rem_minmax(0,1fr)] bg-blue-950">
      <div aria-hidden="true" className="select-none border-r border-blue-800 bg-blue-900 py-4 pr-3 text-right font-mono text-xs leading-6 text-blue-300">
        {lineNumbers.map((line) => <div key={line}>{line}</div>)}
      </div>
      <div className="relative min-w-0">
        <label className="sr-only" htmlFor={editorId}>Code editor</label>
        <div aria-hidden="true" className="code-editor-text pointer-events-none absolute inset-0 z-0 overflow-hidden p-4 text-white">
          <pre
            className="m-0 min-w-max whitespace-pre font-inherit"
            style={{ transform: `translate(${-scroll.left}px, ${-scroll.top}px)` }}
          >
            {highlightCode(value, language)}
            {value.endsWith('\n') ? ' ' : null}
          </pre>
        </div>
        <textarea
          ref={textareaRef}
          id={editorId}
          value={value}
          onChange={(event) => {
            applyValue(event.target.value, event.target.selectionStart, event.target.selectionEnd)
            setSuggestionsOpen(true)
          }}
          onKeyDown={handleKeyDown}
          onClick={updateSelection}
          onBlur={() => setSuggestionsOpen(false)}
          onKeyUp={(event) => {
            if (!['ArrowDown', 'ArrowUp'].includes(event.key) || suggestions.length === 0) updateSelection()
          }}
          onSelect={updateSelection}
          onScroll={(event) => setScroll({ top: event.currentTarget.scrollTop, left: event.currentTarget.scrollLeft })}
          spellCheck={false}
          autoCapitalize="off"
          autoCorrect="off"
          aria-autocomplete="list"
          aria-controls={suggestionsId}
          className="code-editor-text relative z-10 h-full min-h-[500px] w-full resize-none bg-transparent p-4 text-transparent caret-blue-300 outline-none selection:bg-blue-600/30"
        />
        {suggestions.length > 0 && suggestionPosition.top >= 0 ? (
          <div id={suggestionsId} role="listbox" style={{ top: suggestionPosition.top, left: suggestionPosition.left }} className="absolute z-30 w-72 max-w-[calc(100%-1rem)] overflow-hidden rounded-xl border border-blue-200 bg-white shadow-xl shadow-blue-900/20">
            {suggestions.map((completion, index) => (
              <button key={completion.label} type="button" role="option" aria-selected={index === selectedSuggestion} onMouseDown={(event) => event.preventDefault()} onClick={() => acceptSuggestion(completion)} className={`flex w-full items-center gap-3 px-3 py-2 text-left transition ${index === selectedSuggestion ? 'bg-blue-600 text-white' : 'text-blue-700 hover:bg-blue-50'}`}>
                <span className="min-w-0 flex-1 truncate font-mono text-xs font-semibold">{completion.label}</span>
                <span className="shrink-0 rounded bg-blue-50 px-1.5 py-0.5 text-[9px] font-medium text-blue-600">{completion.detail}</span>
              </button>
            ))}
            <div className="flex items-center justify-between border-t border-blue-100 bg-blue-50 px-3 py-1.5 text-[10px] text-blue-600"><span>↑↓ di chuyển</span><span>Tab chọn · Esc đóng</span></div>
          </div>
        ) : null}
      </div>
    </div>
  )
}

function highlightCode(value: string, language: EditorLanguage): ReactNode[] {
  return tokenizeComments(value, language).map((segment, index) => (
    <span
      key={`${segment.kind}-${index}-${segment.value.length}`}
      className={segment.kind === 'comment' ? 'text-blue-300 italic' : 'text-white'}
    >
      {segment.value}
    </span>
  ))
}

interface CodeSegment {
  kind: 'code' | 'comment'
  value: string
}

function tokenizeComments(value: string, language: EditorLanguage): CodeSegment[] {
  if (!value) return []
  if (language === 'HTML') return tokenizeHtmlComments(value)

  const segments: CodeSegment[] = []
  let codeStart = 0
  let cursor = 0

  const pushComment = (start: number, end: number) => {
    if (start > codeStart) segments.push({ kind: 'code', value: value.slice(codeStart, start) })
    segments.push({ kind: 'comment', value: value.slice(start, end) })
    codeStart = end
    cursor = end
  }

  while (cursor < value.length) {
    const character = value[cursor]

    if (isStringDelimiter(character, language)) {
      cursor = skipString(value, cursor, language)
      continue
    }

    if (value.startsWith('/*', cursor) && language !== 'PYTHON') {
      const closingIndex = value.indexOf('*/', cursor + 2)
      pushComment(cursor, closingIndex === -1 ? value.length : closingIndex + 2)
      continue
    }

    const lineComment = value.startsWith('//', cursor) && (language === 'CPP' || language === 'JAVA')
      || value.startsWith('--', cursor) && language === 'MYSQL'
      || character === '#' && (language === 'PYTHON' || language === 'MYSQL')

    if (lineComment) {
      const lineEnd = value.indexOf('\n', cursor)
      pushComment(cursor, lineEnd === -1 ? value.length : lineEnd)
      continue
    }

    cursor += 1
  }

  if (codeStart < value.length) segments.push({ kind: 'code', value: value.slice(codeStart) })
  return segments
}

function tokenizeHtmlComments(value: string): CodeSegment[] {
  const segments: CodeSegment[] = []
  let cursor = 0

  while (cursor < value.length) {
    const start = value.indexOf('<!--', cursor)
    if (start === -1) {
      segments.push({ kind: 'code', value: value.slice(cursor) })
      break
    }
    if (start > cursor) segments.push({ kind: 'code', value: value.slice(cursor, start) })
    const closingIndex = value.indexOf('-->', start + 4)
    const end = closingIndex === -1 ? value.length : closingIndex + 3
    segments.push({ kind: 'comment', value: value.slice(start, end) })
    cursor = end
  }

  return segments
}

function isStringDelimiter(character: string, language: EditorLanguage) {
  if (character === '"' || character === "'") return true
  return character === '`' && language === 'MYSQL'
}

function skipString(value: string, start: number, language: EditorLanguage) {
  const delimiter = value[start]
  const tripleQuoted = language === 'PYTHON' && value.startsWith(delimiter.repeat(3), start)
  const closing = tripleQuoted ? delimiter.repeat(3) : delimiter
  let cursor = start + closing.length

  while (cursor < value.length) {
    if (value[cursor] === '\\') {
      cursor += 2
      continue
    }
    if (value.startsWith(closing, cursor)) return cursor + closing.length
    cursor += 1
  }

  return value.length
}

function mergeCompletions(primary: Completion[], secondary: Completion[]) {
  const seen = new Set<string>()
  return [...primary, ...secondary].filter((completion) => {
    const key = completion.label.toLowerCase()
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function declaredIdentifierCompletions(value: string, cursor: number, language: EditorLanguage): Completion[] {
  const sourceBeforeCursor = value.slice(0, cursor)
  const identifiers = new Set<string>()
  const collect = (pattern: RegExp, group = 1) => {
    for (const match of sourceBeforeCursor.matchAll(pattern)) {
      const identifier = match[group]
      if (identifier) identifiers.add(identifier)
    }
  }

  if (language === 'PYTHON') {
    collect(/^\s*([A-Za-z_]\w*)\s*=(?!=)/gm)
    collect(/\bfor\s+([A-Za-z_]\w*)\s+in\b/g)
    for (const match of sourceBeforeCursor.matchAll(/\bdef\s+[A-Za-z_]\w*\s*\(([^)]*)\)/g)) {
      for (const parameter of match[1].split(',')) {
        const identifier = parameter.trim().match(/^([A-Za-z_]\w*)/)?.[1]
        if (identifier) identifiers.add(identifier)
      }
    }
  } else if (language === 'CPP' || language === 'JAVA') {
    collect(/\b(?:const\s+)?(?:unsigned\s+|signed\s+)?(?:int|long|short|double|float|char|bool|boolean|String|string|auto|var|Scanner|List(?:<[^>\n]+>)?|Map(?:<[^>\n]+>)?|Set(?:<[^>\n]+>)?)\s+([A-Za-z_]\w*)/g)
    collect(/\bfor\s*\(\s*(?:int|long|short|double|float|char|var|auto)\s+([A-Za-z_]\w*)/g)
  } else if (language === 'MYSQL') {
    collect(/\bAS\s+([A-Za-z_]\w*)/gi)
  }

  return [...identifiers].map((identifier) => ({
    label: identifier,
    detail: 'Biến đã khai báo',
    insertText: identifier,
  }))
}

function tokenAt(value: string, cursor: number) {
  const match = value.slice(0, cursor).match(/(?:[#<]?[A-Za-z_][A-Za-z0-9_.]*)$/)
  const raw = match?.[0] ?? ''
  return { start: cursor - raw.length, query: raw.replace(/^[#<]/, '') }
}

function caretPosition(value: string, cursor: number, scrollTop: number, scrollLeft: number) {
  const lines = value.slice(0, cursor).split('\n')
  const line = lines.length - 1
  const column = (lines.at(-1) ?? '').replace(/\t/g, indent).length
  return {
    top: 16 + (line + 1) * 24 - scrollTop,
    left: Math.max(8, 16 + column * 8.4 - scrollLeft),
  }
}
