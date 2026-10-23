import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { IconArrowLeft, IconArrowRight, IconBookOpen, IconChevronDown } from '../../../shared/components/Icons'
import {
  getDocumentRecommendations,
  getLearningDocument,
  getLearningDocumentEditorAccess,
  recordLearningDocumentView,
} from '../api/learningDocumentsApi'
import type { LearningDocumentDetail, LearningDocumentSummary } from '../types/learningDocument'
import { tagLabel } from './LearningDocumentsPage'

interface TableOfContentsItem {
  id: string
  title: string
  level: number
}

export function LearningDocumentPage({ slug }: { slug: string }) {
  const [document, setDocument] = useState<LearningDocumentDetail | null>(null)
  const [related, setRelated] = useState<LearningDocumentSummary[]>([])
  const [canEdit, setCanEdit] = useState(false)
  const [activeHeading, setActiveHeading] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setError('')
    void Promise.all([getLearningDocument(slug), getDocumentRecommendations()])
      .then(([detail, recommendations]) => {
        if (!ignore) {
          setDocument(detail)
          setRelated(recommendations.filter(item => item.slug !== slug).slice(0, 3))
          documentTitle(detail.title)
          void recordLearningDocumentView(slug).catch(() => undefined)
        }
      })
      .catch(reason => { if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải tài liệu.') })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [slug, retry])

  useEffect(() => {
    const user = getStoredUser()
    if (user?.role === 'ADMIN') {
      setCanEdit(true)
      return
    }
    if (user?.role !== 'TEACHER') {
      setCanEdit(false)
      return
    }
    let ignore = false
    void getLearningDocumentEditorAccess()
      .then(access => { if (!ignore) setCanEdit(access.canEdit) })
      .catch(() => { if (!ignore) setCanEdit(false) })
    return () => { ignore = true }
  }, [slug])

  const parsed = useMemo(() => document ? parseDocumentContent(document.content) : { nodes: [], toc: [] }, [document])

  useEffect(() => {
    if (!parsed.toc.length) return
    setActiveHeading(parsed.toc[0].id)
    const observer = new IntersectionObserver(entries => {
      const visible = entries.filter(entry => entry.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0]
      if (visible?.target.id) setActiveHeading(visible.target.id)
    }, { rootMargin: '-96px 0px -68% 0px', threshold: [0, 1] })
    for (const item of parsed.toc) {
      const element = window.document.getElementById(item.id)
      if (element) observer.observe(element)
    }
    return () => observer.disconnect()
  }, [parsed])

  if (loading) return <DocumentSkeleton />
  if (error || !document) return <div className="mx-auto grid min-h-[420px] max-w-4xl place-items-center text-center"><div><p className="font-semibold text-red-700">{error || 'Không tìm thấy tài liệu.'}</p><button type="button" onClick={() => setRetry(value => value + 1)} className="ui-button-primary mt-4 cursor-pointer">Thử lại</button></div></div>

  return (
    <div className="mx-auto w-full max-w-[1440px] overflow-x-hidden">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-4">
        <a href="/documents" className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 transition hover:text-blue-700"><IconArrowLeft className="h-4 w-4" /> Thư viện tài liệu</a>
        {canEdit ? <a href={`/documents/${encodeURIComponent(slug)}/edit`} className="ui-button-secondary min-h-9 px-4 text-xs">Chỉnh sửa tài liệu</a> : null}
      </div>

      <header className="mx-auto max-w-5xl py-8 lg:py-10">
        <div className="flex flex-wrap items-center gap-2 text-xs font-semibold text-blue-700"><span>{tagLabel(document.tags[0])}</span><span className="text-slate-300">/</span><span className="text-slate-500">{document.readingMinutes} phút đọc</span></div>
        <h1 className="mt-4 max-w-5xl font-display text-3xl font-bold leading-tight tracking-tight text-slate-950 sm:text-4xl lg:text-[42px]">{document.title}</h1>
        <p className="mt-4 max-w-4xl text-base leading-7 text-slate-600">{document.summary}</p>
      </header>

      <div className="border-t border-slate-200 lg:grid lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="border-b border-slate-200 bg-white lg:border-b-0 lg:border-r">
          <details className="group lg:hidden">
            <summary className="flex min-h-12 cursor-pointer list-none items-center justify-between font-bold text-slate-800">Mục lục <IconChevronDown className="h-4 w-4 text-blue-600 transition group-open:rotate-180" /></summary>
            <TableOfContents items={parsed.toc} activeHeading={activeHeading} />
          </details>
          <div className="sticky top-[76px] hidden max-h-[calc(100vh-92px)] overflow-y-auto px-5 py-7 lg:block">
            <div className="mb-3 text-xs font-bold uppercase tracking-[0.08em] text-slate-500">Mục lục</div>
            <TableOfContents items={parsed.toc} activeHeading={activeHeading} />
          </div>
        </aside>

        <main className="min-w-0 bg-white px-1 py-8 sm:px-7 lg:px-12 lg:py-10 xl:px-16">
          <article className="prose-document mx-auto max-w-4xl">{parsed.nodes}</article>
          {related.length ? (
            <section className="mx-auto mt-14 max-w-4xl border-t border-slate-200 pt-7">
              <div className="flex items-center gap-2"><IconBookOpen className="h-4 w-4 text-blue-600" /><h2 className="font-bold text-slate-900">Tài liệu tiếp theo</h2></div>
              <div className="mt-3 grid gap-3 sm:grid-cols-3">
                {related.map(item => <a key={item.slug} href={`/documents/${encodeURIComponent(item.slug)}`} className="group flex min-h-28 flex-col rounded-xl border border-slate-200 p-4 transition hover:border-blue-300 hover:bg-blue-50/40"><span className="text-xs font-semibold text-blue-700">{tagLabel(item.tags[0])}</span><span className="mt-2 flex items-start justify-between gap-2 text-sm font-bold leading-5 text-slate-800 group-hover:text-blue-700">{item.title}<IconArrowRight className="mt-0.5 h-4 w-4 shrink-0" /></span></a>)}
              </div>
            </section>
          ) : null}
        </main>
      </div>
    </div>
  )
}

function TableOfContents({ items, activeHeading }: { items: TableOfContentsItem[]; activeHeading: string }) {
  if (!items.length) return <p className="pb-4 text-sm text-slate-500">Tài liệu chưa có tiêu đề mục.</p>
  return <nav aria-label="Mục lục tài liệu" className="space-y-0.5 pb-4">{items.map(item => <a key={item.id} href={`#${item.id}`} className={`block border-l-2 py-2 pr-3 text-sm leading-5 transition ${item.level > 2 ? 'pl-7' : 'pl-3'} ${activeHeading === item.id ? 'border-blue-600 bg-blue-50 font-bold text-blue-800' : 'border-transparent text-slate-500 hover:border-slate-300 hover:bg-slate-50 hover:text-slate-900'}`}>{item.title}</a>)}</nav>
}

function parseDocumentContent(content: string): { nodes: ReactNode[]; toc: TableOfContentsItem[] } {
  const nodes: ReactNode[] = []
  const toc: TableOfContentsItem[] = []
  const usedIds = new Map<string, number>()
  let listItems: string[] = []
  let listKind: 'ul' | 'ol' = 'ul'

  const flushList = () => {
    if (!listItems.length) return
    const className = 'my-5 space-y-2 pl-6 text-[15px] leading-7 text-slate-700'
    const children = listItems.map((item, index) => <li key={`${index}-${item}`} className={`${listKind === 'ul' ? 'list-disc marker:text-blue-500' : 'list-decimal marker:font-semibold marker:text-slate-500'} pl-1`}>{inlineContent(item)}</li>)
    nodes.push(listKind === 'ul' ? <ul key={`list-${nodes.length}`} className={className}>{children}</ul> : <ol key={`list-${nodes.length}`} className={className}>{children}</ol>)
    listItems = []
  }

  for (const rawLine of content.split('\n')) {
    const line = rawLine.trim()
    const unordered = line.match(/^[-*]\s+(.+)$/)
    const ordered = line.match(/^\d+\.\s+(.+)$/)
    if (unordered || ordered) {
      const nextKind = unordered ? 'ul' : 'ol'
      if (listItems.length && listKind !== nextKind) flushList()
      listKind = nextKind
      listItems.push((unordered ?? ordered)?.[1] ?? '')
      continue
    }
    flushList()
    if (!line) continue
    const heading = line.match(/^(#{2,4})\s+(.+)$/)
    if (heading) {
      const level = heading[1].length
      const title = heading[2].trim()
      const baseId = headingId(title)
      const duplicate = usedIds.get(baseId) ?? 0
      usedIds.set(baseId, duplicate + 1)
      const id = duplicate ? `${baseId}-${duplicate + 1}` : baseId
      toc.push({ id, title, level })
      nodes.push(level === 2
        ? <h2 id={id} data-document-heading key={id} className="scroll-mt-24 border-b border-slate-300 pb-3 pt-9 text-2xl font-bold tracking-tight text-slate-950 first:pt-0">{inlineContent(title)}</h2>
        : <h3 id={id} data-document-heading key={id} className={`${level === 4 ? 'text-base' : 'text-xl'} scroll-mt-24 pt-7 font-bold text-slate-900`}>{inlineContent(title)}</h3>)
      continue
    }
    nodes.push(<p key={`p-${nodes.length}`} className="my-4 text-[15px] leading-8 text-slate-700">{inlineContent(line)}</p>)
  }
  flushList()
  return { nodes, toc }
}

function inlineContent(text: string): ReactNode[] {
  return text.split(/(`[^`]+`|\*\*[^*]+\*\*)/g).filter(Boolean).map((part, index) => {
    if (part.startsWith('`') && part.endsWith('`')) return <code key={index} className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-[0.92em] text-blue-800">{part.slice(1, -1)}</code>
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={index} className="font-bold text-slate-950">{part.slice(2, -2)}</strong>
    return part
  })
}

function headingId(title: string) {
  return title.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')
    .toLocaleLowerCase('vi').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'noi-dung'
}

function DocumentSkeleton() {
  return <div className="mx-auto max-w-[1440px] animate-pulse"><div className="h-5 w-36 rounded bg-slate-100" /><div className="mx-auto mt-10 max-w-5xl"><div className="h-12 w-4/5 rounded bg-slate-100" /><div className="mt-5 h-5 w-3/5 rounded bg-slate-100" /></div><div className="mt-10 grid gap-8 border-t border-slate-200 pt-8 lg:grid-cols-[280px_1fr]"><div className="h-80 rounded bg-slate-100" /><div className="space-y-4">{Array.from({ length: 9 }, (_, index) => <div key={index} className="h-4 rounded bg-slate-100" />)}</div></div></div>
}

function documentTitle(title: string) {
  window.requestAnimationFrame(() => { window.document.title = `${title} · DevEdu` })
}
