import { useEffect, useState, type ReactNode } from 'react'
import { CodeCompiler } from '../features/compiler/components/CodeCompiler'
import { CourseLearning } from '../features/course-learning/components/CourseLearning'
import { ExamModule } from '../features/exam/components/ExamModule'
import { ProgrammingProblems } from '../features/programming-problems/components/ProgrammingProblems'
import { AddProgrammingProblemPage } from '../features/programming-problems/components/AddProgrammingProblemPage'
import { EditProgrammingProblemPage } from '../features/programming-problems/components/EditProgrammingProblemPage'
import { AuthPage } from '../features/auth/components/AuthPage'
import { OAuthCallbackPage } from '../features/auth/components/OAuthCallbackPage'
import { clearAuthentication, getStoredUser } from '../features/auth/api/authApi'
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
  { path: '/', label: 'Trình biên dịch', title: 'Trình biên dịch · DevEdu', content: <CodeCompiler />, icon: IconTerminal },
  { path: '/problems', label: 'Bài tập', title: 'Bài tập · DevEdu', content: <ProgrammingProblems />, icon: IconCode },
  { path: '/courses', label: 'Lớp học', title: 'Lớp học · DevEdu', content: <CourseLearning />, icon: IconBookOpen },
  { path: '/exams', label: 'Kỳ thi', title: 'Kỳ thi · DevEdu', content: <ExamModule />, icon: IconTrophy },
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
  const editProblemSlug = editProblemSlugFromPath(pathname)
  const route = pathname === '/problems/add'
    ? { path: pathname, label: 'Thêm bài tập', title: 'Thêm bài tập · DevEdu', content: <AddProgrammingProblemPage />, icon: IconCode }
    : editProblemSlug
    ? { path: pathname, label: 'Sửa bài tập', title: 'Sửa bài tập · DevEdu', content: <EditProgrammingProblemPage slug={editProblemSlug} />, icon: IconCode }
    : problemSlug
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
    <div className="flex min-h-screen w-full max-w-full flex-col overflow-x-hidden bg-white font-sans text-slate-900 antialiased">
      <FlashToast message={flashMessage} onDismiss={() => setFlashMessage('')} />
      <SiteHeader pathname={pathname} />
      <main className="w-full flex-1 px-4 py-5 sm:px-6 sm:py-7 lg:px-8 lg:py-8">
        <div
          key={pathname}
          className={`page-enter mx-auto w-full ${
            pathname === '/' || pathname === '/problems' || pathname.startsWith('/problems/')
              ? 'lg:w-[90vw] lg:max-w-[90vw]'
              : 'max-w-[1180px]'
          }`}
        >
          {route?.content ?? <NotFoundPage />}
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}

