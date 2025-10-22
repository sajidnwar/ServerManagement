import { createContext, useContext, useReducer, useCallback } from 'react'
import { fetchServers, fetchAllServers, startServer, stopServer } from '../api/servers'

// Initial state
const initialState = {
  servers: [],
  summary: { total: 0, running: 0 },
  loading: {
    summary: false,
    servers: false,
    starting: null,
    uploading: new Set()
  },
  errors: {
    summary: null,
    upload: null
  },
  ui: {
    visible: false,
    transitioning: false,
    startingMessage: '',
    uploadStatus: ''
  },
  busyServers: new Set()
}

// Action types
const ActionTypes = {
  SET_LOADING: 'SET_LOADING',
  SET_SERVERS: 'SET_SERVERS',
  SET_SUMMARY: 'SET_SUMMARY',
  SET_ERROR: 'SET_ERROR',
  SET_UI_STATE: 'SET_UI_STATE',
  ADD_BUSY_SERVER: 'ADD_BUSY_SERVER',
  REMOVE_BUSY_SERVER: 'REMOVE_BUSY_SERVER',
  UPDATE_SERVER: 'UPDATE_SERVER',
  CLEAR_MESSAGES: 'CLEAR_MESSAGES'
}

// Reducer
function serverReducer(state, action) {
  switch (action.type) {
    case ActionTypes.SET_LOADING:
      return {
        ...state,
        loading: { ...state.loading, ...action.payload }
      }
    
    case ActionTypes.SET_SERVERS:
      return {
        ...state,
        servers: action.payload
      }
    
    case ActionTypes.SET_SUMMARY:
      return {
        ...state,
        summary: action.payload
      }
    
    case ActionTypes.SET_ERROR:
      return {
        ...state,
        errors: { ...state.errors, ...action.payload }
      }
    
    case ActionTypes.SET_UI_STATE:
      return {
        ...state,
        ui: { ...state.ui, ...action.payload }
      }
    
    case ActionTypes.ADD_BUSY_SERVER:
      return {
        ...state,
        busyServers: new Set([...state.busyServers, action.payload])
      }
    
    case ActionTypes.REMOVE_BUSY_SERVER:
      const newBusyServers = new Set(state.busyServers)
      newBusyServers.delete(action.payload)
      return {
        ...state,
        busyServers: newBusyServers
      }
    
    case ActionTypes.UPDATE_SERVER:
      return {
        ...state,
        servers: state.servers.map(server =>
          server.id === action.payload.id || server.name === action.payload.name
            ? { ...server, ...action.payload.updates }
            : server
        )
      }
    
    case ActionTypes.CLEAR_MESSAGES:
      return {
        ...state,
        ui: {
          ...state.ui,
          startingMessage: '',
          uploadStatus: ''
        },
        errors: {
          ...state.errors,
          summary: null,
          upload: null
        }
      }
    
    default:
      return state
  }
}

// Context
const ServerContext = createContext()

