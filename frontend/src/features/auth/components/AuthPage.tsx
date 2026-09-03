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
    <section className="mx-auto grid min-h-[calc(100vh-220px)] max-w-5xl place-items-center">
      <div className="grid w-full overflow-hidden rounded-[20px] border border-slate-200 bg-white shadow-[0_24px_60px_-32px_rgba(15,23,42,.35)] lg:grid-cols-[0.9fr_1.1fr]">
        <div className="hidden flex-col justify-between border-r border-blue-700 bg-blue-600 p-10 text-white lg:flex">
          <div>
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-[10px] bg-white font-mono text-sm font-black text-blue-600 shadow-sm">
                <IconCode className="h-5 w-5" />
              </span>
              <span className="text-xl font-black tracking-tight">DevEdu</span>
            </div>
            <h1 className="mt-12 max-w-md text-3xl font-bold leading-tight text-white">
              Học, viết code và nâng cao trình độ mỗi ngày.
            </h1>
            <p className="mt-4 text-sm leading-7 text-blue-50">
              Nền tảng học lập trình toàn diện: từ biên dịch đa ngôn ngữ, luyện thuật toán đến thi cử và phỏng vấn kỹ thuật.
            </p>

            <div className="mt-8 space-y-3.5 text-sm text-blue-50">
              <div className="flex items-center gap-3">
                <IconCheckCircle className="h-4 w-4 shrink-0 text-white" />
                <span>Luyện tập lập trình với hệ thống chấm tự động</span>
              </div>
              <div className="flex items-center gap-3">
                <IconCheckCircle className="h-4 w-4 shrink-0 text-white" />
                <span>Lưu giữ tiến độ khóa học và bài tập đã giải</span>
              </div>
              <div className="flex items-center gap-3">
                <IconCheckCircle className="h-4 w-4 shrink-0 text-white" />
                <span>Tham gia kỳ thi và ôn phỏng vấn trực tiếp</span>
              </div>
            </div>
          </div>

          <div className="border-t border-white/20 pt-6 text-xs text-blue-100">
            <span>&copy; 2026 DevEdu</span>
          </div>
        </div>

        <div className="p-6 sm:p-10">
          <p className="ui-kicker">{mode === 'login' ? 'Chào mừng trở lại' : 'Tài khoản sinh viên'}</p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
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
                  className="ui-button-secondary"
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
                  className="ui-button-secondary opacity-50"
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
            <span className="h-px flex-1 bg-slate-200" />
            <span className="font-mono text-[10px] uppercase tracking-wider">hoặc sử dụng email</span>
            <span className="h-px flex-1 bg-slate-200" />
          </div>

          <form onSubmit={(event) => void submit(event)} className="space-y-4">
            {mode === 'register' ? (
              <label className="block text-sm font-semibold text-slate-700">
                Họ và tên
                <input
                  type="text"
                  required
                  maxLength={100}
                  autoComplete="name"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  className="ui-control mt-2"
                  placeholder="Nguyễn Văn An"
                />
              </label>
            ) : null}

            <label className="block text-sm font-semibold text-slate-700">
              Email
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                className="ui-control mt-2"
                placeholder="ban@example.com"
              />
            </label>

            <label className="block text-sm font-semibold text-slate-700">
              Mật khẩu
              <input
                type="password"
                required
                minLength={mode === 'register' ? 8 : undefined}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                className="ui-control mt-2"
                placeholder={mode === 'register' ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu'}
              />
            </label>

            {mode === 'register' ? (
              <label className="block text-sm font-semibold text-slate-700">
                Xác nhận mật khẩu
                <input
                  type="password"
                  required
                  minLength={8}
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  className="ui-control mt-2"
                  placeholder="Nhập lại mật khẩu"
                />
              </label>
            ) : null}

            {message ? (
              <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3.5 text-sm font-semibold text-red-700">
                {message}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={submitting}
              className="ui-button-primary mt-2 w-full"
            >
              {submitting ? 'Đang xử lý...' : mode === 'login' ? 'Đăng nhập vào DevEdu' : 'Tạo tài khoản sinh viên'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-600">
            {mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}{' '}
            <a
              href={mode === 'login' ? '/register' : '/login'}
              className="font-bold text-blue-700 underline decoration-blue-200 underline-offset-4 hover:text-blue-900"
            >
              {mode === 'login' ? 'Đăng ký ngay' : 'Đăng nhập'}
            </a>
          </p>
        </div>
      </div>
    </section>
  )
}
