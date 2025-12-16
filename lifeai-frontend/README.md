# LifeAI Frontend

Frontend application for the LifeAI project built with React, TypeScript, Vite, and Tailwind CSS.

## 📋 Table of Contents

- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Development](#development)
- [Production Build](#production-build)
- [Environment Configuration](#environment-configuration)
- [API Communication](#api-communication)
- [CORS Configuration](#cors-configuration)
- [Deployment](#deployment)
- [Technologies](#technologies)
- [Features](#features)

## Project Structure

```
src/
├── components/        # Reusable React components
├── hooks/            # Custom React hooks
├── pages/            # Page components
├── services/         # API clients and external services
├── types/            # TypeScript type definitions
├── App.tsx           # Main App component
├── main.tsx          # Entry point
└── index.css         # Global styles (Tailwind CSS)

dist/                 # Production build output (generated after build)
```

## Getting Started

### Prerequisites

- **Node.js**: 16.x or higher
- **npm**: 8.x or higher

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd lifeai-frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create environment configuration:
```bash
cp .env.example .env
```

## Development

### Start Development Server

```bash
npm run dev
```

The application will be available at **`http://localhost:3000`**

**Features:**
- Hot Module Replacement (HMR) for instant updates
- API proxy to `http://localhost:8080`
- CORS headers automatically configured for development

### Code Linting

```bash
npm run lint
```

Ensures code quality and consistency with ESLint rules.

## Production Build

### Build for Production

```bash
npm run build
```

This command:
1. Checks TypeScript compilation (`tsc -b`)
2. Builds optimized production bundle with Vite
3. Generates minified assets in `dist/` directory
4. Outputs sourcemaps disabled for security

### Preview Production Build

```bash
npm run preview
```

Test the production build locally before deployment.

## Environment Configuration

### Environment Variables

Create a `.env` file in the project root based on `.env.example`:

```env
VITE_API_URL=http://localhost:8080
VITE_APP_NAME=LifeAI
VITE_APP_ENV=production
```

### Variable Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `VITE_API_URL` | Backend API base URL | `http://localhost:8080` or `https://api.example.com` |
| `VITE_APP_NAME` | Application display name | `LifeAI` |
| `VITE_APP_ENV` | Environment identifier | `development` or `production` |

### Environment Specific Configuration

#### Development (`.env.development`)
```env
VITE_API_URL=http://localhost:8080
VITE_APP_ENV=development
```

#### Production (`.env.production`)
```env
VITE_API_URL=https://api.example.com
VITE_APP_ENV=production
```

## API Communication

### How It Works

The frontend communicates with the Spring Boot backend via HTTP requests using Axios.

**Development Mode:**
- Vite proxy routes requests from `/api` to the configured `VITE_API_URL`
- No CORS issues in development

**Production Mode:**
- Direct HTTP requests to `VITE_API_URL`
- Backend must handle CORS headers

### API Client

Located in `src/services/api.ts`:

```typescript
import { apiClient } from './services/api'

// GET request
const response = await apiClient.get('/api/timeline')

// POST request
const response = await apiClient.post('/api/events', { data })

// PATCH request
const response = await apiClient.patch('/api/recommendations/123/done')
```

### Error Handling

The API client includes interceptors for handling errors:
- 401 Unauthorized responses
- Request timeout (10 seconds)
- Error response parsing

## CORS Configuration

### Development Mode

CORS is handled by Vite's proxy configuration in `vite.config.ts`. No additional setup needed.

### Production Mode

The backend must be configured to accept requests from the frontend domain.

**Spring Boot CORS Configuration:**

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("https://example.com", "http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

Ensure your backend is configured to accept requests from your frontend deployment domain.

## Deployment

### Docker Deployment

The project includes a `Dockerfile` for containerization:

```bash
# Build Docker image
docker build -t lifeai-frontend .

# Run container
docker run -p 3000:80 lifeai-frontend
```

### Static Hosting Deployment

The `dist/` folder contains static files ready for deployment:

1. **Build the application:**
   ```bash
   npm run build
   ```

2. **Deploy to static hosting:**
   - Upload `dist/` contents to your hosting provider
   - Ensure environment variables are properly configured
   - Test the deployment with your backend URL

### Deployment Checklist

- [ ] Environment variables configured for production
- [ ] Backend API URL set correctly
- [ ] CORS enabled on backend for your domain
- [ ] Production build tested with `npm run preview`
- [ ] Sourcemaps disabled in production (already configured)
- [ ] ESLint check passes: `npm run lint`
- [ ] Build output verified
- [ ] Backend server running and accessible
- [ ] HTTPS enabled for production

## Technologies

- **React 18**: Modern UI library with hooks
- **TypeScript**: Type-safe development
- **Vite**: Ultra-fast build tool and dev server
- **Tailwind CSS**: Utility-first CSS framework
- **Axios**: HTTP client with interceptors
- **ESLint**: Code quality and consistency
- **PostCSS**: CSS preprocessing

## Features

✨ **Development Experience:**
- Fast HMR (Hot Module Replacement)
- Type-safe development with TypeScript
- Instant feedback during development

🎨 **UI/UX:**
- Responsive design with Tailwind CSS
- Modern component architecture
- Custom React hooks for reusability

🔌 **API Integration:**
- Axios client with interceptors
- Error handling and timeout management
- Proxy configuration for development

📦 **Production Ready:**
- Optimized production build
- Minified assets
- Environment-based configuration
- Docker support
