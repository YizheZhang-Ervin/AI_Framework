# OpenWorkForce - Digital Employee Platform

A digital employee platform based on AI, featuring expert simulation, skill management, and knowledge base capabilities.

## Tech Stack

### Backend
- **Node.js** with Express
- **SQLite** database (better-sqlite3)
- **node-llama-cpp** for local LLM inference

### Frontend
- **Vue.js 3** with Vite
- **Vue Router 4** for routing
- **Vuex 4** for state management

## Project Structure

```
backend/
├── src/
│   ├── controllers/       # Route controllers
│   ├── routes/           # API routes
│   ├── services/         # Business logic (LLM service)
│   ├── middleware/       # Express middleware
│   ├── db/              # Database initialization and seeding
│   │   ├── init.ts      # Database schema
│   │   └── seed.ts      # Initial data
│   └── utils/           # Utility functions
├── data/                # SQLite database files
└── package.json

frontend/
├── src/
│   ├── views/           # Page views
│   ├── components/      # Vue components
│   ├── router/          # Vue Router configuration
│   ├── store/           # Vuex store
│   ├── api/             # API client
│   └── assets/          # Static assets
└── package.json
```

## Database Schema

### Experts Table
- `id` - Expert ID (Primary Key)
- `name` - Expert name
- `title` - Professional title
- `department` - Department
- `description` - Description

### Skills Table
- `id` - Skill ID (Primary Key)
- `name` - Skill name
- `description` - Description
- `prompt_template` - LLM prompt template
- `api_endpoint` - Optional API endpoint
- `api_method` - HTTP method

### Chat Tables
- `chat_sessions` - Conversation sessions
- `chat_messages` - Individual messages within sessions

## API Endpoints

### Experts
- `GET /api/experts` - Get all experts
- `GET /api/experts/:id` - Get expert by ID
- `POST /api/experts` - Create expert
- `PUT /api/experts/:id` - Update expert
- `DELETE /api/experts/:id` - Delete expert
- `POST /api/experts/:id/skills` - Assign skill to expert
- `DELETE /api/experts/:id/skills/:skillId` - Remove skill from expert

### Skills
- `GET /api/skills` - Get all skills
- `GET /api/skills/:id` - Get skill by ID
- `POST /api/skills` - Create skill
- `PUT /api/skills/:id` - Update skill
- `DELETE /api/skills/:id` - Delete skill

### Chat
- `GET /api/chat` - Get all chat sessions
- `GET /api/chat/:id` - Get chat session by ID
- `POST /api/chat` - Create chat session
- `POST /api/chat/:id/messages` - Send message to chat session

## Setup Instructions

### Backend
```bash
cd backend
npm install
npm run dev
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Deployment Guide

### Prerequisites

- **Node.js** >= 18.x (recommended: 20.x LTS)
- **npm** >= 9.x
- **Git** (for cloning the repository)

### 1. Clone & Install

```bash
# Clone the repository
git clone https://github.com/your-org/OpenWorkForce.git
cd OpenWorkForce

# Install backend dependencies
cd backend
npm install
cd ..

# Install frontend dependencies
cd frontend
npm install
cd ..
```

### 2. Environment Configuration

Copy the example environment file and configure it:

```bash
cd backend
cp .env.example .env
```

Edit `.env` with your settings:

```ini
# LLM Provider: "local" (local node-llama-cpp) or "openai" (OpenAI-compatible API)
LLM_PROVIDER=openai

# OpenAI-compatible API config (used when LLM_PROVIDER=openai)
OPENAI_API_KEY=your_api_key_here
OPENAI_BASE_URL=https://open.bigmodel.cn/api/paas/v4
OPENAI_MODEL=GLM-4.7-Flash

# Local model config (used when LLM_PROVIDER=local)
LLM_MODEL_PATH=/path/to/your/model.gguf

# Server port
PORT=3001
```

#### LLM Provider Options

| Provider | Description | Configuration |
|----------|-------------|---------------|
| `openai` | Uses any OpenAI-compatible API (recommended) | Set `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL` |
| `local` | Runs LLM locally via `node-llama-cpp` | Download a GGUF model and set `LLM_MODEL_PATH` |

### 3. Development Mode

Start both servers in separate terminals:

**Terminal 1 - Backend:**
```bash
cd backend
npm run dev
```
The backend starts on `http://localhost:3001` with hot-reload via `tsx watch`.

