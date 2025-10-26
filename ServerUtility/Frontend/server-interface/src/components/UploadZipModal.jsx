import { useState, useRef } from 'react'
import './UploadZipModal.css'

const UploadZipModal = ({ isOpen, onClose, onUpload }) => {
  const [selectedFile, setSelectedFile] = useState(null)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [error, setError] = useState('')
  const [dragActive, setDragActive] = useState(false)
  const fileInputRef = useRef(null)

  if (!isOpen) return null

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !isUploading) {
      handleClose()
    }
  }

  const handleClose = () => {
    if (!isUploading) {
      setSelectedFile(null)
      setError('')
      setUploadProgress(0)
      onClose()
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Escape' && !isUploading) {
      handleClose()
    }
  }

  const validateFile = (file) => {
    // Check file type
    const allowedTypes = [
      'application/zip',
      'application/x-zip-compressed',
      'application/x-zip',
      'application/octet-stream'
    ]
    
    const isZip = allowedTypes.includes(file.type) || file.name.toLowerCase().endsWith('.zip')
    
    if (!isZip) {
      setError('Please select a ZIP file (.zip)')
      return false
    }

    // Check file size (max 500MB)
    const maxSize = 500 * 1024 * 1024 // 500MB in bytes
    if (file.size > maxSize) {
      setError('File size must be less than 500MB')
      return false
    }

    setError('')
    return true
  }

  const handleFileSelect = (file) => {
    if (validateFile(file)) {
      setSelectedFile(file)
    }
  }

  const handleFileInputChange = (e) => {
    const file = e.target.files?.[0]
    if (file) {
      handleFileSelect(file)
    }
  }

  const handleDrag = (e) => {
    e.preventDefault()
    e.stopPropagation()
  }

  const handleDragIn = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(true)
  }

  const handleDragOut = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)

    const files = e.dataTransfer.files
    if (files && files.length > 0) {
      handleFileSelect(files[0])
    }
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const handleUpload = async () => {
    if (!selectedFile) return

    setIsUploading(true)
    setUploadProgress(0)
    setError('')

    try {
      // Simulate upload progress
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval)
            return prev
          }
          return prev + Math.random() * 15
        })
      }, 200)

      // Call the upload function passed from parent
      const result = await onUpload(selectedFile)
      
      clearInterval(progressInterval)
      setUploadProgress(100)

      // Close modal after successful upload
      setTimeout(() => {
        handleClose()
      }, 1000)

    } catch (error) {
      setError(error.message || 'Upload failed. Please try again.')
      setUploadProgress(0)
    } finally {
      setIsUploading(false)
    }
  }

  const openFileDialog = () => {
    fileInputRef.current?.click()
  }

  return (
    <div 
      className="upload-modal-backdrop" 
      onClick={handleBackdropClick}
      onKeyDown={handleKeyDown}
      tabIndex={-1}
    >
      <div className="upload-modal-content" role="dialog" aria-modal="true">
        <div className="upload-modal-header">
          <h3 className="upload-modal-title">Upload Server ZIP File</h3>
          {!isUploading && (
            <button 
              className="upload-modal-close-button"
              onClick={handleClose}
              aria-label="Close modal"
            >
              ×
            </button>
          )}
        </div>
        
        <div className="upload-modal-body">
          {error && (
            <div className="upload-error-message" role="alert">
              {error}
            </div>
          )}

          <div 
            className={`upload-drop-zone ${dragActive ? 'drag-active' : ''} ${selectedFile ? 'has-file' : ''}`}
            onDragEnter={handleDragIn}
            onDragLeave={handleDragOut}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            onClick={openFileDialog}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".zip,application/zip,application/x-zip-compressed"
              onChange={handleFileInputChange}
              style={{ display: 'none' }}
              disabled={isUploading}
            />

            {!selectedFile ? (
              <div className="upload-drop-content">
                <div className="upload-icon">📁</div>
                <h4>Drop your ZIP file here</h4>
                <p>or <span className="upload-browse-text">browse files</span></p>
                <small>Supports ZIP files up to 500MB</small>
              </div>
            ) : (
              <div className="upload-file-info">
                <div className="upload-file-icon">📦</div>
                <div className="upload-file-details">
                  <div className="upload-file-name">{selectedFile.name}</div>
                  <div className="upload-file-size">{formatFileSize(selectedFile.size)}</div>
                </div>
                {!isUploading && (
                  <button 
                    className="upload-remove-file"
                    onClick={(e) => {
                      e.stopPropagation()
                      setSelectedFile(null)
                      setError('')
                    }}
                    title="Remove file"
                  >
                    ×
                  </button>
                )}
              </div>
            )}
          </div>

          {isUploading && (
            <div className="upload-progress">
              <div className="upload-progress-bar">
                <div 
                  className="upload-progress-fill"
                  style={{ width: `${uploadProgress}%` }}
                ></div>
              </div>
              <div className="upload-progress-text">
                Uploading... {Math.round(uploadProgress)}%
              </div>
            </div>
          )}
        </div>
        
        <div className="upload-modal-footer">
          <button 
            className="upload-modal-button upload-cancel"
            onClick={handleClose}
            disabled={isUploading}
          >
            {isUploading ? 'Uploading...' : 'Cancel'}
          </button>
          <button 
            className="upload-modal-button upload-confirm"
            onClick={handleUpload}
            disabled={!selectedFile || isUploading}
          >
            {isUploading ? 'Uploading...' : 'Upload ZIP'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default UploadZipModal