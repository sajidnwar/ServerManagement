import { useServerState } from '../hooks/useServer'

export default function ServerSummary() {
  const { summary, loading, runningServers } = useServerState()

  if (loading.summary) {
    return (
      <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
        <div className="inline-spinner" />
        <div style={{ color: 'var(--muted)' }}>Loading status…</div>
      </div>
    )
  }

  if (summary.running === 0) {
    return <div style={{ color: 'var(--muted)' }}>No active servers</div>
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
      {runningServers.map(server => (
        <div key={server.id} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.6rem' }}>
          <div className="status-dot" style={{ width: 8, height: 8, borderRadius: 999, background: 'var(--success)' }} />
          <div style={{ fontSize: '0.95rem' }}>
            <strong style={{ color: '#eef2ff' }}>{server.name}</strong>
            <span style={{ marginLeft: 8, color: 'var(--muted)' }}>{server.host}:{server.port}</span>
          </div>
        </div>
      ))}
    </div>
  )
}