import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Auth.css'

const Login = () => {
  const [formData, setFormData] = useState({
    shortName: '',
    password: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  const { login, error, clearError } = useAuth()
  const navigate = useNavigate()

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
    
    // Clear errors when user starts typing
    if (formError) setFormError('')
    if (error) clearError()
  }

  const validateForm = () => {
    if (!formData.shortName.trim()) {
      setFormError('Short name is required')
      return false
    }
    
    if (!formData.password) {
      setFormError('Password is required')
      return false
    }

    if (formData.shortName.length < 3) {
      setFormError('Short name must be at least 3 characters')
      return false
    }

    return true
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!validateForm()) return

    setIsSubmitting(true)
    setFormError('')

    try {
      const result = await login(formData.shortName, formData.password)
      
      if (result.success) {
        // Redirect to home page
        navigate('/', { replace: true })
      }
    } catch (error) {
      setFormError('An unexpected error occurred')
    } finally {
      setIsSubmitting(false)
    }
  }

  const displayError = formError || error

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>JBoss Server Manager</h1>
          <h2>Sign In</h2>
          <p>Enter your credentials to access the server management dashboard</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          {displayError && (
            <div className="error-message" role="alert">
              {displayError}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="shortName">Short Name</label>
            <input
              type="text"
              id="shortName"
              name="shortName"
              value={formData.shortName}
              onChange={handleChange}
              placeholder="Enter your short name"
              disabled={isSubmitting}
              autoComplete="username"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Enter your password"
              disabled={isSubmitting}
              autoComplete="current-password"
              required
            />
          </div>

          <button
            type="submit"
            className="auth-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/signup" className="auth-link">
              Sign up here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default Login