import { useServerOperations, useServerUI } from '../hooks/useServer'

export default function ServerControls() {
  const { loadSummary, toggleServerList } = useServerOperations()
  const { visible, isTransitioning } = useServerUI()

  return (
    <div style={{ display: 'flex', gap: '0.5rem' }}>
      <button 
        onClick={toggleServerList}
        disabled={isTransitioning}
      >
        {visible ? 'Hide servers' : 'Show servers'}
      </button>
      <button 
        onClick={loadSummary} 
        disabled={isTransitioning}
      >
        Refresh Summary
      </button>
    </div>
  )
}