**Terminal 2 - Frontend:**
```bash
cd frontend
npm run dev
```
The frontend starts on `http://localhost:5173` with HMR via Vite.

> The frontend dev server automatically proxies `/api` requests to the backend at `http://localhost:3001`.

### 4. Production Build

#### Build the Backend

```bash
cd backend
npm run build
# Output: dist/ directory with compiled JavaScript
```

#### Build the Frontend

```bash
cd frontend
npm run build
# Output: dist/ directory with static files
```

#### Run in Production

**Option A - Run backend directly:**
```bash
cd backend
npm start
# Runs: node dist/index.js
```

**Option B - Use a process manager (PM2 recommended):**
```bash
# Install PM2 globally
npm install -g pm2

# Start backend with PM2
pm2 start backend/dist/index.js --name openworkforce-backend

# Save PM2 process list for auto-restart on reboot
pm2 save
pm2 startup
```

### 5. Serve Frontend in Production

The built frontend files are static and can be served by any web server.

#### Option A - Nginx (Recommended)

Install Nginx and create a site configuration:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # Frontend static files
    root /path/to/OpenWorkForce/frontend/dist;
    index index.html;

    # SPA fallback - all routes serve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API proxy to backend
    location /api/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Health check endpoint
    location /health {
        proxy_pass http://127.0.0.1:3001;
    }
}
```

#### Option B - Serve with Vite preview (simple, dev-only)

```bash
cd frontend
npm run build
npm run preview
# Serves on http://localhost:4173
```

#### Option C - Serve with backend (embed static files)

Modify the backend to serve frontend static files in production. Add this to `src/index.ts`:

```typescript
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Serve frontend static files in production
if (process.env.NODE_ENV === 'production') {
  app.use(express.static(path.join(__dirname, '../../frontend/dist')));

  // SPA fallback
  app.get('*', (_req, res) => {
    res.sendFile(path.join(__dirname, '../../frontend/dist/index.html'));
  });
}
```

### 6. Database

- The application uses **SQLite** via `better-sqlite3`.
- The database file is automatically created at `backend/data/database.sqlite` on first startup.
- No external database server is required.
- To reset the database, simply delete the file: `rm backend/data/database.sqlite` (it will be recreated).
- **Backup**: Periodically copy the `backend/data/database.sqlite` file to a safe location.

### 7. Verification

After deployment, verify the setup:

1. **Health check:** `curl http://localhost:3001/health`
   - Expected: `{"status":"OK","timestamp":"..."}`
2. **API test:** `curl http://localhost:3001/api/experts`
   - Expected: JSON array of experts (may be empty initially)

### 8. Troubleshooting

| Issue | Solution |
|-------|----------|
| `Cannot find module` | Run `npm install` in both `backend/` and `frontend/` |
| Backend port conflict | Change `PORT` in `backend/.env` or use `PORT=3002 npm start` |
| LLM connection timeout | Verify `OPENAI_API_KEY` and `OPENAI_BASE_URL` are correct |
| Local model not loading | Ensure `LLM_MODEL_PATH` points to a valid GGUF file |
| Database errors | Delete `backend/data/database.sqlite` and restart |
| CORS errors | Check `cors` origin in `backend/src/index.ts` matches frontend URL |

### 9. Update & Maintain

```bash
# Pull latest code
git pull origin main

# Update dependencies
cd backend && npm install && cd ..
cd frontend && npm install && cd ..

# Rebuild for production
cd backend && npm run build && cd ..
cd frontend && npm run build && cd ..

# Restart backend process
pm2 restart openworkforce-backend
```

## Features

1. **Expert Library** - Manage digital employee experts with professional roles
2. **Skill Library** - Configure expert skills with prompt templates and API endpoints
3. **Knowledge Base** - Manage professional knowledge (short markdown, long PageIndex)
4. **Chat Interface** - Interactive conversation with expert selection
5. **Expert Distillation** - Create experts from uploaded documents (planned)
