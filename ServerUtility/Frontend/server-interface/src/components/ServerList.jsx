import { useEffect, useState } from 'react'
import { fetchAllServers, startServer, stopServer } from '../api/servers'
import ServerCard from './ServerCard'

export default function ServerList({ onServersChanged, servers: parentServers, onStart: parentOnStart, onStop: parentOnStop, onUpload: parentOnUpload }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Use servers from parent component
  const servers = parentServers || []

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const items = await fetchAllServers()
      if (typeof onServersChanged === 'function') onServersChanged(items)
      setError(null) // Clear any previous errors on success
    } catch (err) {
      const errorMessage = err.message || String(err)
      setError(errorMessage)
      console.error('ServerList load error:', errorMessage)
      // Keep servers as empty array on error to show proper empty state
      if (typeof onServersChanged === 'function') onServersChanged([])
    } finally {
      setLoading(false)
    }
  }

  // Only load if no parent servers provided
  useEffect(() => {
    if (!parentServers) {
      load()
    }
  }, [parentServers])

  // Use parent handlers if provided, otherwise fallback to local handlers
  const onStart = parentOnStart || (async (id) => {
    try {
      const updated = await startServer(id)
      if (typeof onServersChanged === 'function') {
        const updatedServers = servers.map((s) => (s.id === id ? updated : s))
        onServersChanged(updatedServers)
      }
    } catch (err) {
      console.error(err)
      const errorMessage = err.message || String(err)
      setError(errorMessage)
    }
  })

  const onStop = parentOnStop || (async (id) => {
    try {
      const updated = await stopServer(id)
      if (typeof onServersChanged === 'function') {
        const updatedServers = servers.map((s) => (s.id === id ? updated : s))
        onServersChanged(updatedServers)
      }
    } catch (err) {
      console.error(err)
      const errorMessage = err.message || String(err)
      setError(errorMessage)
    }
  })

  const runningCount = servers.filter((s) => s.state === 'running').length

  return (
    <section>
      <div className="summary">
        <div className="summary-item">
          <strong>Total servers:</strong>
          <span>{servers.length}</span>
        </div>
        <div className="summary-item">
          <strong>Running:</strong>
          <span>{runningCount}</span>
        </div>
        <div className="summary-item">
          <strong>Any running?</strong>
          <span>{runningCount > 0 ? 'Yes' : 'No'}</span>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {loading && <div className="loading">Loading servers...</div>}

      <div className="server-grid">
        {servers.map((s) => (
          <ServerCard key={s.id} server={s} onStart={onStart} onStop={onStop} onUpload={parentOnUpload} />
        ))}
      </div>
    </section>
  )
}