function editProblemSlugFromPath(pathname: string): string | null {
  const match = pathname.match(/^\/problems\/([^/]+)\/edit$/)
  if (!match) return null
  try {
    return decodeURIComponent(match[1])
  } catch {
    return null
  }
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
  const user = getStoredUser()
  const visibleNavigationRoutes = user?.role === 'ADMIN' ? [...navigationRoutes, adminRoute] : navigationRoutes

  function logout() {
    clearAuthentication()
    setPendingFlash('Đăng xuất thành công')
    window.location.assign('/')
  }

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200 bg-white/95 text-slate-700 backdrop-blur-md">
      <div className="mx-auto flex h-13 w-full max-w-[1180px] items-center gap-7 px-4 sm:px-6 lg:px-8">
        <a className="group flex shrink-0 items-center gap-2 font-bold tracking-tight" href="/" aria-label="DevEdu - Trình biên dịch">
          <span className="relative grid h-8 w-8 place-items-center rounded-md border border-blue-200 bg-blue-50 font-mono text-sm font-black text-blue-600 transition duration-200 group-hover:-rotate-3 group-hover:border-blue-400 group-hover:bg-blue-100">
            <IconCode className="h-[18px] w-[18px] text-blue-600" />
          </span>
          <span className="font-display text-[17px] font-semibold tracking-[-0.025em] text-slate-950">DevEdu</span>
        </a>

        <nav className="hidden min-w-0 flex-1 items-stretch gap-1 lg:flex" aria-label="Điều hướng chính">
          {visibleNavigationRoutes.map((item) => {
            const active = pathname === item.path || (item.path === '/problems' && pathname.startsWith('/problems/'))
            return (
              <a
                key={item.path}
                href={item.path}
                aria-current={active ? 'page' : undefined}
                className={`relative flex min-h-13 items-center px-3 text-[13px] font-medium transition-colors after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:rounded-full after:transition-transform ${
                  active
                    ? 'text-blue-600 after:scale-x-100 after:bg-blue-600'
                    : 'text-slate-600 after:scale-x-0 after:bg-blue-600 hover:text-slate-950'
                }`}
              >
                <span>{item.label}</span>
              </a>
            )
          })}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          {user ? (
            <details className="group relative">
              <summary className="flex min-h-9 cursor-pointer list-none items-center gap-2 rounded-lg px-2 transition hover:bg-slate-100 [&::-webkit-details-marker]:hidden">
                <span className="grid h-7 w-7 place-items-center rounded-full bg-slate-200 text-[11px] font-bold text-slate-600 ring-1 ring-white">
                  {avatarInitial(user.name)}
                </span>
                <span className="hidden max-w-32 truncate text-xs font-medium text-slate-700 sm:block">{user.name}</span>
                <IconChevronDown className="h-3 w-3 text-slate-400 transition-transform duration-200 group-open:rotate-180" />
              </summary>
              <div className="absolute right-0 mt-2 w-60 rounded-lg border border-slate-200 bg-white p-2 text-slate-900 shadow-[0_12px_28px_rgba(15,23,42,.12)]">
                <div className="px-3 py-2.5">
                  <p className="truncate text-xs font-bold text-slate-900">{user.name}</p>
                  <div className="mt-1 flex items-center gap-1.5">
                    <span className="h-1.5 w-1.5 rounded-full bg-blue-600" />
                    <span className="rounded-md bg-blue-50 px-1.5 py-0.5 text-[10px] font-bold text-blue-700 ring-1 ring-blue-100">
                      {user.role}
                    </span>
                  </div>
                  <p className="mt-1.5 font-mono text-[10px] text-slate-500">
                    {user.role === 'ADMIN'
                      ? 'Quản trị viên'
                      : `ID: ${user.publicId ?? '—'}${user.studentCode ? ` · ${user.studentCode}` : user.teacherCode ? ` · ${user.teacherCode}` : ''}`}
                  </p>
                </div>
                <div className="my-1 h-px bg-slate-100" />
                <button
                  type="button"
                  onClick={logout}
                  className="flex min-h-9 w-full items-center gap-2.5 rounded-md px-3 text-left text-xs font-semibold text-red-600 transition hover:bg-red-50"
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
                className="hidden min-h-9 items-center rounded-md px-3 text-xs font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-950 sm:inline-flex"
              >
                Đăng nhập
              </a>
              <a
                href="/register"
                className="inline-flex min-h-9 items-center rounded-md bg-blue-600 px-3.5 text-xs font-semibold text-white transition hover:bg-blue-700"
              >
                Đăng ký
              </a>
            </div>
          )}
        </div>
      </div>

      <nav className="flex w-full justify-start gap-1 overflow-x-auto border-t border-slate-100 bg-white px-3 text-xs sm:justify-center lg:hidden" aria-label="Điều hướng chính trên thiết bị di động">
        {visibleNavigationRoutes.map((item) => {
          const active = pathname === item.path || (item.path === '/problems' && pathname.startsWith('/problems/'))
          return (
            <a
              key={item.path}
              href={item.path}
              aria-current={active ? 'page' : undefined}
              className={`relative flex min-h-10 shrink-0 items-center px-3 font-medium transition after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 ${
                active ? 'text-blue-600 after:bg-blue-600' : 'text-slate-500 after:bg-transparent hover:text-slate-900'
              }`}
            >
              <span>{item.label}</span>
            </a>
          )
        })}
      </nav>
    </header>
  )
}

function avatarInitial(name: string) {
  return name.trim().charAt(0).toLocaleUpperCase('vi') || 'U'
}

function SiteFooter() {
  return (
    <footer className="mt-auto border-t border-slate-200 bg-white px-4 py-4 text-xs text-slate-400 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-[1180px] flex-col items-center justify-between gap-3 sm:flex-row">
        <div className="flex items-center gap-2.5">
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-blue-400 opacity-75" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-blue-600" />
          </span>
          <span className="font-display font-bold text-blue-700">DevEdu</span>
          <span className="text-slate-500">&bull; Nền tảng học lập trình</span>
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
      <div className="ui-panel max-w-md p-8">
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-xl bg-blue-50 font-mono text-2xl font-black text-blue-600 ring-1 ring-blue-100">
          404
        </div>
        <h1 className="mt-6 text-2xl font-black tracking-tight text-slate-900">Không tìm thấy trang</h1>
        <p className="mt-2 text-xs leading-6 text-slate-600">
          Đường dẫn bạn mở không tồn tại hoặc đã được di chuyển trên DevEdu.
        </p>
        <a
          href="/"
          className="ui-button-primary mt-6"
        >
          <IconArrowLeft className="h-4 w-4" />
          <span>Về Trình biên dịch</span>
        </a>
      </div>
    </section>
  )
}
