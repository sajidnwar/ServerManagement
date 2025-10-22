import { useServerState, useServerOperations } from '../hooks/useServer'
import ServerCard from './ServerCard'

export default function RunningServersList() {
  const { runningServers, busyServers, loading } = useServerState()
  const { stopServer, uploadFiles } = useServerOperations()

  if (loading.summary) {
    return (
      <div className="card server-card stopped" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 80 }}>
        <div className="inline-spinner" />
        <span style={{ marginLeft: 12, color: 'var(--muted)' }}>Loading status…</span>
      </div>
    )
  }

  if (runningServers.length === 0) {
    return (
      <div className="card server-card stopped">
        <div style={{ padding: '1rem' }}>
          <strong>No servers are currently running</strong>
        </div>
      </div>
    )
  }

  return (
    <div className="server-grid">
      {runningServers.map(server => (
        <ServerCard 
          key={server.id} 
          server={server} 
          onStart={() => {}} // No start action for running servers
          onStop={() => stopServer(server.name || server.id)} 
          onUpload={uploadFiles}
        />
      ))}
    </div>
  )
}