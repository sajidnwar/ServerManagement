import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Auth.css'

const Signup = () => {
  const [formData, setFormData] = useState({
    shortName: '',
    fullName: '',
    password: '',
    confirmPassword: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  const { register, error, clearError } = useAuth()
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
    // Short name validation
    if (!formData.shortName.trim()) {
      setFormError('Short name is required')
      return false
    }
    
    if (formData.shortName.length < 3 || formData.shortName.length > 20) {
      setFormError('Short name must be between 3 and 20 characters')
      return false
    }

    // Full name validation
    if (!formData.fullName.trim()) {
      setFormError('Full name is required')
      return false
    }

    if (formData.fullName.length < 2) {
      setFormError('Full name must be at least 2 characters')
      return false
    }

    // Password validation
    if (!formData.password) {
      setFormError('Password is required')
      return false
    }

    if (formData.password.length < 6) {
      setFormError('Password must be at least 6 characters')
      return false
    }

    // Confirm password validation
    if (formData.password !== formData.confirmPassword) {
      setFormError('Passwords do not match')
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
      const result = await register(formData.shortName, formData.fullName, formData.password)
      
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
          <h2>Create Account</h2>
          <p>Join us to manage your JBoss servers efficiently</p>
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
              placeholder="Enter a short name (3-20 characters)"
              disabled={isSubmitting}
              autoComplete="username"
              required
            />
            <small className="form-hint">This will be your login username</small>
          </div>

          <div className="form-group">
            <label htmlFor="fullName">Full Name</label>
            <input
              type="text"
              id="fullName"
              name="fullName"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="Enter your full name"
              disabled={isSubmitting}
              autoComplete="name"
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
              placeholder="Enter your password (min 6 characters)"
              disabled={isSubmitting}
              autoComplete="new-password"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Confirm your password"
              disabled={isSubmitting}
              autoComplete="new-password"
              required
            />
          </div>

          <button
            type="submit"
            className="auth-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="auth-link">
              Sign in here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default Signup