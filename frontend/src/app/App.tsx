import { useEffect, type ReactNode } from 'react'
import { CodeCompiler } from '../features/compiler/components/CodeCompiler'
import { CourseLearning } from '../features/course-learning/components/CourseLearning'
import { ExamModule } from '../features/exam/components/ExamModule'
import { InterviewModule } from '../features/interview/components/InterviewModule'
import { ProgrammingProblems } from '../features/programming-problems/components/ProgrammingProblems'
import { AuthPage } from '../features/auth/components/AuthPage'
import { OAuthCallbackPage } from '../features/auth/components/OAuthCallbackPage'
import { clearAuthentication } from '../features/auth/api/authApi'
import type { AuthenticatedUser } from '../features/auth/types/auth'

interface RouteDefinition {
  path: string
  label: string
  title: string
  content: ReactNode
}

const navigationRoutes: RouteDefinition[] = [
  { path: '/', label: 'Compiler', title: 'Compiler · DevEdu', content: <CodeCompiler /> },
  { path: '/problems', label: 'Bài tập', title: 'Bài tập · DevEdu', content: <ProgrammingProblems /> },
  { path: '/courses', label: 'Khóa học', title: 'Khóa học · DevEdu', content: <CourseLearning /> },
  { path: '/exams', label: 'Kỳ thi', title: 'Kỳ thi · DevEdu', content: <ExamModule /> },
  { path: '/interview', label: 'Interview', title: 'Interview · DevEdu', content: <InterviewModule /> },
]

const routes: RouteDefinition[] = [
  ...navigationRoutes,
  { path: '/login', label: 'Đăng nhập', title: 'Đăng nhập · DevEdu', content: <AuthPage mode="login" /> },
  { path: '/register', label: 'Đăng ký', title: 'Đăng ký · DevEdu', content: <AuthPage mode="register" /> },
  { path: '/auth/callback', label: 'OAuth', title: 'Đăng nhập · DevEdu', content: <OAuthCallbackPage /> },
]

function normalizedPathname() {
  const pathname = window.location.pathname.replace(/\/+$/, '')
  return pathname || '/'
}

export function App() {
  const pathname = normalizedPathname()
  const route = routes.find((candidate) => candidate.path === pathname)

  useEffect(() => {
    document.title = route?.title ?? 'Không tìm thấy trang · DevEdu'
  }, [route])

  return (
    <div className="min-h-screen bg-white text-slate-900">
      <SiteHeader pathname={pathname} />
      <main className="min-h-[calc(100vh-150px)] w-full bg-gradient-to-b from-blue-50 via-white to-white px-4 py-8 sm:px-6 sm:py-10 lg:px-10">
        {route?.content ?? <NotFoundPage />}
      </main>
      <SiteFooter />
    </div>
  )
}

function SiteHeader({ pathname }: { pathname: string }) {
  const user = storedUser()

  function logout() {
    clearAuthentication()
    window.location.assign('/')
  }

  return (
    <header className="sticky top-0 z-30 border-b border-blue-800 bg-blue-700 text-white shadow-md shadow-blue-900/15">
      <div className="relative flex h-16 w-full items-center px-4 sm:px-6 lg:px-10">
        <a className="absolute left-4 flex items-center gap-2.5 font-semibold tracking-tight sm:left-6 lg:left-10" href="/">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-white font-mono text-sm font-black text-blue-700">&lt;/&gt;</span>
          <span className="text-lg text-white">Dev<span className="text-blue-200">Edu</span></span>
        </a>
        <nav className="mx-auto hidden items-center gap-1 text-sm md:flex" aria-label="Điều hướng chính">
          {navigationRoutes.map((item) => {
            const active = pathname === item.path
            return (
              <a key={item.path} href={item.path} aria-current={active ? 'page' : undefined} className={`shrink-0 rounded-lg px-3 py-2 font-medium transition ${active ? 'bg-white text-blue-700 shadow-sm' : 'text-blue-100 hover:bg-blue-600 hover:text-white'}`}>
                {item.label}
              </a>
            )
          })}
        </nav>
        <div className="absolute right-4 flex items-center gap-2 sm:right-6 lg:right-10">
          {user ? (
            <details className="group relative">
              <summary className="flex cursor-pointer list-none items-center gap-2 rounded-xl px-2 py-1.5 transition hover:bg-blue-600 [&::-webkit-details-marker]:hidden">
                <span className="grid h-9 w-9 place-items-center rounded-full bg-white text-sm font-bold text-blue-700">{avatarInitial(user.name)}</span>
                <span className="hidden max-w-36 truncate text-sm font-semibold text-white sm:block">{user.name}</span>
                <span className="hidden text-xs text-blue-200 sm:block">▾</span>
              </summary>
              <div className="absolute right-0 mt-2 w-44 rounded-xl border border-blue-100 bg-white p-2 shadow-xl">
                <p className="truncate px-3 py-2 text-xs font-semibold text-slate-700">{user.name}</p>
                <div className="my-1 h-px bg-slate-100" />
                <button type="button" onClick={logout} className="w-full rounded-lg px-3 py-2 text-left text-sm font-medium text-rose-600 transition hover:bg-rose-50">Đăng xuất</button>
              </div>
            </details>
          ) : (
            <>
              <a href="/login" className="hidden rounded-lg px-3 py-2 text-xs font-semibold text-blue-100 transition hover:bg-blue-600 hover:text-white sm:inline-flex">Đăng nhập</a>
              <a href="/register" className="rounded-lg bg-white px-3 py-2 text-xs font-bold text-blue-700 transition hover:bg-blue-50">Đăng ký</a>
            </>
          )}
        </div>
      </div>
      <nav className="flex w-full justify-start gap-1 overflow-x-auto border-t border-blue-600 px-4 py-2 text-sm md:hidden" aria-label="Điều hướng chính trên thiết bị di động">
        {navigationRoutes.map((item) => {
          const active = pathname === item.path
          return <a key={item.path} href={item.path} aria-current={active ? 'page' : undefined} className={`shrink-0 rounded-lg px-3 py-2 font-medium transition ${active ? 'bg-white text-blue-700' : 'text-blue-100 hover:bg-blue-600 hover:text-white'}`}>{item.label}</a>
        })}
      </nav>
    </header>
  )
}

function storedUser(): AuthenticatedUser | null {
  try {
    const value = localStorage.getItem('devedu.user')
    if (!value) return null
    const user = JSON.parse(value) as AuthenticatedUser
    return { ...user, name: user.name?.trim() || user.email.split('@')[0] }
  } catch {
    return null
  }
}

function avatarInitial(name: string) {
  return name.trim().charAt(0).toLocaleUpperCase('vi') || 'U'
}

function SiteFooter() {
  return (
    <footer className="flex w-full flex-col gap-2 border-t border-blue-100 bg-white px-4 py-6 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-10">
      <span>© 2026 DevEdu · Programming learning platform</span>
      <span>Modular Monolith · Learn · Write · Build</span>
    </footer>
  )
}

function NotFoundPage() {
  return (
    <section className="grid min-h-[60vh] place-items-center text-center">
      <div>
        <p className="font-mono text-sm text-blue-600">404</p>
        <h1 className="mt-3 text-3xl font-bold text-slate-900">Không tìm thấy trang</h1>
        <p className="mt-3 text-slate-600">Đường dẫn bạn mở không tồn tại trên DevEdu.</p>
        <a href="/" className="mt-6 inline-flex rounded-lg bg-blue-600 px-5 py-3 text-sm font-bold text-white hover:bg-blue-500">Về Compiler</a>
      </div>
    </section>
  )
}
