import { useEffect, useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

export function ModalDialog({
  title,
  onClose,
  children,
  footer,
  maxWidth = 'max-w-xl',
}: {
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  maxWidth?: 'max-w-xl' | 'max-w-2xl'
}) {
  const titleId = useId()
  const widthClass = maxWidth === 'max-w-2xl' ? 'sm:max-w-2xl' : 'sm:max-w-xl'
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeRef = useRef(onClose)
  const previousFocus = useRef(document.activeElement)

  useEffect(() => { closeRef.current = onClose }, [onClose])

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); closeRef.current(); return }
      if (event.key !== 'Tab') return
      const focusable = Array.from(dialogRef.current?.querySelectorAll<HTMLElement>(
        'button, input, select, textarea, a[href], [tabindex]',
      ) ?? []).filter(element => element.tabIndex >= 0 && !element.matches(':disabled') && element.getClientRects().length > 0)
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (!first) { event.preventDefault(); dialogRef.current?.focus(); return }
      if (event.shiftKey && (document.activeElement === first || document.activeElement === dialogRef.current)) {
        event.preventDefault(); last.focus()
      } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialogRef.current)) {
        event.preventDefault(); first.focus()
      }
    }
    document.body.style.overflow = 'hidden'
    if (!dialogRef.current?.contains(document.activeElement)) dialogRef.current?.focus({ preventScroll: true })
    window.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleKeyDown)
      if (previousFocus.current instanceof HTMLElement && previousFocus.current.isConnected) {
        previousFocus.current.focus({ preventScroll: true })
      }
    }
  }, [])

  // The app's page-enter transform must not become the fixed overlay's containing block.
  return createPortal(
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/35 p-2 backdrop-blur-[1px] sm:p-4"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1}
        className={`flex max-h-[calc(100dvh-1rem)] min-h-0 w-full min-w-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-[0_20px_60px_rgba(15,23,42,.18)] sm:max-h-[calc(100dvh-2rem)] ${widthClass}`}>
        <div className="flex shrink-0 items-center justify-between gap-3 border-b border-slate-200 bg-white px-5 py-3.5 sm:px-6">
          <h2 id={titleId} className="text-lg font-semibold text-slate-950">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Đóng" className="ui-button-ghost h-9 min-h-9 w-9 p-0 text-xl">×</button>
        </div>
        <div className="min-h-0 overflow-y-auto overscroll-contain">{children}</div>
        {footer && <div className="shrink-0 border-t border-slate-200 bg-white px-5 py-4 sm:px-6">{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}
