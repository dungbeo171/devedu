import { useCallback, useEffect, useRef, useState } from 'react'
import { contestRequest, ContestRequestError } from '../api/contestApi'
import { getStoredAccessToken } from '../../auth/api/authApi'
import { subscribeContestUpdates } from '../api/contestUpdates'

export function useContestResource<T extends { serverTime: string }>(path: string, intervalMs = 30000) {
  const accessToken = getStoredAccessToken()
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState('')
  const [errorStatus, setErrorStatus] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [now, setNow] = useState(Date.now())
  const clock = useRef({ server: Date.now(), received: performance.now() })
  const request = useRef<AbortController | null>(null)

  const reload = useCallback(async () => {
    request.current?.abort()
    const controller = new AbortController()
    request.current = controller
    setRefreshing(true)
    try {
      const result = await contestRequest<T>(path, { signal: controller.signal })
      if (controller.signal.aborted) return
      clock.current = { server: Date.parse(result.serverTime), received: performance.now() }
      setNow(clock.current.server)
      setData(result)
      setError('')
      setErrorStatus(null)
    } catch (reason) {
      if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : 'Không thể tải Contest.')
      if (!controller.signal.aborted) setErrorStatus(reason instanceof ContestRequestError ? reason.status : null)
    } finally {
      if (!controller.signal.aborted) { setLoading(false); setRefreshing(false) }
      if (request.current === controller) request.current = null
    }
  }, [path, accessToken])

  useEffect(() => {
    setData(null); setLoading(true); setError('')
    void reload()
    const unsubscribe = subscribeContestUpdates(() => { if (!request.current) void reload() }, intervalMs)
    return () => {
      request.current?.abort()
      unsubscribe()
    }
  }, [reload, intervalMs])

  useEffect(() => {
    const interval = window.setInterval(() => setNow(clock.current.server + performance.now() - clock.current.received), 1000)
    return () => window.clearInterval(interval)
  }, [])
  return { data, error, errorStatus, loading, refreshing, now, reload }
}
