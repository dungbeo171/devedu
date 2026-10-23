import { useEffect, useState } from 'react'
import { IconArrowRight, IconBookOpen } from '../../../shared/components/Icons'
import { getDocumentRecommendations, getLearningDocuments } from '../api/learningDocumentsApi'
import type { LearningDocumentSummary } from '../types/learningDocument'

export function LearningDocumentsPage() {
  const [documents, setDocuments] = useState<LearningDocumentSummary[]>([])
  const [recommended, setRecommended] = useState<LearningDocumentSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setError('')
    void Promise.all([getLearningDocuments(), getDocumentRecommendations()])
      .then(([catalog, suggestions]) => {
        if (!ignore) {
          setDocuments(catalog)
          setRecommended(suggestions.slice(0, 4))
        }
      })
      .catch(reason => {
        if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải thư viện tài liệu.')
      })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [retry])

  if (loading) return <DocumentsSkeleton />
  if (error) return (
    <div className="mx-auto grid min-h-[420px] max-w-6xl place-items-center text-center">
      <div><p className="font-semibold text-red-700">{error}</p><button type="button" onClick={() => setRetry(value => value + 1)} className="ui-button-primary mt-4 cursor-pointer">Thử lại</button></div>
    </div>
  )

  return (
    <section className="mx-auto w-full max-w-6xl overflow-x-hidden">
      <header className="border-b border-slate-200 pb-7">
        <div className="flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-blue-600 text-white"><IconBookOpen className="h-5 w-5" /></span>
          <h1 className="max-w-5xl font-display text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">Tài liệu học lập trình</h1>
        </div>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">Lộ trình, kiến thức nền tảng và phương pháp học được sắp xếp theo hoạt động luyện tập của bạn.</p>
      </header>

      {recommended.length ? (
        <section className="mt-8">
          <div className="flex items-end justify-between gap-4">
            <div><h2 className="text-lg font-bold text-slate-950">Dành cho bạn</h2><p className="mt-1 text-sm text-slate-500">Đề xuất dựa trên chủ đề và ngôn ngữ bạn đang luyện.</p></div>
          </div>
          <div className="mt-4 grid grid-flow-dense gap-4 md:grid-cols-2 xl:grid-cols-4">
            {recommended.map((document, index) => <DocumentCard key={document.slug} document={document} featured index={index} />)}
          </div>
        </section>
      ) : null}

      <section className="mt-10">
        <div className="flex items-center justify-between gap-4 border-b border-slate-200 pb-3">
          <h2 className="text-lg font-bold text-slate-950">Tất cả tài liệu</h2>
          <span className="text-sm font-medium text-slate-500">{documents.length} tài liệu</span>
        </div>
        {documents.length ? (
          <div className="mt-4 grid grid-flow-dense gap-4 md:grid-cols-2">
            {documents.map((document, index) => <DocumentCard key={document.slug} document={document} index={index} />)}
          </div>
        ) : (
          <div className="mt-4 rounded-2xl border border-dashed border-slate-300 p-10 text-center text-sm text-slate-500">Chưa có tài liệu được xuất bản.</div>
        )}
      </section>
    </section>
  )
}

function DocumentCard({ document, featured = false, index }: { document: LearningDocumentSummary; featured?: boolean; index: number }) {
  const accent = accentFor(document.tags[0], index)
  return (
    <a href={`/documents/${encodeURIComponent(document.slug)}`} className={`group flex min-h-full cursor-pointer flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white transition duration-300 hover:-translate-y-0.5 hover:border-blue-300 hover:shadow-[0_18px_38px_-28px_rgba(13,110,253,.55)] ${featured ? '' : 'sm:flex-row'}`}>
      <div className={`${featured ? 'h-2 w-full' : 'h-auto w-2 shrink-0'} ${accent}`} />
      <div className="flex min-w-0 flex-1 flex-col p-5">
        <div className="flex items-center justify-between gap-3 text-xs font-medium text-slate-500">
          <span>{tagLabel(document.tags[0])}</span><span>{document.readingMinutes} phút đọc</span>
        </div>
        <h3 className={`mt-3 font-bold leading-snug text-slate-950 transition-colors group-hover:text-blue-700 ${featured ? 'text-base' : 'text-lg'}`}>{document.title}</h3>
        <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">{document.summary}</p>
        {document.recommendationReason ? <p className="mt-4 text-xs font-semibold text-blue-700">{document.recommendationReason}</p> : null}
        <span className="mt-auto flex items-center gap-1.5 pt-5 text-sm font-semibold text-blue-700">Đọc tài liệu <IconArrowRight className="h-4 w-4 transition-transform duration-300 group-hover:translate-x-1" /></span>
      </div>
    </a>
  )
}

function DocumentsSkeleton() {
  return <div className="mx-auto max-w-6xl animate-pulse"><div className="h-10 w-72 rounded bg-slate-100" /><div className="mt-4 h-4 w-96 max-w-full rounded bg-slate-100" /><div className="mt-10 grid gap-4 md:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 8 }, (_, index) => <div key={index} className="h-56 rounded-2xl bg-slate-100" />)}</div></div>
}

function accentFor(tag: string | undefined, index: number) {
  const accents: Record<string, string> = { JAVA: 'bg-orange-500', PYTHON: 'bg-blue-500', CPP: 'bg-indigo-500', SQL: 'bg-cyan-500', HTML: 'bg-rose-500', OOP: 'bg-violet-500', ALGORITHMS: 'bg-emerald-500', LEARNING: 'bg-blue-600' }
  return accents[tag ?? ''] ?? ['bg-blue-600', 'bg-cyan-500', 'bg-indigo-500'][index % 3]
}

export function tagLabel(tag: string | undefined) {
  const labels: Record<string, string> = { JAVA: 'Java', PYTHON: 'Python', CPP: 'C++', SQL: 'SQL', HTML: 'Web', WEB: 'Web', OOP: 'OOP', ALGORITHMS: 'Thuật toán', DATA_STRUCTURES: 'Cấu trúc dữ liệu', LEARNING: 'Phương pháp học', BEGINNER: 'Nhập môn', INTRODUCTION: 'Nhập môn' }
  return labels[tag ?? ''] ?? tag ?? 'Lập trình'
}
