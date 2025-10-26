# Authentication System Documentation

## Overview
This application now includes a complete authentication system with login and signup functionality. Users must authenticate before accessing the JBoss Server Manager dashboard.

## Features

### 🔐 **Authentication**
- **Login Page**: Users can sign in with their short name and password
- **Signup Page**: New users can create accounts with short name, full name, and password
- **JWT Token Management**: Automatic token storage and validation
- **Session Persistence**: Users remain logged in across browser sessions
- **Automatic Logout**: Tokens are validated and expired tokens are automatically cleared

### 🛡️ **Security**
- **Protected Routes**: All server management functionality requires authentication
- **API Authentication**: All API calls include authentication headers
- **Token Validation**: JWT tokens are validated client-side for expiration
- **Secure Storage**: Tokens are stored in localStorage with proper cleanup

### 🎨 **User Experience**
- **Modern UI**: Clean, responsive design with gradient backgrounds
- **Form Validation**: Client-side validation with helpful error messages
- **Loading States**: Visual feedback during authentication processes
- **Error Handling**: Comprehensive error messages for various scenarios
- **User Info**: Display current user information in the header

## API Endpoints

### Login
**POST** `http://localhost:8081/api/auth/login`
```json
{
    "shortName": "admin",
    "password": "admin123"
}
```

**Response:**
```json
{
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "shortName": "admin",
    "fullName": "System Administrator",
    "role": "ADMIN",
    "expiresIn": 86400
}
```

### Register/Signup
**POST** `http://localhost:8081/api/auth/register`
```json
{
    "shortName": "john",
    "fullName": "John Doe",
    "password": "john123"
}
```

**Response:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "shortName": "john",
    "fullName": "John Doe",
    "role": "USER",
    "expiresIn": 86400
}
```

## Application Flow

1. **First Visit**: Users are redirected to `/login`
2. **Login/Signup**: Users can authenticate or create new accounts
3. **Token Storage**: JWT tokens are stored in localStorage
4. **Home Access**: Authenticated users access the server management dashboard
5. **API Calls**: All server operations include authentication headers
6. **Logout**: Users can sign out, clearing their session

## File Structure

```
src/
├── components/
│   ├── Login.jsx          # Login page component
│   ├── Signup.jsx         # Signup page component
│   ├── Home.jsx           # Main dashboard (protected)
│   ├── Header.jsx         # Header with user info and logout
│   ├── ProtectedRoute.jsx # Route protection wrapper
│   ├── Auth.css          # Authentication styling
│   └── Header.css        # Header component styling
├── context/
│   └── AuthContext.jsx   # Authentication state management
└── api/
    └── servers.js        # Updated with auth headers
```

## Validation Rules

### Login
- Short name: Required, minimum 3 characters
- Password: Required

### Signup
- Short name: Required, 3-20 characters
- Full name: Required, minimum 2 characters
- Password: Required, minimum 6 characters
- Confirm password: Must match password

## Error Handling

The application handles various error scenarios:
- **Network errors**: Connection issues with the backend
- **Validation errors**: Invalid input data
- **Authentication errors**: Invalid credentials
- **Authorization errors**: Expired or invalid tokens
- **User exists errors**: Duplicate account creation attempts

## Usage

1. Start the backend server on port 8081
2. Run `npm run dev` to start the frontend
3. Navigate to `http://localhost:5175` (or assigned port)
4. Create an account or login with existing credentials
5. Access the server management dashboard

## Default Credentials

You can test with the default admin account:
- **Short Name**: `admin`  
- **Password**: `admin123`

## Security Considerations

- Tokens are stored in localStorage (consider httpOnly cookies for production)
- Client-side token validation (server-side validation is still required)
- CORS configuration needed for cross-origin requests
- HTTPS recommended for production deployments