const flashStorageKey = 'devedu.flashMessage'

export function setPendingFlash(message: string) {
  sessionStorage.setItem(flashStorageKey, message)
}

export function takePendingFlash(): string {
  const message = sessionStorage.getItem(flashStorageKey) ?? ''
  sessionStorage.removeItem(flashStorageKey)
  return message
}
