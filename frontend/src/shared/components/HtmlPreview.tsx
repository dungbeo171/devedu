export function HtmlPreview({ source, allowScripts = false }: { source: string; allowScripts?: boolean }) {
  const policy = "default-src 'none'; script-src 'unsafe-inline' data:; style-src 'unsafe-inline' data:; img-src data: blob:; font-src data:; connect-src 'none'; frame-src 'none'; form-action 'none'; base-uri 'none'"
  const preview = allowScripts
    ? `<!doctype html><meta http-equiv="Content-Security-Policy" content="${policy}">${source.replace(/^\s*<!doctype[^>]*>/i, '')}`
    : source
  return (
    <div className="border-b border-blue-900/70 bg-white p-2">
      <div className="mb-2 flex items-center justify-between px-1">
        <span className="font-mono text-[10px] font-bold uppercase tracking-wider text-slate-500">Xem trước Web</span>
        <span className="text-[10px] font-semibold text-slate-400">Sandbox</span>
      </div>
      <iframe
        title="Xem trước kết quả Web"
        srcDoc={preview}
        sandbox={allowScripts ? 'allow-scripts' : ''}
        referrerPolicy="no-referrer"
        className="h-56 w-full rounded-md border border-slate-200 bg-white sm:h-64"
      />
    </div>
  )
}
