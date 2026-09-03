import { useEffect, useId, type ReactNode } from 'react'

export function ModalDialog({
  title,
  onClose,
  children,
  maxWidth = 'max-w-xl',
}: {
  title: string
  onClose: () => void
  children: ReactNode
  maxWidth?: 'max-w-xl' | 'max-w-2xl'
}) {
  const titleId = useId()
  const widthClass = maxWidth === 'max-w-2xl' ? 'sm:max-w-2xl' : 'sm:max-w-xl'

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [onClose])

  return (
    <div
      className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/50 p-0 sm:items-center sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className={`max-h-[95vh] w-full overflow-y-auto rounded-t-[20px] bg-white shadow-[0_28px_80px_-32px_rgba(15,23,42,.55)] sm:rounded-[20px] ${widthClass}`}>
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4 sm:px-6">
          <h2 id={titleId} className="text-xl font-bold text-slate-950">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Đóng" className="ui-button-ghost h-9 min-h-9 w-9 p-0 text-xl">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}
