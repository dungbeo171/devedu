import type { SubmissionLanguage } from './types/programmingProblem'

export interface EditorDraft { language: SubmissionLanguage; sourceCode: string; input: string }
export interface LocalDraft {
  value: EditorDraft
  revision: string
  dirty: boolean
}
export interface DraftStore {
  read(language?: SubmissionLanguage): Promise<LocalDraft | null>
  write(draft: LocalDraft): Promise<void>
  acknowledge(draft: LocalDraft): Promise<void>
}

let database: Promise<IDBDatabase> | undefined
function openDatabase(): Promise<IDBDatabase> {
  if (!database) database = new Promise<IDBDatabase>((resolve, reject) => {
    if (typeof indexedDB === 'undefined') { reject(new Error('IndexedDB unavailable')); return }
    const request = indexedDB.open('devedu-editor-drafts', 1)
    request.onupgradeneeded = () => request.result.createObjectStore('drafts', { keyPath: 'key' })
    request.onsuccess = () => {
      const db = request.result
      db.onversionchange = () => { db.close(); database = undefined }
      resolve(db)
    }
    request.onerror = () => reject(request.error)
    request.onblocked = () => reject(new Error('IndexedDB blocked'))
  }).catch(error => { database = undefined; throw error })
  return database
}

export function draftStorageKey(owner: string, namespace: string, slug: string, language = 'latest'): string {
  return JSON.stringify([owner, namespace, slug, language])
}

function validRecord(record: LocalDraft | undefined): record is LocalDraft {
  return !!record && typeof record.revision === 'string' && typeof record.dirty === 'boolean'
    && !!record.value && ['CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL'].includes(record.value.language)
    && typeof record.value.sourceCode === 'string' && typeof record.value.input === 'string'
}

export function indexedDraftStore(owner: string, namespace: string, slug: string): DraftStore {
  const key = (language?: string) => draftStorageKey(owner, namespace, slug, language)
  async function mutate(draft: LocalDraft, acknowledge: boolean): Promise<void> {
    const db = await openDatabase()
    return new Promise((resolve, reject) => {
      const transaction = db.transaction('drafts', 'readwrite')
      const store = transaction.objectStore('drafts')
      for (const storageKey of [key(), key(draft.value.language)]) {
        if (!acknowledge) store.put({ key: storageKey, record: draft })
        else {
          const request = store.get(storageKey)
          request.onsuccess = () => {
            // A late response must never mark a newer edit as synchronized.
            if (request.result?.record?.revision === draft.revision) {
              store.put({ key: storageKey, record: { ...draft, dirty: false } })
            }
          }
        }
      }
      transaction.oncomplete = () => resolve()
      transaction.onabort = () => reject(transaction.error ?? new Error('Draft transaction aborted'))
      transaction.onerror = () => reject(transaction.error)
    })
  }
  return {
    async read(language) {
      const db = await openDatabase()
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('drafts', 'readonly')
        const request = transaction.objectStore('drafts').get(key(language))
        request.onsuccess = () => resolve(validRecord(request.result?.record) ? request.result.record : null)
        request.onerror = () => reject(request.error)
      })
    },
    write: draft => mutate(draft, false),
    acknowledge: draft => mutate(draft, true),
  }
}

// Serialize writes to the same server draft, including navigation between workspaces.
const serverWrites = new Map<string, Promise<void>>()
function serialize(key: string, action: () => Promise<void>): Promise<void> {
  const next = (serverWrites.get(key) ?? Promise.resolve()).catch(() => undefined).then(action)
  serverWrites.set(key, next)
  void next.finally(() => { if (serverWrites.get(key) === next) serverWrites.delete(key) }).catch(() => undefined)
  return next
}

export const draftSyncDelay = 5000

export function createDraftSession(options: {
  store: DraftStore
  serverKey: string
  saveRemote?: (value: EditorDraft) => Promise<boolean>
  onWarning: (message: string) => void
  now?: () => number
}) {
  let pending: LocalDraft | null = null
  let localWrite = Promise.resolve()
  let timer: ReturnType<typeof setTimeout> | undefined
  let disposed = false
  let localHealthy = true
  const now = options.now ?? (() => performance.now())
  let syncAfter = 0
  const memory = new Map<SubmissionLanguage, EditorDraft>()
  const warn = (message: string) => { if (!disposed) options.onWarning(message) }

  function schedule() {
    clearTimeout(timer)
    if (!disposed && options.saveRemote && pending) {
      timer = setTimeout(() => { void synchronize() }, Math.max(0, syncAfter - now()))
    }
  }

  function edit(value: EditorDraft) {
    const record: LocalDraft = { value: { ...value }, revision: crypto.randomUUID(), dirty: true }
    pending = record
    syncAfter = now() + draftSyncDelay
    memory.set(value.language, record.value)
    // Local writes start on every edit; only network writes are debounced.
    localWrite = localWrite.then(() => options.store.write(record))
      .then(() => { localHealthy = true; warn('') })
      .catch(() => {
        localHealthy = false
        warn('Không thể lưu trên thiết bị này. Hãy giữ trang mở hoặc sao chép code để tránh mất bản nháp.')
      })
    schedule()
  }

  async function synchronize(): Promise<void> {
    await localWrite
    if (disposed || !options.saveRemote || !pending) return
    return serialize(options.serverKey, async () => {
      await localWrite
      const snapshot = pending
      if (disposed || !snapshot) return
      // Recheck after asynchronous storage AND the server queue, not just when setting a timer.
      // No caller can bypass this deadline to send the latest keystroke early.
      if (now() < syncAfter) { schedule(); return }
      try {
        const saved = await options.saveRemote!(snapshot.value)
        if (!saved) throw new Error('Draft was not synchronized')
        await options.store.acknowledge(snapshot).catch(() => undefined)
        if (pending?.revision === snapshot.revision) pending = null
        warn(localHealthy ? '' : 'Code đã đồng bộ lên máy chủ nhưng không lưu được trên thiết bị. Không có bản dự phòng ngoại tuyến.')
      } catch {
        warn('Bản nháp chưa đồng bộ lên máy chủ. Hệ thống sẽ thử lại khi có mạng hoặc khi bạn tiếp tục chỉnh sửa.')
      }
    })
  }

  return {
    edit,
    persistLocal: () => localWrite,
    retry: schedule,
    async restore(loadRemote: () => Promise<EditorDraft | null>): Promise<EditorDraft | null> {
      const local = await options.store.read().catch(() => {
        warn('Không thể đọc bản nháp trên thiết bị này.'); return null
      })
      // Unsynchronized edits win over the server; clean local copies do not hide newer remote edits.
      if (local?.dirty) {
        pending = local
        syncAfter = now() + draftSyncDelay
        memory.set(local.value.language, local.value)
        schedule()
        return local.value
      }
      const remote = await loadRemote().catch(() => null)
      const restored = remote ?? local?.value ?? null
      if (restored) memory.set(restored.language, restored)
      return restored
    },
    async forLanguage(language: SubmissionLanguage): Promise<EditorDraft | null> {
      await localWrite
      return memory.get(language) ?? (await options.store.read(language).catch(() => null))?.value ?? null
    },
    dispose() {
      disposed = true
      clearTimeout(timer)
      // Never flush to the server on unmount: recover dirty IndexedDB data on the next visit.
    },
  }
}
