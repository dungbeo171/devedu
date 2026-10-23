import { CameraReportDialog } from '../camera/CameraReportDialog'

export function ContestMonitoringPage({ contestId }: { contestId: string }) {
  return <section className="min-w-0">
    <a href="/admin" className="ui-button-ghost mb-4 !px-0">← Quản lý kỳ thi</a>
    <header className="mb-5">
      <h1 className="ui-page-title">Giám sát kỳ thi</h1>
    </header>
    <CameraReportDialog contestId={contestId} embedded />
  </section>
}
