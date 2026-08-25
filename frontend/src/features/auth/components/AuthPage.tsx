import { useEffect, useState, type FormEvent } from 'react'
import { authenticateWithEmail, getOAuthProviders, saveAuthentication } from '../api/authApi'
import type { OAuthProvider } from '../types/auth'

const providerLabels: Record<OAuthProvider, string> = {
  google: 'Google',
  github: 'GitHub',
}

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [enabledProviders, setEnabledProviders] = useState<OAuthProvider[]>([])
  const [providersLoaded, setProvidersLoaded] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    void getOAuthProviders()
      .then((response) => setEnabledProviders(response.enabledProviders))
      .catch(() => setMessage('Không thể tải trạng thái đăng nhập mạng xã hội.'))
      .finally(() => setProvidersLoaded(true))
  }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (mode === 'register' && password !== confirmPassword) {
      setMessage('Mật khẩu xác nhận không khớp.')
      return
    }
    setSubmitting(true)
    setMessage('')
    try {
      const result = await authenticateWithEmail(mode, email, password, name)
      saveAuthentication(result)
      window.location.assign('/')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Không thể xác thực tài khoản.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto grid min-h-[calc(100vh-230px)] max-w-5xl place-items-center">
      <div className="grid w-full overflow-hidden rounded-3xl border border-blue-100 bg-white shadow-2xl shadow-blue-200/50 lg:grid-cols-[0.85fr_1.15fr]">
        <div className="hidden border-r border-blue-100 bg-gradient-to-br from-blue-100 via-white to-blue-50 p-10 lg:block">
          <span className="font-mono text-sm font-bold text-blue-600">&lt;/&gt; DevEdu</span>
          <h1 className="mt-16 text-4xl font-black leading-tight text-slate-900">Học, viết code và tiến bộ mỗi ngày.</h1>
          <p className="mt-5 text-sm leading-7 text-slate-600">Một tài khoản để lưu tiến độ khóa học, tham gia kỳ thi và submit bài lập trình.</p>
        </div>

        <div className="p-6 sm:p-10">
          <p className="text-sm font-semibold text-blue-600">{mode === 'login' ? 'Chào mừng trở lại' : 'Bắt đầu với DevEdu'}</p>
          <h2 className="mt-2 text-3xl font-bold text-slate-900">{mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}</h2>

          <div className="mt-7 grid gap-3 sm:grid-cols-2">
            {(Object.keys(providerLabels) as OAuthProvider[]).map((provider) => {
              const enabled = enabledProviders.includes(provider)
              return enabled ? (
                <a key={provider} href={`/oauth2/authorization/${provider}`} className="flex items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-3 py-3 text-sm font-semibold text-slate-800 transition hover:border-blue-500/60 hover:bg-blue-100">
                  <span className="grid h-5 w-5 place-items-center rounded bg-blue-100 text-[10px]">{provider === 'github' ? 'GH' : provider[0].toUpperCase()}</span>
                  {providerLabels[provider]}
                </a>
              ) : (
                <button key={provider} type="button" disabled title="Chưa cấu hình Client ID/Secret" className="flex items-center justify-center gap-2 rounded-xl border border-blue-100 bg-white px-3 py-3 text-sm font-semibold text-slate-600">
                  <span className="grid h-5 w-5 place-items-center rounded bg-blue-50 text-[10px]">{provider === 'github' ? 'GH' : provider[0].toUpperCase()}</span>
                  {providerLabels[provider]}
                </button>
              )
            })}
          </div>
          {providersLoaded && enabledProviders.length === 0 ? <p className="mt-3 text-xs text-amber-700">Đăng nhập Google và GitHub cần Client ID/Secret trong file .env.</p> : null}

          <div className="my-7 flex items-center gap-4 text-xs text-slate-600"><span className="h-px flex-1 bg-blue-50" /><span>hoặc dùng email</span><span className="h-px flex-1 bg-blue-50" /></div>

          <form onSubmit={(event) => void submit(event)} className="space-y-4">
            {mode === 'register' ? <label className="block text-sm font-medium text-slate-700">Họ và tên
              <input type="text" required maxLength={100} autoComplete="name" value={name} onChange={(event) => setName(event.target.value)} className="mt-2 w-full rounded-xl border border-blue-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10" placeholder="Nguyễn Văn An" />
            </label> : null}
            <label className="block text-sm font-medium text-slate-700">Email
              <input type="email" required autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} className="mt-2 w-full rounded-xl border border-blue-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10" placeholder="ban@example.com" />
            </label>
            <label className="block text-sm font-medium text-slate-700">Mật khẩu
              <input type="password" required minLength={mode === 'register' ? 8 : undefined} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} className="mt-2 w-full rounded-xl border border-blue-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10" placeholder={mode === 'register' ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu'} />
            </label>
            {mode === 'register' ? <label className="block text-sm font-medium text-slate-700">Xác nhận mật khẩu
              <input type="password" required minLength={8} autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} className="mt-2 w-full rounded-xl border border-blue-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10" placeholder="Nhập lại mật khẩu" />
            </label> : null}
            {message ? <p role="alert" className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">{message}</p> : null}
            <button type="submit" disabled={submitting} className="w-full rounded-xl bg-blue-600 px-5 py-3 font-bold text-white transition hover:bg-blue-500 disabled:opacity-60">{submitting ? 'Đang xử lý...' : mode === 'login' ? 'Đăng nhập bằng email' : 'Đăng ký bằng email'}</button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            {mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}{' '}
            <a href={mode === 'login' ? '/register' : '/login'} className="font-semibold text-blue-600 hover:text-blue-700">{mode === 'login' ? 'Đăng ký' : 'Đăng nhập'}</a>
          </p>
        </div>
      </div>
    </section>
  )
}
