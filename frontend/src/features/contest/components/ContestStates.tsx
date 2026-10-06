import type { ReactNode } from 'react'
export function ContestLoading() {
  return <div role="status" aria-label="Đang tải Contest" className="mt-6 space-y-3">
    {[0, 1, 2].map(i => <div key={i} className="ui-skeleton h-36 rounded-lg" />)}
    <span className="sr-only">Đang tải Contest…</span>
  </div>
}
export function ContestError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return <div role="alert" className="my-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
    <span>{message}</span><button type="button" onClick={onRetry} className="ui-button-danger">Thử lại</button>
  </div>
}
export function ContestEmpty({ title, children }: { title: string; children?: ReactNode }) {
  return <div className="ui-panel my-5 p-8 text-center sm:p-12"><h2 className="text-base font-semibold text-slate-800">{title}</h2>
    {children && <div className="mt-3 text-sm leading-6 text-slate-500">{children}</div>}</div>
}
