// Transport boundary: an SSE/WebSocket subscription can replace this polling implementation.
export function subscribeContestUpdates(refresh: () => void, intervalMs: number): () => void {
  const update = () => { if (!document.hidden) refresh() }
  const timer = window.setInterval(update, intervalMs)
  window.addEventListener('focus', update)
  document.addEventListener('visibilitychange', update)
  return () => {
    window.clearInterval(timer)
    window.removeEventListener('focus', update)
    document.removeEventListener('visibilitychange', update)
  }
}
