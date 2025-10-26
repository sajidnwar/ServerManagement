import { useServerState, useServerOperations, useServerUI } from '../hooks/useServer'
import ServerList from './ServerList'
import ServerSummary from './ServerSummary'
import ServerControls from './ServerControls'
import RunningServersList from './RunningServersList'
import ErrorDisplay from './ErrorDisplay'
import LoadingOverlay from './LoadingOverlay'
import Header from './Header'

const Home = () => {
  const { servers, handleServersChanged } = useServerState()
  const { uploadFiles, startServer, stopServer } = useServerOperations()
  const { visible } = useServerUI()

  return (
    <div id="root">
      <Header />

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

export default Home