import { useState } from 'react'
import UploadModal from './UploadModal'

export default function ServerCard({ server, onStart, onStop, onUpload }) {
  const busy = server.busy
  const isRunning = server.state === 'running'
  const [showUploadModal, setShowUploadModal] = useState(false)

  const handleUpload = () => {
    setShowUploadModal(true)
  }

  const handleUploadSuccess = (serverName, result) => {
    console.log('Upload successful:', result)
    
    // Call parent callback if provided
    if (onUpload) {
      onUpload(serverName, result)
    }
  }

  return (
    <>
      <div className={`card server-card ${isRunning ? 'running' : 'stopped'}`}>
        <div className="card-header">
          <h3>{server.name}</h3>
          <div className={`status-badge ${isRunning ? 'running' : 'stopped'}`}>{server.state}</div>
        </div>

        <div className="meta">
          <div>Host: {server.host}</div>
          <div>Port: {server.port}</div>
          <div>Uptime: {server.uptime ? `${server.uptime}s` : '—'}</div>
        </div>

        <div className="controls">
          <button className={`btn primary`} onClick={() => onStart(server.id)} disabled={busy || isRunning}>
            {busy && !isRunning ? 'Starting…' : 'Start'}
          </button>
          <button className={`btn ${isRunning ? 'danger' : 'ghost'}`} onClick={() => onStop(server.id)} disabled={busy || !isRunning}>
            {busy && isRunning ? 'Stopping…' : 'Stop'}
          </button>
          
          {isRunning && (
            <button 
              className="btn" 
              onClick={handleUpload} 
              disabled={busy}
              style={{ backgroundColor: 'var(--accent)', color: 'white' }}
            >
              Upload Files
            </button>
          )}
        </div>
      </div>

      <UploadModal
        isOpen={showUploadModal}
        onClose={() => setShowUploadModal(false)}
        serverName={server.name || server.id}
        deploymentsPath={server.deploymentsPath}
        onUploadSuccess={handleUploadSuccess}
      />
    </>
  )
}
