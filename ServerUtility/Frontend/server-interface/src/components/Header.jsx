import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import ConfirmationModal from './ConfirmationModal'
import './Header.css'

const Header = () => {
  const { user, logout } = useAuth()
  const [showLogoutModal, setShowLogoutModal] = useState(false)

  const handleLogoutClick = () => {
    setShowLogoutModal(true)
  }

  const handleConfirmLogout = () => {
    logout()
    setShowLogoutModal(false)
  }

  const handleCancelLogout = () => {
    setShowLogoutModal(false)
  }

  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-title">
          <h1>JBoss Server Manager</h1>
          <p className="read-the-docs">Monitor and control JBoss servers from a simple UI.</p>
        </div>
        
        <div className="header-user">
          <div className="user-info">
            <span className="user-name">{user?.fullName}</span>
            <span className="user-role">{user?.role}</span>
          </div>
          <button 
            onClick={handleLogoutClick} 
            className="logout-button"
            title="Sign out"
          >
            Sign Out
          </button>
        </div>
      </div>

      <ConfirmationModal
        isOpen={showLogoutModal}
        onClose={handleCancelLogout}
        onConfirm={handleConfirmLogout}
        title="Sign Out"
        message={`Are you sure you want to sign out? You'll need to log in again to access the server manager.`}
        confirmText="Sign Out"
        cancelText="Cancel"
        type="warning"
      />
    </header>
  )
}

export default Header