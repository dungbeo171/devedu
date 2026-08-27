import { useEffect, useState, type ReactNode } from 'react'
import { CodeCompiler } from '../features/compiler/components/CodeCompiler'
import { CourseLearning } from '../features/course-learning/components/CourseLearning'
import { ExamModule } from '../features/exam/components/ExamModule'
import { InterviewModule } from '../features/interview/components/InterviewModule'
import { ProgrammingProblems } from '../features/programming-problems/components/ProgrammingProblems'
import { AuthPage } from '../features/auth/components/AuthPage'
import { OAuthCallbackPage } from '../features/auth/components/OAuthCallbackPage'
import { clearAuthentication } from '../features/auth/api/authApi'
import type { AuthenticatedUser } from '../features/auth/types/auth'
import { AdminUsersPage } from '../features/admin-users/components/AdminUsersPage'
import { FlashToast } from '../shared/components/FlashToast'
import { setPendingFlash, takePendingFlash } from '../shared/flashMessage'
import {
  IconArrowLeft,
  IconBookOpen,
  IconChevronDown,
  IconCode,
  IconLogOut,
  IconShield,
  IconSparkles,
  IconTerminal,
  IconTrophy,
} from '../shared/components/Icons'

interface RouteDefinition {
  path: string
  label: string
  title: string
  content: ReactNode
  icon: (props: { className?: string }) => ReactNode
}

const navigationRoutes: RouteDefinition[] = [
  { path: '/', label: 'Compiler', title: 'Trình biên dịch · DevEdu', content: <CodeCompiler />, icon: IconTerminal },
  { path: '/problems', label: 'Bài tập', title: 'Bài tập · DevEdu', content: <ProgrammingProblems />, icon: IconCode },
  { path: '/courses', label: 'Khóa học', title: 'Khóa học · DevEdu', content: <CourseLearning />, icon: IconBookOpen },
  { path: '/exams', label: 'Kỳ thi', title: 'Kỳ thi · DevEdu', content: <ExamModule />, icon: IconTrophy },
  { path: '/interview', label: 'Interview', title: 'Interview · DevEdu', content: <InterviewModule />, icon: IconSparkles },
]

const adminRoute: RouteDefinition = {
  path: '/admin/users',
  label: 'Quản trị',
  title: 'Quản lý người dùng · DevEdu',
  content: <AdminUsersPage />,
  icon: IconShield,
}

const routes: RouteDefinition[] = [
  ...navigationRoutes,
  adminRoute,
  { path: '/login', label: 'Đăng nhập', title: 'Đăng nhập · DevEdu', content: <AuthPage mode="login" />, icon: IconCode },
  { path: '/register', label: 'Đăng ký', title: 'Đăng ký · DevEdu', content: <AuthPage mode="register" />, icon: IconCode },
  { path: '/auth/callback', label: 'OAuth', title: 'Đăng nhập · DevEdu', content: <OAuthCallbackPage />, icon: IconCode },
]

function normalizedPathname() {
  const pathname = window.location.pathname.replace(/\/+$/, '')
  return pathname || '/'
}

