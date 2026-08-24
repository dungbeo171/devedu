import { useEffect, useState, type FormEvent } from 'react'
import { authenticateWithEmail, getOAuthProviders, saveAuthentication } from '../api/authApi'
import type { OAuthProvider } from '../types/auth'

const providerLabels: Record<OAuthProvider, string> = {
  google: 'Google',
  github: 'GitHub',
  microsoft: 'Microsoft',
}

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
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
      const result = await authenticateWithEmail(mode, email, password)
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
      <div className="grid w-full overflow-hidden rounded-3xl border border-slate-800 bg-slate-900/70 shadow-2xl shadow-black/40 lg:grid-cols-[0.85fr_1.15fr]">
        <div className="hidden border-r border-slate-800 bg-gradient-to-br from-cyan-400/15 via-slate-900 to-violet-500/10 p-10 lg:block">
          <span className="font-mono text-sm font-bold text-cyan-300">&lt;/&gt; DevEdu</span>
          <h1 className="mt-16 text-4xl font-black leading-tight text-white">Học, viết code và tiến bộ mỗi ngày.</h1>
          <p className="mt-5 text-sm leading-7 text-slate-400">Một tài khoản để lưu tiến độ khóa học, tham gia kỳ thi và submit bài lập trình.</p>
        </div>

        <div className="p-6 sm:p-10">
          <p className="text-sm font-semibold text-cyan-300">{mode === 'login' ? 'Chào mừng trở lại' : 'Bắt đầu với DevEdu'}</p>
          <h2 className="mt-2 text-3xl font-bold text-white">{mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}</h2>

          <div className="mt-7 grid gap-3 sm:grid-cols-3">
            {(Object.keys(providerLabels) as OAuthProvider[]).map((provider) => {
              const enabled = enabledProviders.includes(provider)
              return enabled ? (
                <a key={provider} href={`/oauth2/authorization/${provider}`} className="flex items-center justify-center gap-2 rounded-xl border border-slate-700 bg-slate-800 px-3 py-3 text-sm font-semibold text-slate-200 transition hover:border-cyan-400/60 hover:bg-slate-700">
                  <span className="grid h-5 w-5 place-items-center rounded bg-slate-700 text-[10px]">{provider === 'github' ? 'GH' : provider[0].toUpperCase()}</span>
                  {providerLabels[provider]}
                </a>
              ) : (
                <button key={provider} type="button" disabled title="Chưa cấu hình Client ID/Secret" className="flex items-center justify-center gap-2 rounded-xl border border-slate-800 bg-slate-900 px-3 py-3 text-sm font-semibold text-slate-600">
                  <span className="grid h-5 w-5 place-items-center rounded bg-slate-800 text-[10px]">{provider === 'github' ? 'GH' : provider[0].toUpperCase()}</span>
                  {providerLabels[provider]}
                </button>
              )
            })}
          </div>
          {providersLoaded && enabledProviders.length === 0 ? <p className="mt-3 text-xs text-amber-300/70">Đăng nhập Google, GitHub và Microsoft cần Client ID/Secret trong file .env.</p> : null}

          <div className="my-7 flex items-center gap-4 text-xs text-slate-600"><span className="h-px flex-1 bg-slate-800" /><span>hoặc dùng email</span><span className="h-px flex-1 bg-slate-800" /></div>

          <form onSubmit={(event) => void submit(event)} className="space-y-4">
            <label className="block text-sm font-medium text-slate-300">Email
              <input type="email" required autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-[#0b1018] px-4 py-3 text-slate-100 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/10" placeholder="ban@example.com" />
            </label>
            <label className="block text-sm font-medium text-slate-300">Mật khẩu
              <input type="password" required minLength={mode === 'register' ? 8 : undefined} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-[#0b1018] px-4 py-3 text-slate-100 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/10" placeholder={mode === 'register' ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu'} />
            </label>
            {mode === 'register' ? <label className="block text-sm font-medium text-slate-300">Xác nhận mật khẩu
              <input type="password" required minLength={8} autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-[#0b1018] px-4 py-3 text-slate-100 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/10" placeholder="Nhập lại mật khẩu" />
            </label> : null}
            {message ? <p role="alert" className="rounded-lg border border-rose-400/20 bg-rose-400/5 px-3 py-2 text-xs text-rose-200">{message}</p> : null}
            <button type="submit" disabled={submitting} className="w-full rounded-xl bg-cyan-400 px-5 py-3 font-bold text-slate-950 transition hover:bg-cyan-300 disabled:opacity-60">{submitting ? 'Đang xử lý...' : mode === 'login' ? 'Đăng nhập bằng email' : 'Đăng ký bằng email'}</button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            {mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}{' '}
            <a href={mode === 'login' ? '/register' : '/login'} className="font-semibold text-cyan-300 hover:text-cyan-200">{mode === 'login' ? 'Đăng ký' : 'Đăng nhập'}</a>
          </p>
        </div>
      </div>
    </section>
  )
}
