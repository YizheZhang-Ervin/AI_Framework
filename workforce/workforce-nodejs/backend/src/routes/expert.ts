import { Router } from 'express';
import {
  listExperts,
  getExpert,
  createExpert,
  updateExpert,
  deleteExpert,
  addSkill,
  updateSkill,
  deleteSkill
} from '../controllers/expertController.js';

const router = Router();

// Expert CRUD
router.get('/', listExperts);
router.get('/:id', getExpert);
router.post('/', createExpert);
router.put('/:id', updateExpert);
router.delete('/:id', deleteExpert);

// Skill CRUD (nested under expert)
router.post('/:expertId/skills', addSkill);
router.put('/:expertId/skills/:skillId', updateSkill);
router.delete('/:expertId/skills/:skillId', deleteSkill);

export default router;