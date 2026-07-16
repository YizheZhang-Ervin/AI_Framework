import { Request, Response, NextFunction } from 'express';
import { getDB } from '../db/init.js';
import { generateResponse, generateResponseStream } from '../ai/llmService.js';

// Get sessions
export const getSessions = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const db = getDB();
    const sessions = db.prepare('SELECT * FROM chat_sessions ORDER BY updated_at DESC').all() as any[];
    // Parse expert_info JSON for each session
    const result = sessions.map(s => {
      if (s.expert_info) {
        try {
          s.expert_info = JSON.parse(s.expert_info);
        } catch {
          s.expert_info = null;
        }
      }
      return s;
    });
    res.json(result);
  } catch (error) {
    next(error);
  }
};

// Get messages for a session
export const getMessages = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const db = getDB();
    const messages = db.prepare(
      'SELECT * FROM chat_messages WHERE session_id = ? ORDER BY created_at ASC'
    ).all(req.params.id);
    res.json(messages);
  } catch (error) {
    next(error);
  }
};

// Create new session
export const createSession = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const db = getDB();
    const id = 'session-' + Date.now();
    const title = req.body.title || '新对话';
    const expert = req.body.expert || null;
    const expertInfo = expert ? JSON.stringify({
      id: expert.id,
      name: expert.name,
      title: expert.title,
      description: expert.description || '',
      skills: expert.skills || []
    }) : null;
    db.prepare('INSERT INTO chat_sessions (id, title, expert_info) VALUES (?, ?, ?)').run(id, title, expertInfo);
    let session = db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(id) as any;
    if (session && session.expert_info) {
      try {
        session.expert_info = JSON.parse(session.expert_info);
      } catch {
        session.expert_info = null;
      }
    }
    res.status(201).json(session);
  } catch (error) {
    next(error);
  }
};

// Delete session
export const deleteSession = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const db = getDB();
    const result = db.prepare('DELETE FROM chat_sessions WHERE id = ?').run(req.params.id);
    if (result.changes === 0) {
      return res.status(404).json({ error: 'Session not found' });
    }
    res.json({ message: 'Session deleted' });
  } catch (error) {
    next(error);
  }
};

// Send message and get AI response
export const sendMessage = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const db = getDB();
    const { session_id, message, expert } = req.body;

    if (!session_id || !message) {
      return res.status(400).json({ error: 'session_id and message are required' });
    }

    // Ensure session exists
    let session = db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(session_id) as any;
    if (!session) {
      // Resolve expert: if provided in request AND session doesn't exist, store it
      const expertInfo = expert ? JSON.stringify({
        id: expert.id,
        name: expert.name,
        title: expert.title,
        description: expert.description || '',
        skills: expert.skills || []
      }) : null;
      db.prepare('INSERT INTO chat_sessions (id, title, expert_info) VALUES (?, ?, ?)').run(
        session_id,
        message.substring(0, 30) + '...',
        expertInfo
      );
      session = db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(session_id) as any;
    } else {
      // Session exists, if expert is provided, update the stored expert_info
      if (expert) {
        const expertInfo = JSON.stringify({
          id: expert.id,
          name: expert.name,
          title: expert.title,
          description: expert.description || '',
          skills: expert.skills || []
        });
        db.prepare('UPDATE chat_sessions SET expert_info = ? WHERE id = ?').run(expertInfo, session_id);
      }
    }

    // Resolve effective expert: use request expert, fallback to session stored expert
    let effectiveExpert = expert;
    if (!effectiveExpert && session && session.expert_info) {
      try {
        effectiveExpert = typeof session.expert_info === 'string' ? JSON.parse(session.expert_info) : session.expert_info;
      } catch {
        effectiveExpert = null;
      }
    }

    // Save user message
    const userMsgId = 'msg-' + Date.now();
    db.prepare(
      'INSERT INTO chat_messages (id, session_id, role, content) VALUES (?, ?, ?, ?)'
    ).run(userMsgId, session_id, 'user', message);

    // Get conversation history
    const history = db.prepare(
      'SELECT role, content FROM chat_messages WHERE session_id = ? ORDER BY created_at ASC'
    ).all(session_id) as { role: string; content: string }[];

    // Generate AI response
    const aiContent = await generateResponse(message, history.slice(0, -1), effectiveExpert); // exclude current user msg

    // Save AI message
    const aiMsgId = 'msg-' + (Date.now() + 1);
    db.prepare(
      'INSERT INTO chat_messages (id, session_id, role, content) VALUES (?, ?, ?, ?)'
    ).run(aiMsgId, session_id, 'assistant', aiContent);

    // Update session timestamp
    db.prepare('UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(session_id);

    // Update session title if first message
    const msgCount = db.prepare(
      'SELECT COUNT(*) as count FROM chat_messages WHERE session_id = ?'
    ).get(session_id) as { count: number };
    if (msgCount.count <= 2) {
      db.prepare('UPDATE chat_sessions SET title = ? WHERE id = ?').run(
        message.substring(0, 30) + '...',
        session_id
      );
    }

    res.status(201).json({
      user_message: { id: userMsgId, role: 'user', content: message },
      ai_message: { id: aiMsgId, role: 'assistant', content: aiContent }
    });
  } catch (error) {
    next(error);
  }
};

