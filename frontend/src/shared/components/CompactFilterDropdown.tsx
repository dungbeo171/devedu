import type { MouseEvent } from 'react'
import { IconCheck, IconChevronDown } from './Icons'

export function CompactFilterDropdown({ label, value, options, onChange }: {
  label: string
  value: string
  options: { value: string; label: string }[]
  onChange: (value: string) => void
}) {
  const selected = options.find((option) => option.value === value) ?? options[0]

  function choose(event: MouseEvent<HTMLButtonElement>, nextValue: string) {
    onChange(nextValue)
    event.currentTarget.closest('details')?.removeAttribute('open')
  }

  return (
    <details className="group relative w-full sm:w-auto sm:min-w-36" name="compact-filters">
      <summary className="flex min-h-9 list-none items-center justify-between gap-2 rounded-md border border-transparent bg-slate-100 px-2.5 text-xs font-medium text-slate-600 transition hover:bg-slate-200/80 hover:text-slate-900 group-open:border-slate-300 group-open:bg-white [&::-webkit-details-marker]:hidden">
        <span className="min-w-0"><span className="mr-1 text-slate-400">{label}:</span><span className="truncate text-slate-800">{selected.label}</span></span>
        <IconChevronDown className="h-3 w-3 shrink-0 text-slate-400 transition-transform group-open:rotate-180" />
      </summary>
      <div className="absolute left-0 top-[calc(100%+.35rem)] z-50 w-full min-w-48 rounded-lg border border-slate-200 bg-white p-1 shadow-[0_10px_28px_rgba(15,23,42,.12)]">
        {options.map((option) => (
          <button key={option.value} type="button" onClick={(event) => choose(event, option.value)} aria-selected={option.value === value} className={`flex min-h-8 w-full items-center justify-between gap-3 rounded-md px-2.5 text-left text-xs font-medium transition ${option.value === value ? 'bg-blue-50 text-blue-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'}`}>
            <span>{option.label}</span>
            {option.value === value ? <IconCheck className="h-3.5 w-3.5" /> : null}
          </button>
        ))}
      </div>
    </details>
  )
}
