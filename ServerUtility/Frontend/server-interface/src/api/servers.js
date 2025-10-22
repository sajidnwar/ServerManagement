// API for server management. Connects to backend that manages server instances.

const BASE_URL = 'http://localhost:8081'

// Fetch all servers (called when "Show servers" is clicked)
export async function fetchAllServers() {
  try {
    const response = await fetch(`${BASE_URL}/servers`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      mode: 'cors'
    })
    
    if (!response.ok) {
      if (response.status === 0) {
        throw new Error('Network error: Unable to connect to server.')
      }
      
      // Try to get the error message from response
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `Server error: ${response.status} - ${response.statusText}`)
      } catch (parseError) {
        throw new Error(`Server error: ${response.status} - ${response.statusText}`)
      }
    }
    
    const data = await response.json()
    
    // Transform the array response to match our expected format
    return data.map((server, index) => ({
      id: server.name || `server-${index + 1}`,
      name: server.name || `Server ${index + 1}`,
      host: 'localhost',
      port: server.port || 8080,
      state: server.running ? 'running' : 'stopped',
      uptime: server.running ? 3600 : 0, // Default uptime for running servers
      pid: server.pid,
      path: server.path
    }))
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('CORS error: Unable to connect to server. Please ensure the backend allows cross-origin requests.')
    }
    console.error('Failed to fetch all servers:', error)
    throw error
  }
}

export async function fetchServers() {
  try {
    const response = await fetch(`${BASE_URL}/servers/running`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      mode: 'cors'
    })
    
    // Handle 204 No Content - no servers running
    if (response.status === 204) {
      return [] // Return empty array when no servers are running
    }
    
    if (!response.ok) {
      if (response.status === 0) {
        throw new Error('Network error: Unable to connect to server. Please check if the server is running on port 8081.')
      }
      
      // Handle 404 and other errors - try to get the error message from response
      try {
        const errorData = await response.json()
        console.log('Error response data:', errorData)
        const errorMessage = errorData.message || `Server error: ${response.status} - ${response.statusText}`
        throw new Error(errorMessage)
      } catch (parseError) {
        console.log('Failed to parse error response:', parseError)
        throw new Error(`Server error: ${response.status} - ${response.statusText}`)
      }
    }
    
    const data = await response.json()
    
    // Transform the response to match our expected format
    return [{
      id: data.name || 'server-1',
      name: data.name || 'Unknown Server',
      host: 'localhost',
      port: data.port || 8080,
      state: data.running ? 'running' : 'stopped',
      uptime: data.running ? 3600 : 0, // Default uptime for running servers
      pid: data.pid,
      path: data.path
    }]
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('CORS error: Unable to connect to server. Please ensure the backend allows cross-origin requests.')
    }
    console.error('Failed to fetch servers:', error)
    throw error
  }
}

export async function fetchServer(id) {
  try {
    const response = await fetch(`${BASE_URL}/servers`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      mode: 'cors'
    })
    
    if (!response.ok) {
      if (response.status === 0) {
        throw new Error('Network error: Unable to connect to server.')
      }
      
      // Try to get the error message from response
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `Server error: ${response.status} - ${response.statusText}`)
      } catch (parseError) {
        throw new Error(`Server error: ${response.status} - ${response.statusText}`)
      }
    }
    
    const data = await response.json()
    
    // Find the server that matches the requested id
    const serverData = data.find(server => server.name === id)
    
    if (serverData) {
      return {
        id: serverData.name || id,
        name: serverData.name || 'Unknown Server',
        host: 'localhost',
        port: serverData.port || 8080,
        state: serverData.running ? 'running' : 'stopped',
        uptime: serverData.running ? 3600 : 0,
        pid: serverData.pid,
        path: serverData.path
      }
    }
    return null
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('CORS error: Unable to connect to server. Please ensure the backend allows cross-origin requests.')
    }
    console.error('Failed to fetch server:', error)
    throw error
  }
}

export async function startServer(serverName) {
  try {
    // Call start endpoint
    const response = await fetch(`${BASE_URL}/servers/${encodeURIComponent(serverName)}/start`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      mode: 'cors'
    })
    
    if (!response.ok) {
      if (response.status === 0) {
        throw new Error('Network error: Unable to connect to server.')
      }
      
      // Try to get the error message from response
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `Failed to start server: ${response.status} - ${response.statusText}`)
      } catch (parseError) {
        throw new Error(`Failed to start server: ${response.status} - ${response.statusText}`)
      }
    }
    
    const startMessage = await response.text()
    console.log('Server start initiated:', startMessage)
    
    // Poll status until server is fully started
    return await pollServerStatus(serverName)
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('CORS error: Unable to connect to server. Please ensure the backend allows cross-origin requests.')
    }
    console.error('Failed to start server:', error)
    throw error
  }
}

