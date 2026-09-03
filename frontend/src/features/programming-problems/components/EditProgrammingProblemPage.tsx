import { useEffect, useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { setPendingFlash } from '../../../shared/flashMessage'
import { IconArrowLeft } from '../../../shared/components/Icons'
import { getManagedProgrammingProblem } from '../api/programmingProblemsApi'
import type { ManagedProgrammingProblem } from '../types/programmingProblem'
import { ProblemCreator } from './ProblemCreator'

export function EditProgrammingProblemPage({ slug }: { slug: string }) {
  const user = getStoredUser()
  const [problem, setProblem] = useState<ManagedProgrammingProblem | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (user?.role !== 'ADMIN') return
    let ignore = false
    void getManagedProgrammingProblem(slug)
      .then((result) => { if (!ignore) setProblem(result) })
      .catch((reason: unknown) => {
        if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải bài tập.')
      })
    return () => { ignore = true }
  }, [slug, user?.role])

  if (user?.role !== 'ADMIN') {
    return (
      <section className="ui-panel mx-auto max-w-xl p-8 text-center">
        <h2 className="text-xl font-black text-slate-900">Không có quyền sửa bài tập</h2>
        <p className="mt-2 text-xs leading-6 text-slate-600">Chỉ quản trị viên được sử dụng chức năng này.</p>
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
      {error ? <p role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-xs font-bold text-red-700">{error}</p> : null}
      {!error && !problem ? <div className="ui-skeleton mt-5 h-80 rounded-[18px] border border-slate-200" /> : null}
      {problem ? (
        <ProblemCreator
          standalone
          initialProblem={problem}
          onCancel={() => window.location.assign('/problems')}
          onCreated={(updated) => {
            setPendingFlash(`Đã cập nhật bài tập “${updated.title}”`)
            window.location.assign('/problems')
          }}
        />
      ) : null}
    </section>
  )
}
