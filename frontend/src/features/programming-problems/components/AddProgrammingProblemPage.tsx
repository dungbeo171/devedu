import { getStoredUser } from '../../auth/api/authApi'
import { setPendingFlash } from '../../../shared/flashMessage'
import { IconArrowLeft } from '../../../shared/components/Icons'
import { ProblemCreator } from './ProblemCreator'

export function AddProgrammingProblemPage() {
  const user = getStoredUser()
  const canCreate = user?.role === 'TEACHER' || user?.role === 'ADMIN'

  if (!canCreate) {
    return (
      <section className="ui-panel mx-auto max-w-xl p-8 text-center">
        <h2 className="text-xl font-black text-slate-900">Không có quyền thêm bài tập</h2>
        <p className="mt-2 text-xs leading-6 text-slate-600">
          Chỉ tài khoản giáo viên hoặc quản trị viên được sử dụng chức năng này.
        </p>
        <a href={user ? '/problems' : '/login'} className="ui-button-primary mt-5">
          {user ? 'Quay lại bài tập' : 'Đăng nhập'}
        </a>
      </section>
    )
  }

  return (
    <section className="mx-auto w-full max-w-4xl">
      <a href="/problems" className="ui-button-ghost px-0 hover:bg-transparent hover:text-blue-700">
        <IconArrowLeft className="h-4 w-4" />
        <span>Quay lại danh sách bài tập</span>
      </a>
      <div className="mt-5">
        <ProblemCreator
          standalone
          onCancel={() => window.location.assign('/problems')}
          onCreated={(problem) => {
            setPendingFlash(`Đã thêm bài tập “${problem.title}”`)
            window.location.assign('/problems')
          }}
        />
      </div>
    </section>
  )
}
