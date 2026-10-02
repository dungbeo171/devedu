import { useState, type FormEvent } from 'react'

interface CompilerFilesProps {
  files: Record<string, string>
  activeFile: string
  canAdd: boolean
  disabled: boolean
  onSelect: (name: string) => void
  onAdd: (name: string) => void
  onRemove: (name: string) => void
}

export function CompilerFiles({ files, activeFile, canAdd, disabled, onSelect, onAdd, onRemove }: CompilerFilesProps) {
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const names = Object.keys(files)

  function addFile(event: FormEvent) {
    event.preventDefault()
    const fileName = name.trim().endsWith('.java') ? name.trim() : `${name.trim()}.java`
    if (!/^[A-Za-z_][A-Za-z0-9_]{0,79}\.java$/.test(fileName)
      || /^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])\.java$/i.test(fileName)) {
      setError('Nhập tên class hợp lệ, ví dụ Student.java.')
      return
    }
    if (names.some((item) => item.toLowerCase() === fileName.toLowerCase())) {
      setError('Tên file đã tồn tại.')
      return
    }
    if (names.length >= 20) return
    onAdd(fileName)
    setName('')
    setError('')
  }

  return (
    <aside aria-label="Danh sách file" className="min-w-0 bg-white">
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-2">
        <h2 className="text-xs font-semibold text-slate-700">File dự án</h2>
        <span className="text-xs text-slate-400">{names.length} file</span>
      </div>
      <ul className="max-h-44 overflow-y-auto p-2 md:max-h-96">
        {names.map((fileName) => (
          <li key={fileName} className="flex min-w-0 items-center gap-1">
            <button type="button" aria-pressed={activeFile === fileName} onClick={() => onSelect(fileName)}
              className={`min-w-0 flex-1 cursor-pointer truncate rounded-md px-3 py-2 text-left font-mono text-sm transition-colors focus-visible:outline-2 focus-visible:outline-blue-600 ${activeFile === fileName ? 'bg-blue-50 font-semibold text-blue-700' : 'text-slate-600 hover:bg-slate-50'}`}>
              {fileName}
            </button>
            {canAdd && fileName !== 'Main.java' && (
              <button type="button" disabled={disabled} aria-label={`Xóa ${fileName}`}
                onClick={() => { if (window.confirm(`Xóa file ${fileName}? Nội dung file sẽ bị xóa.`)) onRemove(fileName) }}
                className="cursor-pointer rounded px-2 py-1 text-slate-400 hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-50">×</button>
            )}
          </li>
        ))}
      </ul>
      {canAdd && (
        <form onSubmit={addFile} className="px-3 pb-3">
          <div className="flex gap-2">
            <label className="sr-only" htmlFor="java-file-name">Tên file Java mới</label>
            <input id="java-file-name" value={name} onChange={(event) => { setName(event.target.value); setError('') }}
              disabled={disabled || names.length >= 20} maxLength={85} placeholder="Student.java" aria-describedby={error ? 'file-error' : undefined}
              className="min-w-0 flex-1 rounded-md border border-slate-200 px-2 py-1.5 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100" />
            <button type="submit" disabled={disabled || !name.trim() || names.length >= 20} className="ui-button-secondary shrink-0">+ File</button>
          </div>
          {error && <p id="file-error" role="alert" className="mt-2 text-xs text-red-600">{error}</p>}
          <p className="mt-2 text-xs text-slate-500">Chạy từ Main.java · tối đa 20 file, không dùng package.</p>
        </form>
      )}
    </aside>
  )
}
