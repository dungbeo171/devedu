import { IconCheck } from './Icons'

export function FlashToast({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  if (!message) return null

  return (
    <div
      role="status"
      aria-atomic="true"
      className="flash-toast fixed right-4 top-28 z-[90] flex max-w-[calc(100vw-2rem)] items-center gap-3 rounded-xl border border-blue-700 bg-blue-600 px-4 py-3 text-sm font-bold text-white shadow-[0_18px_36px_-16px_rgba(8,66,152,.7)] sm:right-6 lg:right-8 lg:top-20"
      onAnimationEnd={onDismiss}
    >
      <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-white text-blue-600 shadow-sm" aria-hidden="true">
        <IconCheck className="h-4 w-4" />
      </span>
      <span>{message}</span>
    </div>
  )
}
