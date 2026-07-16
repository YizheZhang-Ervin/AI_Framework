import { Request, Response } from 'express';
import { getDB } from '../db/init.js';
import crypto from 'crypto';

function generateId(): string {
  return crypto.randomUUID();
}

// === Experts CRUD ===

export async function listExperts(_req: Request, res: Response) {
  try {
    const db = getDB();
    const experts = db.prepare('SELECT * FROM experts ORDER BY created_at DESC').all() as any[];
    // Attach skills for each expert
    const stmt = db.prepare('SELECT * FROM skills WHERE expert_id = ? ORDER BY created_at ASC');
    const result = experts.map(expert => ({
      ...expert,
      skills: stmt.all(expert.id)
    }));
    res.json(result);
  } catch (error) {
    res.status(500).json({ error: '获取专家列表失败', details: (error as Error).message });
  }
}

export async function getExpert(req: Request, res: Response) {
  try {
    const db = getDB();
    const expert = db.prepare('SELECT * FROM experts WHERE id = ?').get(req.params.id) as any;
    if (!expert) {
      res.status(404).json({ error: '专家不存在' });
      return;
    }
    expert.skills = db.prepare('SELECT * FROM skills WHERE expert_id = ? ORDER BY created_at ASC').all(expert.id);
    res.json(expert);
  } catch (error) {
    res.status(500).json({ error: '获取专家详情失败', details: (error as Error).message });
  }
}

export async function createExpert(req: Request, res: Response) {
  try {
    const { name, title, description, avatar, skills: initialSkills } = req.body;
    if (!name || !title) {
      res.status(400).json({ error: '名称(name)和头衔(title)为必填项' });
      return;
    }

    const db = getDB();
    const id = generateId();
    const now = new Date().toISOString().replace('T', ' ').slice(0, 19);

    db.prepare(`
      INSERT INTO experts (id, name, title, description, avatar, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(id, name, title, description || '', avatar || '', now, now);

    // Create initial skills if provided
    if (initialSkills && Array.isArray(initialSkills) && initialSkills.length > 0) {
      const skillStmt = db.prepare(`
        INSERT INTO skills (id, expert_id, name, description, created_at)
        VALUES (?, ?, ?, ?, ?)
      `);
      for (const skill of initialSkills) {
        skillStmt.run(generateId(), id, skill.name, skill.description || '', now);
      }
    }

    const expert = db.prepare('SELECT * FROM experts WHERE id = ?').get(id) as any;
    expert.skills = db.prepare('SELECT * FROM skills WHERE expert_id = ? ORDER BY created_at ASC').all(id);
    res.status(201).json(expert);
  } catch (error) {
    res.status(500).json({ error: '创建专家失败', details: (error as Error).message });
  }
}

export async function updateExpert(req: Request, res: Response) {
  try {
    const { name, title, description, avatar } = req.body;
    const db = getDB();
    const existing = db.prepare('SELECT * FROM experts WHERE id = ?').get(req.params.id);
    if (!existing) {
      res.status(404).json({ error: '专家不存在' });
      return;
    }

    const now = new Date().toISOString().replace('T', ' ').slice(0, 19);
    db.prepare(`
      UPDATE experts SET name = ?, title = ?, description = ?, avatar = ?, updated_at = ?
      WHERE id = ?
    `).run(
      name || (existing as any).name,
      title || (existing as any).title,
      description !== undefined ? description : (existing as any).description,
      avatar !== undefined ? avatar : (existing as any).avatar,
      now,
      req.params.id
    );

    const expert = db.prepare('SELECT * FROM experts WHERE id = ?').get(req.params.id) as any;
    expert.skills = db.prepare('SELECT * FROM skills WHERE expert_id = ? ORDER BY created_at ASC').all(expert.id);
    res.json(expert);
  } catch (error) {
    res.status(500).json({ error: '更新专家失败', details: (error as Error).message });
  }
}

export async function deleteExpert(req: Request, res: Response) {
  try {
    const db = getDB();
    const existing = db.prepare('SELECT * FROM experts WHERE id = ?').get(req.params.id);
    if (!existing) {
      res.status(404).json({ error: '专家不存在' });
      return;
    }

    // Cascade delete will handle skills
    db.prepare('DELETE FROM experts WHERE id = ?').run(req.params.id);
    res.json({ success: true, message: '已删除专家及其所有技能' });
  } catch (error) {
    res.status(500).json({ error: '删除专家失败', details: (error as Error).message });
  }
}

// === Skills CRUD ===

export async function addSkill(req: Request, res: Response) {
  try {
    const { name, description } = req.body;
    const { expertId } = req.params;

    if (!name) {
      res.status(400).json({ error: '技能名称(name)为必填项' });
      return;
    }

    const db = getDB();
    const expert = db.prepare('SELECT * FROM experts WHERE id = ?').get(expertId);
    if (!expert) {
      res.status(404).json({ error: '专家不存在' });
      return;
    }

    const id = generateId();
    const now = new Date().toISOString().replace('T', ' ').slice(0, 19);
    db.prepare(`
      INSERT INTO skills (id, expert_id, name, description, created_at)
      VALUES (?, ?, ?, ?, ?)
    `).run(id, expertId, name, description || '', now);

    const skill = db.prepare('SELECT * FROM skills WHERE id = ?').get(id);
    res.status(201).json(skill);
  } catch (error) {
    res.status(500).json({ error: '添加技能失败', details: (error as Error).message });
  }
}

export async function updateSkill(req: Request, res: Response) {
  try {
    const { name, description } = req.body;
    const db = getDB();
    const existing = db.prepare('SELECT * FROM skills WHERE id = ? AND expert_id = ?').get(req.params.skillId, req.params.expertId) as any;
    if (!existing) {
      res.status(404).json({ error: '技能不存在' });
      return;
    }

    db.prepare(`
      UPDATE skills SET name = ?, description = ? WHERE id = ? AND expert_id = ?
    `).run(
      name || existing.name,
      description !== undefined ? description : existing.description,
      req.params.skillId,
      req.params.expertId
    );

    const skill = db.prepare('SELECT * FROM skills WHERE id = ?').get(req.params.skillId);
    res.json(skill);
  } catch (error) {
    res.status(500).json({ error: '更新技能失败', details: (error as Error).message });
  }
}

export async function deleteSkill(req: Request, res: Response) {
  try {
    const db = getDB();
    const existing = db.prepare('SELECT * FROM skills WHERE id = ? AND expert_id = ?').get(req.params.skillId, req.params.expertId);
    if (!existing) {
      res.status(404).json({ error: '技能不存在' });
      return;
    }

    db.prepare('DELETE FROM skills WHERE id = ? AND expert_id = ?').run(req.params.skillId, req.params.expertId);
    res.json({ success: true, message: '已删除技能' });
  } catch (error) {
    res.status(500).json({ error: '删除技能失败', details: (error as Error).message });
  }
}