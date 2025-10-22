import { useServerUI } from '../hooks/useServer'

export default function LoadingOverlay() {
  const { isTransitioning, startingMessage } = useServerUI()

  if (!isTransitioning) {
    return null
  }

  return (
    <div className="overlay-spinner">
      <div className="lds-dual-ring"></div>
      <div style={{ marginLeft: 12 }}>
        {startingMessage || 'Updating view…'}
      </div>
    </div>
  )
}