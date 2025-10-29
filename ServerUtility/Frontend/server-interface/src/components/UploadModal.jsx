import { useState, useRef, useEffect } from 'react'
import { uploadDeployableFiles } from '../api/servers'

export default function UploadModal({ isOpen, onClose, serverName, deploymentsPath, onUploadSuccess }) {
  const [deploymentPath, setDeploymentPath] = useState(deploymentsPath || '')
  const [selectedFiles, setSelectedFiles] = useState([])
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState(null)
  const fileInputRef = useRef(null)

  // Update deployment path when prop changes
  useEffect(() => {
    if (deploymentsPath) {
      setDeploymentPath(deploymentsPath)
    }
  }, [deploymentsPath])

  const handleFileChange = (event) => {
    const files = Array.from(event.target.files)
    setSelectedFiles(files)
    setError(null)
  }

  const handleChooseFiles = () => {
    fileInputRef.current?.click()
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (selectedFiles.length === 0) {
      setError('Please select a file to upload')
      return
    }

    if (!deploymentPath.trim()) {
      setError('Deployment path not available for this server')
      return
    }

    setUploading(true)
    setError(null)

    try {
      const result = await uploadDeployableFiles(serverName, selectedFiles, deploymentPath)
      console.log('Upload successful:', result)
      
      if (onUploadSuccess) {
        onUploadSuccess(serverName, result)
      }
      
      // Reset form and close modal
      setSelectedFiles([])
      setDeploymentPath('')
      fileInputRef.current.value = ''
      onClose()
    } catch (error) {
      console.error('Upload failed:', error)
      setError(error.message)
    } finally {
      setUploading(false)
    }
  }

  const handleClose = () => {
    if (!uploading) {
      setSelectedFiles([])
      setDeploymentPath('')
      setError(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      onClose()
    }
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Upload Deployable Files</h3>
          <button className="modal-close" onClick={handleClose} disabled={uploading}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-group">
            <label htmlFor="serverName">Server Name:</label>
            <input
              id="serverName"
              type="text"
              value={serverName}
              disabled
              className="form-input disabled"
            />
          </div>

          <div className="form-group">
            <label htmlFor="deploymentPath">Deployment Folder Path:</label>
            <input
              id="deploymentPath"
              type="text"
              value={deploymentPath}
              placeholder="e.g., /opt/jboss/standalone/deployments"
              className="form-input disabled"
              disabled={true}
              readOnly
            />
          </div>

          <div className="form-group">
            <label>Choose Files:</label>
            <div className="file-input-group">
              <input
                ref={fileInputRef}
                type="file"
                accept=".war,.jar"
                onChange={handleFileChange}
                style={{ display: 'none' }}
                disabled={uploading}
              />
              <button
                type="button"
                onClick={handleChooseFiles}
                className="btn file-choose-btn"
                disabled={uploading}
              >
                Choose File
              </button>
              <div className="selected-files">
                {selectedFiles.length > 0 ? (
                  <div>
                    <strong>Selected file:</strong>
                    <div style={{ marginTop: '0.5rem' }}>
                      {selectedFiles[0].name} ({(selectedFiles[0].size / 1024 / 1024).toFixed(2)} MB)
                    </div>
                  </div>
                ) : (
                  <span className="no-files">No file selected</span>
                )}
              </div>
            </div>
          </div>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <div className="modal-footer">
            <button
              type="button"
              onClick={handleClose}
              className="btn btn-secondary"
              disabled={uploading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={uploading || selectedFiles.length === 0}
            >
              {uploading ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                  <span className="inline-spinner" /> Uploading…
                </span>
              ) : (
                'Submit'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}