export function App() {
  const [pathname, setPathname] = useState(normalizedPathname)
  const [flashMessage, setFlashMessage] = useState(takePendingFlash)
  const problemSlug = problemSlugFromPath(pathname)
  const route = problemSlug
    ? { path: pathname, label: 'Bài tập', title: 'Bài tập · DevEdu', content: <ProgrammingProblems slug={problemSlug} />, icon: IconCode }
    : routes.find((candidate) => candidate.path === pathname)

  useEffect(() => {
    const updatePathname = () => setPathname(normalizedPathname())
    window.addEventListener('popstate', updatePathname)
    return () => window.removeEventListener('popstate', updatePathname)
  }, [])

  useEffect(() => {
    document.title = route?.title ?? 'Không tìm thấy trang · DevEdu'
  }, [route])

  return (
    <div className="flex min-h-screen flex-col bg-white font-sans text-slate-900 antialiased selection:bg-blue-600 selection:text-white">
      <FlashToast message={flashMessage} onDismiss={() => setFlashMessage('')} />
      <SiteHeader pathname={pathname} />
      <main className="devedu-content w-full flex-1 bg-white px-4 py-6 sm:px-6 sm:py-8 lg:px-10">
        <div className="mx-auto max-w-7xl">
          {route?.content ?? <NotFoundPage />}
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}

function problemSlugFromPath(pathname: string): string | null {
  const prefix = '/problems/'
  if (!pathname.startsWith(prefix)) return null
  const encodedSlug = pathname.slice(prefix.length)
  if (!encodedSlug || encodedSlug.includes('/')) return null
  try {
    return decodeURIComponent(encodedSlug)
  } catch {
    return null
  }
}

function SiteHeader({ pathname }: { pathname: string }) {
  const user = storedUser()
  const visibleNavigationRoutes = user?.role === 'ADMIN' ? [...navigationRoutes, adminRoute] : navigationRoutes

  function logout() {
    clearAuthentication()
    setPendingFlash('Đăng xuất thành công')
    window.location.assign('/')
  }

  return (
    <header className="sticky top-0 z-40 w-full border-b border-blue-700 bg-blue-600 text-white shadow-md shadow-blue-900/20">
      <div className="relative flex h-16 w-full items-center justify-between px-4 sm:px-6 lg:px-10">
        {/* Brand Logo */}
        <a className="group flex items-center gap-3 font-bold tracking-tight transition-transform hover:scale-[1.02]" href="/">
          <span className="relative grid h-9 w-9 place-items-center rounded-xl bg-white font-mono text-sm font-black text-blue-600 shadow-sm ring-1 ring-blue-100">
            <IconCode className="h-5 w-5 text-blue-600" />
            <span className="absolute inset-0 rounded-xl bg-blue-50 opacity-0 transition-opacity group-hover:opacity-30" />
          </span>
          <div className="flex flex-col leading-none">
            <span className="text-lg font-black tracking-tight text-white">
              Dev<span className="text-blue-100">Edu</span>
            </span>
            <span className="font-mono text-[9px] font-semibold tracking-wider text-blue-100">LEARN &bull; CODE &bull; GROW</span>
          </div>
        </a>

        {/* Center Navigation Bar */}
        <nav className="hidden items-center gap-2 rounded-2xl border border-blue-500 bg-blue-700/40 p-1.5 lg:flex" aria-label="Điều hướng chính">
          {visibleNavigationRoutes.map((item) => {
            const active = pathname === item.path || (item.path === '/problems' && pathname.startsWith('/problems/'))
            const Icon = item.icon
            return (
              <a
                key={item.path}
                href={item.path}
                aria-current={active ? 'page' : undefined}
                className={`group flex items-center gap-2 rounded-xl px-3.5 py-1.5 text-xs font-semibold tracking-wide transition-all ${
                  active
                    ? 'bg-white text-blue-700 shadow-sm'
                    : 'text-blue-50 hover:bg-blue-700 hover:text-white'
                }`}
              >
                <Icon className={`h-3.5 w-3.5 transition-transform group-hover:scale-110 ${active ? 'text-blue-700' : 'text-blue-100 group-hover:text-white'}`} />
                <span>{item.label}</span>
              </a>
            )
          })}
        </nav>

        {/* Right User & Actions */}
        <div className="flex items-center gap-3">
          {user ? (
            <details className="group relative">
              <summary className="flex cursor-pointer list-none items-center gap-2.5 rounded-xl border border-blue-400 bg-blue-700/40 px-3 py-1.5 shadow-sm transition hover:bg-blue-700 [&::-webkit-details-marker]:hidden">
                <span className="grid h-7 w-7 place-items-center rounded-lg bg-white text-xs font-black text-blue-600 shadow-sm ring-1 ring-blue-100">
                  {avatarInitial(user.name)}
                </span>
                <span className="hidden max-w-36 truncate text-xs font-semibold text-white sm:block">{user.name}</span>
                <IconChevronDown className="h-3.5 w-3.5 text-blue-100 transition-transform duration-200 group-open:rotate-180" />
              </summary>
              <div className="absolute right-0 mt-2 w-56 rounded-2xl border border-blue-100 bg-white p-2 text-slate-900 shadow-xl shadow-blue-900/15">
                <div className="px-3 py-2.5">
                  <p className="truncate text-xs font-bold text-slate-900">{user.name}</p>
                  <div className="mt-1 flex items-center gap-1.5">
                    <span className="h-1.5 w-1.5 rounded-full bg-blue-600" />
                    <span className="rounded bg-blue-50 px-1.5 py-0.5 font-mono text-[10px] font-bold text-blue-700 ring-1 ring-blue-100">
                      {user.role}
                    </span>
                  </div>
                </div>
                <div className="my-1 h-px bg-blue-100" />
                <button
                  type="button"
                  onClick={logout}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-semibold text-blue-700 transition hover:bg-blue-50 hover:text-blue-800"
                >
                  <IconLogOut className="h-4 w-4" />
                  <span>Đăng xuất</span>
                </button>
              </div>
            </details>
          ) : (
            <div className="flex items-center gap-2">
              <a
                href="/login"
                className="hidden rounded-xl px-3.5 py-2 text-xs font-semibold text-white transition hover:bg-blue-700 sm:inline-flex"
              >
                Đăng nhập
              </a>
              <a
                href="/register"
                className="rounded-xl bg-white px-4 py-2 text-xs font-bold text-blue-700 shadow-sm ring-1 ring-blue-100 transition hover:bg-blue-50"
              >
                Đăng ký
              </a>
            </div>
          )}
        </div>
      </div>

      {/* Mobile Navigation */}
      <nav className="flex w-full justify-start gap-1 overflow-x-auto border-t border-blue-500 bg-blue-600 px-4 py-2 text-xs sm:justify-evenly lg:hidden" aria-label="Điều hướng chính trên thiết bị di động">
        {visibleNavigationRoutes.map((item) => {
          const active = pathname === item.path || (item.path === '/problems' && pathname.startsWith('/problems/'))
          const Icon = item.icon
          return (
            <a
              key={item.path}
              href={item.path}
              aria-current={active ? 'page' : undefined}
              className={`flex shrink-0 items-center gap-1.5 rounded-lg px-3 py-1.5 font-semibold transition ${
                active ? 'bg-white text-blue-700 shadow-sm' : 'text-blue-100 hover:bg-blue-700 hover:text-white'
              }`}
            >
              <Icon className="h-3.5 w-3.5" />
              <span>{item.label}</span>
            </a>
          )
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
    <footer className="mt-auto border-t border-blue-100 bg-white px-4 py-6 text-xs text-slate-500 sm:px-6 lg:px-10">
      <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 sm:flex-row">
        <div className="flex items-center gap-2.5">
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-blue-400 opacity-75" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-blue-600" />
          </span>
          <span className="font-bold text-blue-700">DevEdu</span>
          <span className="text-slate-500">&bull; Nền tảng học lập trình Modular Monolith</span>
        </div>
        <div className="flex items-center gap-4 text-[11px] text-slate-500">
          <span>&copy; 2026 DevEdu Platform</span>
        </div>
      </div>
    </footer>
  )
}

function NotFoundPage() {
  return (
    <section className="grid min-h-[60vh] place-items-center text-center">
      <div className="max-w-md rounded-3xl border border-blue-100 bg-white p-8 shadow-xl shadow-blue-900/10">
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-blue-50 font-mono text-2xl font-black text-blue-600 ring-1 ring-blue-100">
          404
        </div>
        <h1 className="mt-6 text-2xl font-black tracking-tight text-slate-900">Không tìm thấy trang</h1>
        <p className="mt-2 text-xs leading-6 text-slate-600">
          Đường dẫn bạn mở không tồn tại hoặc đã được di chuyển trên DevEdu.
        </p>
        <a
          href="/"
          className="mt-6 inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-xs font-bold text-white shadow-md shadow-blue-500/20 transition hover:bg-blue-700"
        >
          <IconArrowLeft className="h-4 w-4" />
          <span>Về Trình biên dịch</span>
        </a>
      </div>
    </section>
  )
}
