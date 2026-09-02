import { useEffect, useState } from 'react'
import { saveAuthentication } from '../api/authApi'
import type { AuthenticationResponse, UserRole } from '../types/auth'
import { IconArrowLeft } from '../../../shared/components/Icons'
import { setPendingFlash } from '../../../shared/flashMessage'

export function OAuthCallbackPage() {
  const [message, setMessage] = useState('Đang hoàn tất quá trình xác thực tài khoản...')
  const [isError, setIsError] = useState(false)

  useEffect(() => {
    const query = new URLSearchParams(window.location.search)
    const fragment = new URLSearchParams(window.location.hash.slice(1))
    const token = fragment.get('access_token')
    const name = fragment.get('name')
    const email = fragment.get('email')
    const role = fragment.get('role') as UserRole | null
    const expiresIn = Number(fragment.get('expires_in'))
    const publicId = Number(fragment.get('public_id'))
    const studentCode = fragment.get('student_code') || null
    const teacherCode = fragment.get('teacher_code') || null

    window.history.replaceState({}, document.title, '/auth/callback')
    if (query.has('oauth_error') || !token || !email || !role || !Number.isFinite(expiresIn) || !Number.isFinite(publicId)) {
      setMessage('Đăng nhập mạng xã hội thất bại hoặc bị hủy. Vui lòng thử lại.')
      setIsError(true)
      return
    }

    saveAuthentication({
      accessToken: token,
      tokenType: 'Bearer',
      expiresIn,
      user: { id: publicId, publicId, studentCode, teacherCode,
        name: name?.trim() || email.split('@')[0], email, role, createdAt: '' },
    } satisfies AuthenticationResponse)
    setPendingFlash('Đăng nhập thành công')
    window.location.replace('/')
  }, [])

  return (
    <section className="grid min-h-[60vh] place-items-center text-center">
      <div className="max-w-md rounded-3xl border border-white/10 bg-slate-900/90 p-8 shadow-2xl shadow-black/60 backdrop-blur-xl ring-1 ring-white/5">
        {!isError ? (
          <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-blue-500/20 text-blue-400 ring-1 ring-blue-500/30">
            <svg className="h-6 w-6 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
          </div>
        ) : (
          <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-rose-500/20 text-2xl text-rose-400 ring-1 ring-rose-500/30">
            ⚠️
          </div>
        )}
        <h2 className="mt-4 text-lg font-bold text-white">{isError ? 'Xác thực không thành công' : 'Đang chuyển hướng'}</h2>
        <p className="mt-2 text-xs leading-5 text-slate-400">{message}</p>
        {isError ? (
          <a
            href="/login"
            className="mt-6 inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-blue-500/25 transition hover:from-blue-500 hover:to-indigo-500"
          >
            <IconArrowLeft className="h-3.5 w-3.5" />
            <span>Quay về trang đăng nhập</span>
          </a>
        ) : null}
      </div>
    </section>
  )
}
