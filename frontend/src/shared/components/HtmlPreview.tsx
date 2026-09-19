export function HtmlPreview({ source }: { source: string }) {
  return (
    <div className="border-b border-blue-900/70 bg-white p-2">
      <div className="mb-2 flex items-center justify-between px-1">
        <span className="font-mono text-[10px] font-bold uppercase tracking-wider text-slate-500">Xem trước HTML</span>
        <span className="text-[10px] font-semibold text-slate-400">Sandbox</span>
      </div>
      <iframe
        title="Xem trước kết quả HTML"
        srcDoc={source}
        sandbox=""
        referrerPolicy="no-referrer"
        className="h-56 w-full rounded-md border border-slate-200 bg-white sm:h-64"
      />
    </div>
  )
}
