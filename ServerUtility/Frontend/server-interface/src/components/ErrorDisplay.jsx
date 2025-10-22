import { useServerState } from '../hooks/useServer'

export default function ErrorDisplay() {
  const { errors, ui } = useServerState()

  if (!errors.summary && !errors.upload && !ui.uploadStatus) {
    return null
  }

  return (
    <>
      {errors.summary && (
        <div className="error" style={{ 
          marginBottom: '1rem', 
          padding: '0.8rem', 
          background: 'rgba(239, 68, 68, 0.1)', 
          border: '1px solid rgba(239, 68, 68, 0.2)', 
          borderRadius: '8px', 
          color: 'var(--danger)' 
        }}>
          <strong>Error:</strong> {errors.summary}
        </div>
      )}
      
      {ui.uploadStatus && (
        <div style={{ 
          marginBottom: '1rem', 
          padding: '0.8rem', 
          background: 'rgba(16, 185, 129, 0.1)', 
          border: '1px solid rgba(16, 185, 129, 0.2)', 
          borderRadius: '8px', 
          color: 'var(--success)' 
        }}>
          <strong>Success:</strong> {ui.uploadStatus}
        </div>
      )}
    </>
  )
}