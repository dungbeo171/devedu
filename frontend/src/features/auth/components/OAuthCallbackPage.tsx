import { useEffect, useState } from 'react'
import { saveAuthentication } from '../api/authApi'
import type { AuthenticationResponse, UserRole } from '../types/auth'

export function OAuthCallbackPage() {
  const [message, setMessage] = useState('Đang hoàn tất đăng nhập...')

  useEffect(() => {
    const query = new URLSearchParams(window.location.search)
    const fragment = new URLSearchParams(window.location.hash.slice(1))
    const token = fragment.get('access_token')
    const email = fragment.get('email')
    const role = fragment.get('role') as UserRole | null
    const expiresIn = Number(fragment.get('expires_in'))

    window.history.replaceState({}, document.title, '/auth/callback')
    if (query.has('oauth_error') || !token || !email || !role || !Number.isFinite(expiresIn)) {
      setMessage('Đăng nhập mạng xã hội thất bại. Vui lòng thử lại.')
      return
    }

    saveAuthentication({
      accessToken: token,
      tokenType: 'Bearer',
      expiresIn,
      user: { id: '', email, role, createdAt: '' },
    } satisfies AuthenticationResponse)
    window.location.replace('/')
  }, [])

  return <section className="grid min-h-[60vh] place-items-center text-center text-sm text-slate-300">{message}</section>
}
