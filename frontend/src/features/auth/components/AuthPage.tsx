import { useEffect, useRef, useState, type ClipboardEvent, type FormEvent, type KeyboardEvent } from 'react'
import {
  getOAuthProviders,
  loginWithEmail,
  requestEmailRegistration,
  saveAuthentication,
  verifyEmailRegistration,
} from '../api/authApi'
import type { OAuthProvider } from '../types/auth'
import { setPendingFlash } from '../../../shared/flashMessage'
import { IconCheckCircle, IconCode, IconGithub, IconGoogle } from '../../../shared/components/Icons'

const providerLabels: Record<OAuthProvider, string> = { google: 'Google', github: 'GitHub' }
type MessageTone = 'error' | 'success'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [verificationEmail, setVerificationEmail] = useState('')
  const [verificationStep, setVerificationStep] = useState(false)
  const [resendRemaining, setResendRemaining] = useState(0)
  const [enabledProviders, setEnabledProviders] = useState<OAuthProvider[]>([])
  const [providersLoaded, setProvidersLoaded] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [messageTone, setMessageTone] = useState<MessageTone>('error')

  function showMessage(value: string, tone: MessageTone) {
    setMessage(value)
    setMessageTone(tone)
  }

  useEffect(() => {
    void getOAuthProviders()
      .then((response) => setEnabledProviders(response.enabledProviders))
      .catch(() => showMessage('Không thể tải trạng thái đăng nhập mạng xã hội.', 'error'))
      .finally(() => setProvidersLoaded(true))
  }, [])

  useEffect(() => {
    if (!verificationStep || resendRemaining <= 0) return
    const timer = window.setInterval(() => setResendRemaining((current) => Math.max(0, current - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [verificationStep, resendRemaining])

  async function submitCredentials(event: FormEvent) {
    event.preventDefault()
    if (mode === 'register' && password !== confirmPassword) {
      showMessage('Mật khẩu xác nhận không khớp.', 'error')
      return
    }
    setSubmitting(true)
    setMessage('')
    try {
      if (mode === 'login') {
        const result = await loginWithEmail(email, password)
        saveAuthentication(result)
        setPendingFlash('Đăng nhập thành công')
        window.location.assign('/')
        return
      }
      const challenge = await requestEmailRegistration(name, email, password)
      setVerificationEmail(challenge.email)
      setResendRemaining(challenge.resendAfterSeconds)
      setVerificationCode('')
      setVerificationStep(true)
      showMessage('Mã xác nhận gồm 6 chữ số đã được gửi đến email của bạn.', 'success')
    } catch (error) {
      showMessage(error instanceof Error ? error.message : 'Không thể xác thực tài khoản.', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  async function submitVerification(event: FormEvent) {
    event.preventDefault()
    if (!/^\d{6}$/.test(verificationCode)) {
      showMessage('Vui lòng nhập đủ 6 chữ số.', 'error')
      return
    }
    setSubmitting(true)
    setMessage('')
    try {
      const result = await verifyEmailRegistration(verificationEmail, verificationCode)
      saveAuthentication(result)
      setPendingFlash('Đăng ký và xác nhận email thành công')
      window.location.assign('/')
    } catch (error) {
      showMessage(error instanceof Error ? error.message : 'Không thể xác nhận email.', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  async function resendCode() {
    if (submitting || resendRemaining > 0) return
    setSubmitting(true)
    setMessage('')
    try {
      const challenge = await requestEmailRegistration(name, email, password)
      setVerificationEmail(challenge.email)
      setResendRemaining(challenge.resendAfterSeconds)
      setVerificationCode('')
      showMessage('Đã gửi một mã xác nhận mới.', 'success')
    } catch (error) {
      showMessage(error instanceof Error ? error.message : 'Không thể gửi lại mã xác nhận.', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const isVerifying = mode === 'register' && verificationStep

  return (
    <section className="mx-auto grid min-h-[calc(100vh-220px)] max-w-5xl place-items-center">
      <div className="grid w-full overflow-hidden rounded-[20px] border border-slate-200 bg-white shadow-[0_24px_60px_-32px_rgba(15,23,42,.35)] lg:grid-cols-[0.9fr_1.1fr]">
        <div className="hidden flex-col justify-between border-r border-blue-700 bg-blue-600 p-10 text-white lg:flex">
          <div>
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-[10px] bg-white text-blue-600 shadow-sm"><IconCode className="h-5 w-5" /></span>
              <span className="text-xl font-black tracking-tight">DevEdu</span>
            </div>
            <h1 className="mt-12 max-w-md text-3xl font-bold leading-tight text-white">Học, viết code và nâng cao trình độ mỗi ngày.</h1>
            <div className="mt-8 space-y-3.5 text-sm text-blue-50">
              {['Luyện tập lập trình với hệ thống chấm tự động', 'Lưu giữ tiến độ và bài tập đã giải', 'Tham gia Contest lập trình'].map((item) => (
                <div key={item} className="flex items-center gap-3"><IconCheckCircle className="h-4 w-4 shrink-0 text-white" /><span>{item}</span></div>
              ))}
            </div>
          </div>
          <div className="border-t border-white/20 pt-6 text-xs text-blue-100">&copy; 2026 DevEdu</div>
        </div>

        <div className="p-6 sm:p-10">
          <h2 className="text-3xl font-bold tracking-tight text-slate-950">
            {mode === 'login' ? 'Đăng nhập DevEdu' : isVerifying ? 'Xác nhận email' : 'Tạo tài khoản mới'}
          </h2>
          {isVerifying ? <p className="mt-2 text-sm leading-6 text-slate-600">Nhập mã đã gửi tới <strong className="text-slate-900">{verificationEmail}</strong>.</p> : null}

          {!isVerifying ? (
            <>
              <div className="mt-6 grid gap-3 sm:grid-cols-2">
                {(Object.keys(providerLabels) as OAuthProvider[]).map((provider) => {
                  const enabled = enabledProviders.includes(provider)
                  const ProviderIcon = provider === 'github' ? IconGithub : IconGoogle
                  return enabled ? (
                    <a key={provider} href={`/oauth2/authorization/${provider}`} className="ui-button-secondary"><ProviderIcon className="h-4 w-4 shrink-0" />{providerLabels[provider]}</a>
                  ) : (
                    <button key={provider} type="button" disabled title="Chưa cấu hình Client ID/Secret" className="ui-button-secondary opacity-50">
                      <ProviderIcon className="h-4 w-4 shrink-0 opacity-40" />{providerLabels[provider]}
                    </button>
                  )
                })}
              </div>
              {providersLoaded && enabledProviders.length === 0 ? <p className="mt-3 text-[11px] text-amber-600">Đăng nhập Google và GitHub cần cấu hình Client ID/Secret trong biến môi trường.</p> : null}
              <div className="my-6 flex items-center gap-4 text-xs font-semibold text-slate-500">
                <span className="h-px flex-1 bg-slate-200" /><span className="font-mono text-[10px] uppercase tracking-wider">hoặc sử dụng email</span><span className="h-px flex-1 bg-slate-200" />
              </div>
            </>
          ) : null}

          {isVerifying ? (
            <form onSubmit={(event) => void submitVerification(event)} className="mt-8 space-y-5">
              <div>
                <span className="block text-sm font-semibold text-slate-700">Mã xác nhận</span>
                <OtpCodeInput value={verificationCode} onChange={setVerificationCode} disabled={submitting} />
              </div>
              {message ? <AuthMessage message={message} tone={messageTone} /> : null}
              <button type="submit" disabled={submitting || verificationCode.length !== 6} className="ui-button-primary w-full">
                {submitting ? 'Đang xác nhận...' : 'Xác nhận và tạo tài khoản'}
              </button>
              <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
                <button type="button" className="font-semibold text-slate-600 hover:text-blue-700" onClick={() => { setVerificationStep(false); setMessage('') }}>Sửa thông tin</button>
                <button type="button" disabled={submitting || resendRemaining > 0} className="font-semibold text-blue-700 disabled:cursor-not-allowed disabled:text-slate-400" onClick={() => void resendCode()}>
                  {resendRemaining > 0 ? `Gửi lại sau ${resendRemaining}s` : 'Gửi lại mã'}
                </button>
              </div>
            </form>
          ) : (
            <form onSubmit={(event) => void submitCredentials(event)} className="space-y-4">
              {mode === 'register' ? <Field label="Họ và tên"><input type="text" required maxLength={100} autoComplete="name" value={name} onChange={(event) => setName(event.target.value)} className="ui-control mt-2" placeholder="Nguyễn Văn An" /></Field> : null}
              <Field label="Email"><input type="email" required autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} className="ui-control mt-2" placeholder="ban@example.com" /></Field>
              <Field label="Mật khẩu"><input type="password" required minLength={mode === 'register' ? 8 : undefined} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} className="ui-control mt-2" placeholder={mode === 'register' ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu'} /></Field>
              {mode === 'register' ? <Field label="Xác nhận mật khẩu"><input type="password" required minLength={8} autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} className="ui-control mt-2" placeholder="Nhập lại mật khẩu" /></Field> : null}
              {message ? <AuthMessage message={message} tone={messageTone} /> : null}
              <button type="submit" disabled={submitting} className="ui-button-primary mt-2 w-full">{submitting ? 'Đang xử lý...' : mode === 'login' ? 'Đăng nhập vào DevEdu' : 'Gửi mã xác nhận'}</button>
            </form>
          )}

          {!isVerifying ? <p className="mt-6 text-center text-sm text-slate-600">
            {mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}{' '}
            <a href={mode === 'login' ? '/register' : '/login'} className="font-bold text-blue-700 underline decoration-blue-200 underline-offset-4 hover:text-blue-900">{mode === 'login' ? 'Đăng ký ngay' : 'Đăng nhập'}</a>
          </p> : null}
        </div>
      </div>
    </section>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <label className="block text-sm font-semibold text-slate-700">{label}{children}</label>
}

function AuthMessage({ message, tone }: { message: string; tone: MessageTone }) {
  return <div role={tone === 'error' ? 'alert' : 'status'} className={tone === 'error'
    ? 'rounded-xl border border-red-200 bg-red-50 p-3.5 text-sm font-semibold text-red-700'
    : 'rounded-xl border border-blue-200 bg-blue-50 p-3.5 text-sm font-semibold text-blue-700'}>{message}</div>
}

function OtpCodeInput({ value, onChange, disabled }: { value: string; onChange: (value: string) => void; disabled: boolean }) {
  const inputs = useRef<Array<HTMLInputElement | null>>([])
  const digits = Array.from({ length: 6 }, (_, index) => value[index] ?? '')

  function writeFrom(index: number, raw: string) {
    const incoming = raw.replace(/\D/g, '')
    if (!incoming) {
      const next = digits.slice()
      next[index] = ''
      onChange(next.join(''))
      return
    }
    const next = digits.slice()
    incoming.slice(0, 6 - index).split('').forEach((digit, offset) => { next[index + offset] = digit })
    onChange(next.join(''))
    inputs.current[Math.min(index + incoming.length, 5)]?.focus()
  }

  function handleKeyDown(index: number, event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      event.preventDefault()
      const next = digits.slice()
      next[index - 1] = ''
      onChange(next.join(''))
      inputs.current[index - 1]?.focus()
    } else if (event.key === 'ArrowLeft' && index > 0) {
      event.preventDefault()
      inputs.current[index - 1]?.focus()
    } else if (event.key === 'ArrowRight' && index < 5) {
      event.preventDefault()
      inputs.current[index + 1]?.focus()
    }
  }

  function handlePaste(event: ClipboardEvent<HTMLDivElement>) {
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (!pasted) return
    event.preventDefault()
    onChange(pasted)
    inputs.current[Math.min(pasted.length, 6) - 1]?.focus()
  }

  return (
    <div className="mt-3 grid max-w-[360px] grid-cols-6 gap-2 sm:gap-3" onPaste={handlePaste}>
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(element) => { inputs.current[index] = element }}
          type="text"
          inputMode="numeric"
          autoComplete={index === 0 ? 'one-time-code' : 'off'}
          aria-label={`Chữ số ${index + 1} của mã xác nhận`}
          value={digit}
          disabled={disabled}
          onFocus={(event) => event.currentTarget.select()}
          onChange={(event) => writeFrom(index, event.target.value)}
          onKeyDown={(event) => handleKeyDown(index, event)}
          className="aspect-square min-w-0 rounded-xl border border-slate-300 bg-white text-center font-mono text-xl font-bold text-slate-950 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:bg-slate-100 sm:text-2xl"
          autoFocus={index === 0}
        />
      ))}
    </div>
  )
}
