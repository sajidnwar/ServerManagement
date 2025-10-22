
import './App.css'
import { ServerProvider } from './context/ServerContext'
import { useServerState, useServerOperations, useServerUI } from './hooks/useServer'
import ServerList from './components/ServerList'
import ServerSummary from './components/ServerSummary'
import ServerControls from './components/ServerControls'
import RunningServersList from './components/RunningServersList'
import ErrorDisplay from './components/ErrorDisplay'
import LoadingOverlay from './components/LoadingOverlay'

function AppContent() {
  const { servers, handleServersChanged } = useServerState()
  const { uploadFiles, startServer, stopServer } = useServerOperations()
  const { visible } = useServerUI()

  return (
    <div id="root">
      <header>
        <h1>JBoss Server Manager</h1>
        <p className="read-the-docs">Monitor and control JBoss servers from a simple UI.</p>
      </header>

      <main>
        <ErrorDisplay />

        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.5rem', marginBottom: '1rem', alignItems: 'center' }}>
          <ServerSummary />
          <ServerControls />
        </div>

        {!visible && (
          <div style={{ maxWidth: 720, margin: '0 auto', textAlign: 'left' }}>
            <h2>Running Servers</h2>
            <RunningServersList />
          </div>
        )}

        {visible && (
          <ServerList 
            onServersChanged={handleServersChanged} 
            servers={servers} 
            onStart={startServer} 
            onStop={stopServer} 
            onUpload={uploadFiles} 
          />
        )}

        <LoadingOverlay />
      </main>
    </div>
  )
}

function App() {
  return (
    <ServerProvider>
      <AppContent />
    </ServerProvider>
  )
}

export default App
