import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext()

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  // Check for existing token on mount
  useEffect(() => {
    const token = localStorage.getItem('authToken')
    const userData = localStorage.getItem('userData')
    
    if (token && userData) {
      try {
        const parsedUserData = JSON.parse(userData)
        // Check if token is expired
        const tokenPayload = JSON.parse(atob(token.split('.')[1]))
        const currentTime = Date.now() / 1000
        
        if (tokenPayload.exp > currentTime) {
          setUser(parsedUserData)
        } else {
          // Token expired, clear storage
          localStorage.removeItem('authToken')
          localStorage.removeItem('userData')
        }
      } catch (error) {
        console.error('Error parsing stored user data:', error)
        localStorage.removeItem('authToken')
        localStorage.removeItem('userData')
      }
    }
    
    setIsLoading(false)
  }, [])

  const login = async (shortName, password) => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await fetch('http://localhost:8081/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ shortName, password }),
      })

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.message || 'Login failed')
      }

      // Store token and user data
      localStorage.setItem('authToken', data.token)
      localStorage.setItem('userData', JSON.stringify({
        shortName: data.shortName,
        fullName: data.fullName,
        role: data.role,
      }))

      setUser({
        shortName: data.shortName,
        fullName: data.fullName,
        role: data.role,
      })

      return { success: true }
    } catch (error) {
      setError(error.message)
      return { success: false, error: error.message }
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (shortName, fullName, password) => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await fetch('http://localhost:8081/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ shortName, fullName, password }),
      })

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.message || 'Registration failed')
      }

      // Store token and user data
      localStorage.setItem('authToken', data.token)
      localStorage.setItem('userData', JSON.stringify({
        shortName: data.shortName,
        fullName: data.fullName,
        role: data.role,
      }))

      setUser({
        shortName: data.shortName,
        fullName: data.fullName,
        role: data.role,
      })

      return { success: true }
    } catch (error) {
      setError(error.message)
      return { success: false, error: error.message }
    } finally {
      setIsLoading(false)
    }
  }

  const logout = () => {
    localStorage.removeItem('authToken')
    localStorage.removeItem('userData')
    setUser(null)
    setError(null)
  }

  const clearError = () => {
    setError(null)
  }

  const getToken = () => {
    return localStorage.getItem('authToken')
  }

  const value = {
    user,
    isLoading,
    error,
    login,
    register,
    logout,
    clearError,
    getToken,
    isAuthenticated: !!user,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}