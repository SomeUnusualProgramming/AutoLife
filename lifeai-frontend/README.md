# LifeAI Frontend

Frontend application for the LifeAI project built with React, TypeScript, Vite, and Tailwind CSS.

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
```

## Getting Started

### Prerequisites

- Node.js 16+ and npm

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

The application will be available at `http://localhost:3000` with API proxy to `http://localhost:8080`.

### Build

```bash
npm run build
```

### Lint

```bash
npm run lint
```

### Preview Build

```bash
npm run preview
```

## Environment Variables

Create a `.env.local` file based on `.env.example`:

```env
VITE_API_URL=http://localhost:8080
```

## API Communication

The frontend communicates with the Spring Boot backend via HTTP requests. The Vite dev server includes a proxy configuration that routes `/api` requests to `http://localhost:8080`.

Example:
```typescript
import { apiClient } from './services/api'

const response = await apiClient.get('/greet')
```

## Technologies

- **React 18**: UI library
- **TypeScript**: Type-safe development
- **Vite**: Fast build tool and dev server
- **Tailwind CSS**: Utility-first CSS framework
- **Axios**: HTTP client
- **ESLint**: Code linting

## Features

- Fast HMR (Hot Module Replacement)
- Type-safe development with TypeScript
- Responsive design with Tailwind CSS
- API client with interceptors
- Custom hooks for common patterns
