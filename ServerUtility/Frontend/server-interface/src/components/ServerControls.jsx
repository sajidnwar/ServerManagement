import { useState } from 'react'
import { useServerOperations, useServerUI } from '../hooks/useServer'
import UploadZipModal from './UploadZipModal'
import { uploadServerZip } from '../api/servers'

export default function ServerControls() {
  const { loadSummary, toggleServerList } = useServerOperations()
  const { visible, isTransitioning } = useServerUI()
  const [showUploadModal, setShowUploadModal] = useState(false)

  const handleUploadZip = async (zipFile) => {
    try {
      const result = await uploadServerZip(zipFile)
      
      // Show success message or refresh server list
      console.log('ZIP uploaded successfully:', result)
      
      // Optionally refresh the server summary after upload
      setTimeout(() => {
        loadSummary()
      }, 1000)
      
      return result
    } catch (error) {
      console.error('Upload failed:', error)
      throw error
    }
  }

  return (
    <>
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
        <button 
          onClick={() => setShowUploadModal(true)}
          disabled={isTransitioning}
          style={{
            background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            padding: '0.5rem 1rem',
            cursor: 'pointer',
            fontWeight: '500',
            transition: 'all 0.2s ease'
          }}
          onMouseOver={(e) => {
            e.target.style.transform = 'translateY(-1px)'
            e.target.style.boxShadow = '0 4px 12px rgba(16, 185, 129, 0.3)'
          }}
          onMouseOut={(e) => {
            e.target.style.transform = 'translateY(0)'
            e.target.style.boxShadow = 'none'
          }}
        >
          📦 Upload ZIP
        </button>
      </div>

      <UploadZipModal
        isOpen={showUploadModal}
        onClose={() => setShowUploadModal(false)}
        onUpload={handleUploadZip}
      />
    </>
  )
}