// Provider component
export function ServerProvider({ children }) {
  const [state, dispatch] = useReducer(serverReducer, initialState)

  // Load summary data
  const loadSummary = useCallback(async () => {
    dispatch({ type: ActionTypes.SET_LOADING, payload: { summary: true } })
    dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: null } })
    
    try {
      const items = await fetchServers()
      const runningServers = items.filter((s) => s.state === 'running')
      const running = runningServers.length
      
      dispatch({ type: ActionTypes.SET_SERVERS, payload: items })
      dispatch({ type: ActionTypes.SET_SUMMARY, payload: { total: items.length, running } })
    } catch (err) {
      console.error('Failed to load summary', err)
      dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: err.message || 'Failed to load server data' } })
    } finally {
      dispatch({ type: ActionTypes.SET_LOADING, payload: { summary: false } })
    }
  }, [])

  // Load all servers
  const loadAllServers = useCallback(async () => {
    dispatch({ type: ActionTypes.SET_UI_STATE, payload: { transitioning: true } })
    dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: null } })
    
    try {
      const items = await fetchAllServers()
      const runningCount = items.filter(s => s.state === 'running').length
      
      dispatch({ type: ActionTypes.SET_SERVERS, payload: items })
      dispatch({ type: ActionTypes.SET_SUMMARY, payload: { total: items.length, running: runningCount } })
    } catch (err) {
      console.error('Failed to load all servers', err)
      dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: err.message || 'Failed to load server list' } })
    } finally {
      dispatch({ type: ActionTypes.SET_UI_STATE, payload: { transitioning: false } })
    }
  }, [])

  // Start server
  const handleStartServer = useCallback(async (serverName) => {
    if (state.busyServers.has(serverName) || state.loading.starting) return

    dispatch({ type: ActionTypes.ADD_BUSY_SERVER, payload: serverName })
    dispatch({ type: ActionTypes.SET_LOADING, payload: { starting: serverName } })
    dispatch({ type: ActionTypes.SET_UI_STATE, payload: { startingMessage: `Starting server '${serverName}'...` } })
    dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: null } })

    try {
      const updatedServer = await startServer(serverName)
      
      dispatch({ type: ActionTypes.UPDATE_SERVER, payload: { 
        id: serverName, 
        name: serverName, 
        updates: updatedServer 
      } })
      
      const runningCount = state.servers.filter(s => 
        s.state === 'running' || s.id === serverName || s.name === serverName
      ).length
      
      dispatch({ type: ActionTypes.SET_SUMMARY, payload: { ...state.summary, running: runningCount } })
      dispatch({ type: ActionTypes.SET_UI_STATE, payload: { 
        startingMessage: updatedServer.message || `Server '${serverName}' started successfully!` 
      } })
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        dispatch({ type: ActionTypes.SET_UI_STATE, payload: { startingMessage: '' } })
      }, 3000)
      
    } catch (err) {
      console.error('Failed to start server:', err)
      dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: err.message || 'Failed to start server' } })
      dispatch({ type: ActionTypes.SET_UI_STATE, payload: { startingMessage: '' } })
    } finally {
      dispatch({ type: ActionTypes.REMOVE_BUSY_SERVER, payload: serverName })
      dispatch({ type: ActionTypes.SET_LOADING, payload: { starting: null } })
    }
  }, [state.busyServers, state.loading.starting, state.servers, state.summary])

  // Stop server
  const handleStopServer = useCallback(async (serverName) => {
    if (state.busyServers.has(serverName) || state.loading.starting) return
    
    dispatch({ type: ActionTypes.ADD_BUSY_SERVER, payload: serverName })
    dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: null } })

    // Optimistically update UI
    dispatch({ type: ActionTypes.UPDATE_SERVER, payload: {
      id: serverName,
      name: serverName,
      updates: { state: 'stopped', uptime: 0 }
    } })
    dispatch({ type: ActionTypes.SET_SUMMARY, payload: {
      ...state.summary,
      running: Math.max(0, state.summary.running - 1)
    } })

    try {
      const updatedServer = await stopServer(serverName)
      dispatch({ type: ActionTypes.UPDATE_SERVER, payload: {
        id: serverName,
        name: serverName,
        updates: updatedServer
      } })
      dispatch({ type: ActionTypes.SET_SUMMARY, payload: {
        ...state.summary,
        running: Math.max(0, state.summary.running - (updatedServer.state === 'stopped' ? 1 : 0))
      } })
    } catch (err) {
      console.error('Failed to stop server:', err)
      dispatch({ type: ActionTypes.SET_ERROR, payload: { summary: err.message || 'Failed to stop server' } })
      // Revert optimistic update
      dispatch({ type: ActionTypes.UPDATE_SERVER, payload: {
        id: serverName,
        name: serverName,
        updates: { state: 'running', uptime: 1 }
      } })
      dispatch({ type: ActionTypes.SET_SUMMARY, payload: {
        ...state.summary,
        running: state.summary.running + 1
      } })
    } finally {
      dispatch({ type: ActionTypes.REMOVE_BUSY_SERVER, payload: serverName })
    }
  }, [state.busyServers, state.loading.starting, state.summary])

  // Handle upload
  const handleUpload = useCallback((serverName, uploadResult) => {
    console.log(`Files uploaded to ${serverName}:`, uploadResult)
    dispatch({ type: ActionTypes.SET_UI_STATE, payload: { 
      uploadStatus: `Files uploaded successfully to ${serverName}` 
    } })
    
    // Clear upload status after 3 seconds
    setTimeout(() => {
      dispatch({ type: ActionTypes.SET_UI_STATE, payload: { uploadStatus: '' } })
    }, 3000)
  }, [])

  // Handle servers changed (from ServerList)
  const handleServersChanged = useCallback((items) => {
    const running = items.filter((s) => s.state === 'running').length
    dispatch({ type: ActionTypes.SET_SUMMARY, payload: { total: items.length, running } })
  }, [])

  // Toggle server list visibility
  const toggleServerList = useCallback(async () => {
    if (state.ui.visible) {
      // hiding: show loader, fetch latest summary, then hide
      dispatch({ type: ActionTypes.SET_UI_STATE, payload: { transitioning: true } })
      await loadSummary()
      dispatch({ type: ActionTypes.SET_UI_STATE, payload: { visible: false, transitioning: false } })
    } else {
      // showing list: fetch all servers and open
      await loadAllServers()
      dispatch({ type: ActionTypes.SET_UI_STATE, payload: { visible: true } })
    }
  }, [state.ui.visible, loadSummary, loadAllServers])

  const value = {
    // State
    ...state,
    
    // Actions
    loadSummary,
    loadAllServers,
    handleStartServer,
    handleStopServer,
    handleUpload,
    handleServersChanged,
    toggleServerList,
    
    // UI Actions
    setVisible: (visible) => dispatch({ type: ActionTypes.SET_UI_STATE, payload: { visible } }),
    clearMessages: () => dispatch({ type: ActionTypes.CLEAR_MESSAGES })
  }

  return (
    <ServerContext.Provider value={value}>
      {children}
    </ServerContext.Provider>
  )
}

// Custom hook to use server context
export function useServerContext() {
  const context = useContext(ServerContext)
  if (!context) {
    throw new Error('useServerContext must be used within a ServerProvider')
  }
  return context
}