// Poll server status every 20 seconds until is_started becomes true
export async function pollServerStatus(serverName) {
  return new Promise((resolve, reject) => {
    const checkStatus = async () => {
      try {
        const response = await fetch(`${BASE_URL}/servers/${encodeURIComponent(serverName)}/status`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
          },
          mode: 'cors'
        })
        
        if (!response.ok) {
          if (response.status === 0) {
            throw new Error('Network error: Unable to connect to server.')
          }
          
          try {
            const errorData = await response.json()
            throw new Error(errorData.message || `Failed to check server status: ${response.status} - ${response.statusText}`)
          } catch (parseError) {
            throw new Error(`Failed to check server status: ${response.status} - ${response.statusText}`)
          }
        }
        
        const statusData = await response.json()
        console.log('Server status check:', statusData)
        
        if (statusData.is_started === true) {
          // Server is fully started
          resolve({
            id: serverName,
            name: serverName,
            host: 'localhost',
            port: statusData.port || 8080,
            state: 'running',
            uptime: 1,
            pid: statusData.pid,
            path: statusData.path,
            message: `Server '${serverName}' started successfully`
          })
        } else if (statusData.is_running === true) {
          // Server is running but not fully started yet, continue polling
          console.log('Server is running but not fully started yet, continuing to poll...')
          setTimeout(checkStatus, 5000) // Poll every 5 seconds
        } else {
          // Server failed to start
          reject(new Error(`Server '${serverName}' failed to start properly`))
        }
      } catch (error) {
        console.error('Status check error:', error)
        reject(error)
      }
    }
    
    // Start the first status check immediately
    checkStatus()
  })
}

// Upload deployable files to server
export async function uploadDeployableFiles(serverName, files, deploymentPath) {
  try {
    const formData = new FormData()
    
    // Add all selected files to FormData
    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i])
    }
    
    // Add deployment path to FormData
    if (deploymentPath) {
      formData.append('deploymentPath', deploymentPath)
    }
    
    const response = await fetch(`${BASE_URL}/servers/${encodeURIComponent(serverName)}/upload`, {
      method: 'POST',
      headers: {
        'Access-Control-Allow-Origin': '*'
      },
      mode: 'cors',
      body: formData
    })
    
    if (!response.ok) {
      if (response.status === 0) {
        throw new Error('Network error: Unable to connect to server.')
      }
      
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `Failed to upload files: ${response.status} - ${response.statusText}`)
      } catch (parseError) {
        throw new Error(`Failed to upload files: ${response.status} - ${response.statusText}`)
      }
    }
    
    const result = await response.json()
    return result
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('CORS error: Unable to connect to server. Please ensure the backend allows cross-origin requests.')
    }
    console.error('Failed to upload files:', error)
    throw error
  }
}

// Get server status
export async function getServerStatus(serverName) {
  try {
    const response = await fetch(`${BASE_URL}/servers/${encodeURIComponent(serverName)}/status`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      mode: 'cors'
    })
    
    if (!response.ok) {
      if (response.status === 0) {
        throw new Error('Network error: Unable to connect to server.')
      }
      
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `Failed to get server status: ${response.status} - ${response.statusText}`)
      } catch (parseError) {
        throw new Error(`Failed to get server status: ${response.status} - ${response.statusText}`)
      }
    }
    
    return await response.json()
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('CORS error: Unable to connect to server. Please ensure the backend allows cross-origin requests.')
    }
    console.error('Failed to get server status:', error)
    throw error
  }
}

export async function stopServer(id) {
  try {
    const response = await fetch(`${BASE_URL}/servers/stop`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      mode: 'cors',
      body: JSON.stringify({ id })
    })
    
    if (!response.ok) {
      if (response.status === 0) {
        throw new Error('Network error: Unable to connect to server.')
      }
      
      // Try to get the error message from response
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `Failed to stop server: ${response.status} - ${response.statusText}`)
      } catch (parseError) {
        throw new Error(`Failed to stop server: ${response.status} - ${response.statusText}`)
      }
    }
    
    const data = await response.json()
    
    // Return the server state from the response
    return {
      id: data.name || id,
      name: data.name || 'Unknown Server',
      host: 'localhost',
      port: data.port || 8080,
      state: data.running ? 'running' : 'stopped',
      uptime: data.running ? 3600 : 0,
      pid: data.pid,
      path: data.path
    }
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('CORS error: Unable to connect to server. Please ensure the backend allows cross-origin requests.')
    }
    console.error('Failed to stop server:', error)
    throw error
  }
}
