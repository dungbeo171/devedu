import { useEffect, useState, type FormEvent } from 'react'
import { authenticateWithEmail, getOAuthProviders, saveAuthentication } from '../api/authApi'
import type { OAuthProvider } from '../types/auth'
import { setPendingFlash } from '../../../shared/flashMessage'
import {
  IconCheckCircle,
  IconCode,
  IconGithub,
  IconGoogle,
} from '../../../shared/components/Icons'

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
      setPendingFlash(mode === 'login' ? 'Đăng nhập thành công' : 'Đăng ký thành công')
      window.location.assign('/')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Không thể xác thực tài khoản.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto grid min-h-[calc(100vh-200px)] max-w-5xl place-items-center">
      <div className="grid w-full overflow-hidden rounded-3xl border border-white/10 bg-slate-900/90 shadow-2xl shadow-black/80 backdrop-blur-xl ring-1 ring-white/5 lg:grid-cols-[0.9fr_1.1fr]">
        {/* Left Hero Brand Panel */}
        <div className="hidden flex-col justify-between border-r border-white/10 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-10 text-white lg:flex">
          <div>
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 font-mono text-sm font-black text-white shadow-lg shadow-blue-500/25 ring-1 ring-white/20">
                <IconCode className="h-5 w-5" />
              </span>
              <span className="text-xl font-black tracking-tight">DevEdu</span>
            </div>
            <h1 className="mt-12 text-3xl font-black leading-tight text-white">
              Học, viết code và nâng cao trình độ mỗi ngày.
            </h1>
            <p className="mt-4 text-xs leading-6 text-slate-300">
              Nền tảng học lập trình toàn diện: từ biên dịch đa ngôn ngữ, luyện thuật toán đến thi cử và phỏng vấn kỹ thuật.
            </p>

            <div className="mt-8 space-y-3.5 text-xs text-slate-300">
              <div className="flex items-center gap-3">
                <IconCheckCircle className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Luyện tập lập trình với hệ thống chấm tự động</span>
              </div>
              <div className="flex items-center gap-3">
                <IconCheckCircle className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Lưu giữ tiến độ khóa học và bài tập đã giải</span>
              </div>
              <div className="flex items-center gap-3">
                <IconCheckCircle className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Tham gia kỳ thi và ôn phỏng vấn trực tiếp</span>
              </div>
            </div>
          </div>

          <div className="border-t border-white/10 pt-6 text-[11px] text-slate-400">
            <span>&copy; 2026 DevEdu &bull; Modular Monolith Architecture</span>
          </div>
        </div>

        {/* Right Auth Form */}
        <div className="p-6 sm:p-10">
          <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-[10px] font-bold text-blue-400">
            {mode === 'login' ? 'CHÀO MỪNG TRỞ LẠI' : 'BẮT ĐẦU NGAY'}
          </span>
          <h2 className="mt-3 text-2xl font-black tracking-tight text-white sm:text-3xl">
            {mode === 'login' ? 'Đăng nhập DevEdu' : 'Tạo tài khoản mới'}
          </h2>

          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            {(Object.keys(providerLabels) as OAuthProvider[]).map((provider) => {
              const enabled = enabledProviders.includes(provider)
              const ProviderIcon = provider === 'github' ? IconGithub : IconGoogle
              return enabled ? (
                <a
                  key={provider}
                  href={`/oauth2/authorization/${provider}`}
                  className="flex items-center justify-center gap-2.5 rounded-2xl border border-white/10 bg-slate-800/80 px-4 py-2.5 text-xs font-bold text-slate-200 shadow-sm transition hover:border-blue-500/50 hover:bg-slate-700/80 hover:text-white"
                >
                  <ProviderIcon className="h-4 w-4 shrink-0" />
                  <span>{providerLabels[provider]}</span>
                </a>
              ) : (
                <button
                  key={provider}
                  type="button"
                  disabled
                  title="Chưa cấu hình Client ID/Secret"
                  className="flex items-center justify-center gap-2 rounded-2xl border border-white/5 bg-slate-950/40 px-4 py-2.5 text-xs font-semibold text-slate-600"
                >
                  <ProviderIcon className="h-4 w-4 shrink-0 opacity-40" />
                  <span>{providerLabels[provider]}</span>
                </button>
              )
            })}
          </div>

          {providersLoaded && enabledProviders.length === 0 ? (
            <p className="mt-3 text-[11px] text-amber-400">
              Đăng nhập Google và GitHub cần cấu hình Client ID/Secret trong biến môi trường.
            </p>
          ) : null}

          <div className="my-6 flex items-center gap-4 text-xs font-semibold text-slate-500">
            <span className="h-px flex-1 bg-white/10" />
            <span className="font-mono text-[10px] uppercase tracking-wider">hoặc sử dụng email</span>
            <span className="h-px flex-1 bg-white/10" />
          </div>

          <form onSubmit={(event) => void submit(event)} className="space-y-4">
            {mode === 'register' ? (
              <label className="block text-xs font-bold text-slate-300">
                Họ và tên
                <input
                  type="text"
                  required
                  maxLength={100}
                  autoComplete="name"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  className="mt-1.5 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 py-3 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Nguyễn Văn An"
                />
              </label>
            ) : null}

            <label className="block text-xs font-bold text-slate-300">
              Email
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                className="mt-1.5 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 py-3 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                placeholder="ban@example.com"
              />
            </label>

            <label className="block text-xs font-bold text-slate-300">
              Mật khẩu
              <input
                type="password"
                required
                minLength={mode === 'register' ? 8 : undefined}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                className="mt-1.5 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 py-3 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                placeholder={mode === 'register' ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu'}
              />
            </label>

            {mode === 'register' ? (
              <label className="block text-xs font-bold text-slate-300">
                Xác nhận mật khẩu
                <input
                  type="password"
                  required
                  minLength={8}
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  className="mt-1.5 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 py-3 text-xs text-white outline-none placeholder:text-slate-600 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Nhập lại mật khẩu"
                />
              </label>
            ) : null}

            {message ? (
              <div role="alert" className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-3.5 text-xs font-bold text-rose-300">
                {message}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={submitting}
              className="mt-2 w-full rounded-2xl bg-gradient-to-r from-blue-600 via-indigo-600 to-blue-600 px-5 py-3.5 text-xs font-bold text-white shadow-lg shadow-blue-600/30 ring-1 ring-white/20 transition hover:from-blue-500 hover:to-indigo-500 disabled:opacity-60"
            >
              {submitting ? 'Đang xử lý...' : mode === 'login' ? 'Đăng nhập vào DevEdu' : 'Tạo tài khoản sinh viên'}
            </button>
          </form>

          <p className="mt-6 text-center text-xs text-slate-400">
            {mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}{' '}
            <a
              href={mode === 'login' ? '/register' : '/login'}
              className="font-bold text-blue-400 hover:text-cyan-300 underline"
            >
              {mode === 'login' ? 'Đăng ký ngay' : 'Đăng nhập'}
            </a>
          </p>
        </div>
      </div>
    </section>
  )
}
