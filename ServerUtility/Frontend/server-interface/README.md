# Server Interface Frontend

A React-based frontend application for managing JBoss/Java servers with authentication and file upload capabilities.

## 🚀 Quick Start

```bash
# Install dependencies
npm install

# Copy environment configuration
cp .env.example .env

# Start development server
npm run dev
```

## ⚙️ Configuration

### Environment Variables

The application uses environment variables for configuration. Copy `.env.example` to `.env` and customize the values:

```bash
# Main API Server
VITE_API_BASE_URL=http://localhost:8081

# Upload Service (can be same as API or different microservice)
VITE_UPLOAD_URL=http://localhost:8081

# Request Configuration
VITE_REQUEST_TIMEOUT=30000
VITE_POLL_INTERVAL=5000
```

### API Configuration

All API configurations are centralized in `src/config/api.js`:

- **API_CONFIG**: Base URLs and timeout settings
- **ENDPOINTS**: All API endpoint definitions
- **Helper Functions**: URL builders and header factories

### Usage Example

```javascript
import { buildUrl, ENDPOINTS, getAuthHeaders } from '../config/api.js'

// Build API URL
const url = buildUrl(ENDPOINTS.SERVERS.LIST)

// Get authenticated headers
const headers = getAuthHeaders()
```

## 🏗️ Architecture

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is enabled on this template. See [this documentation](https://react.dev/learn/react-compiler) for more information.

Note: This will impact Vite dev & build performances.

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.