// Send message with streaming (SSE) AI response
export const sendMessageStream = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const db = getDB();
    const { session_id, message, expert } = req.body;

    if (!session_id || !message) {
      return res.status(400).json({ error: 'session_id and message are required' });
    }

    // Ensure session exists
    let session = db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(session_id) as any;
    if (!session) {
      // Store expert info if provided
      const expertInfo = expert ? JSON.stringify({
        id: expert.id,
        name: expert.name,
        title: expert.title,
        description: expert.description || '',
        skills: expert.skills || []
      }) : null;
      db.prepare('INSERT INTO chat_sessions (id, title, expert_info) VALUES (?, ?, ?)').run(
        session_id,
        message.substring(0, 30) + '...',
        expertInfo
      );
      session = db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(session_id) as any;
    } else {
      // Session exists, if expert is provided, update the stored expert_info
      if (expert) {
        const expertInfo = JSON.stringify({
          id: expert.id,
          name: expert.name,
          title: expert.title,
          description: expert.description || '',
          skills: expert.skills || []
        });
        db.prepare('UPDATE chat_sessions SET expert_info = ? WHERE id = ?').run(expertInfo, session_id);
      }
    }

    // Resolve effective expert: use request expert, fallback to session stored expert
    let effectiveExpert = expert;
    if (!effectiveExpert && session && session.expert_info) {
      try {
        effectiveExpert = typeof session.expert_info === 'string' ? JSON.parse(session.expert_info) : session.expert_info;
      } catch {
        effectiveExpert = null;
      }
    }

    // Save user message
    const userMsgId = 'msg-' + Date.now();
    db.prepare(
      'INSERT INTO chat_messages (id, session_id, role, content) VALUES (?, ?, ?, ?)'
    ).run(userMsgId, session_id, 'user', message);

    // Get conversation history
    const history = db.prepare(
      'SELECT role, content FROM chat_messages WHERE session_id = ? ORDER BY created_at ASC'
    ).all(session_id) as { role: string; content: string }[];

    // Set up SSE headers
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.setHeader('X-Accel-Buffering', 'no');
    res.flushHeaders();

    // Send user message event first
    res.write(`data: ${JSON.stringify({ type: 'user_message', id: userMsgId, content: message })}\n\n`);

    // Stream AI response chunks
    let fullContent = '';
    const aiMsgId = 'msg-' + (Date.now() + 1);

    try {
      for await (const chunk of generateResponseStream(message, history.slice(0, -1), effectiveExpert)) {
        fullContent += chunk;
        res.write(`data: ${JSON.stringify({ type: 'chunk', content: chunk })}\n\n`);
      }
    } catch (streamError) {
      console.error('Stream error, falling back to non-streaming:', streamError);
      const fallbackContent = await generateResponse(message, history.slice(0, -1), effectiveExpert);
      fullContent = fallbackContent;
      res.write(`data: ${JSON.stringify({ type: 'chunk', content: fallbackContent })}\n\n`);
    }

    // Save AI message to DB
    db.prepare(
      'INSERT INTO chat_messages (id, session_id, role, content) VALUES (?, ?, ?, ?)'
    ).run(aiMsgId, session_id, 'assistant', fullContent);

    // Update session timestamp
    db.prepare('UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(session_id);

    // Update session title if first message
    const msgCount = db.prepare(
      'SELECT COUNT(*) as count FROM chat_messages WHERE session_id = ?'
    ).get(session_id) as { count: number };
    if (msgCount.count <= 2) {
      db.prepare('UPDATE chat_sessions SET title = ? WHERE id = ?').run(
        message.substring(0, 30) + '...',
        session_id
      );
    }

    // Send done event
    res.write(`data: ${JSON.stringify({ type: 'done', ai_msg_id: aiMsgId })}\n\n`);
    res.end();
  } catch (error) {
    // If headers already sent, try to end gracefully
    if (res.headersSent) {
      res.write(`data: ${JSON.stringify({ type: 'error', message: (error as Error).message })}\n\n`);
      return res.end();
    }
    next(error);
  }
};

export default {
  getSessions,
  getMessages,
  createSession,
  deleteSession,
  sendMessage,
  sendMessageStream
};