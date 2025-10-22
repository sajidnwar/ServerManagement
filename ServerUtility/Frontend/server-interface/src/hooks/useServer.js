import { useEffect } from 'react'
import { useServerContext } from '../context/ServerContext'

// Custom hook for server management operations
export function useServerOperations() {
  const {
    loadSummary,
    loadAllServers,
    handleStartServer,
    handleStopServer,
    handleUpload,
    toggleServerList
  } = useServerContext()

  // Initialize on mount
  useEffect(() => {
    loadSummary()
  }, [loadSummary])

  return {
    loadSummary,
    loadAllServers,
    startServer: handleStartServer,
    stopServer: handleStopServer,
    uploadFiles: handleUpload,
    toggleServerList
  }
}

// Custom hook for server state
export function useServerState() {
  const {
    servers,
    summary,
    loading,
    errors,
    ui,
    busyServers
  } = useServerContext()

  return {
    servers,
    summary,
    loading,
    errors,
    ui,
    busyServers,
    runningServers: servers.filter(s => s.state === 'running'),
    stoppedServers: servers.filter(s => s.state === 'stopped'),
    isLoading: loading.summary || loading.servers || ui.transitioning,
    hasErrors: !!(errors.summary || errors.upload)
  }
}

// Custom hook for UI state management
export function useServerUI() {
  const {
    ui,
    setVisible,
    clearMessages,
    loading
  } = useServerContext()

  return {
    ...ui,
    setVisible,
    clearMessages,
    isTransitioning: ui.transitioning || !!loading.starting
  }
}