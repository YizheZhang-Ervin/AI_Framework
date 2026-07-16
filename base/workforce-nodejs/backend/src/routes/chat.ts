import { Router } from 'express';
import chatController from '../controllers/chatController.js';

const router = Router();

// Session management
router.get('/sessions', chatController.getSessions);
router.post('/sessions', chatController.createSession);
router.delete('/sessions/:id', chatController.deleteSession);

// Messages
router.get('/sessions/:id/messages', chatController.getMessages);

// Non-streaming message endpoint
router.post('/sessions/:id/messages', chatController.sendMessage);

// Streaming (SSE) message endpoint
router.post('/sessions/:id/messages/stream', chatController.sendMessageStream);

export default router;