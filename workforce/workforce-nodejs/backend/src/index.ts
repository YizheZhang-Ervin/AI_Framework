import dotenv from 'dotenv';
dotenv.config();
import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import chatRoutes from './routes/chat.js';
import expertRoutes from './routes/expert.js';
import { initDB } from './db/init.js';
import { initLLM } from './ai/llmService.js';

const app = express();
const PORT = process.env.PORT || 3001;

// Initialize database
initDB();

// Middleware
app.use(cors({ origin: 'http://localhost:5173', credentials: true }));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Routes
app.use('/api/chat', chatRoutes);
app.use('/api/experts', expertRoutes);

// Health check
app.get('/health', (_req: Request, res: Response) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// 404 handler
app.use((_req: Request, res: Response) => {
  res.status(404).json({ error: 'Not found' });
});

// Error handler
app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Internal server error' });
});

// Start server
async function startServer() {
  await initLLM();
  app.listen(PORT, () => {
    console.log(`AI Assistant server running on port ${PORT}`);
  });
}
startServer();

export default app;