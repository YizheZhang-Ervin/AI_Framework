<template>
  <div v-show="visible" class="expert-modal-overlay" @click.self="$emit('close')">
    <div class="expert-modal">
      <!-- Header -->
      <div class="modal-header">
        <div class="header-left">
          <button class="btn-back" @click="$emit('close')" title="返回">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>
          <h1>专家库管理</h1>
        </div>
        <button class="btn btn-primary" @click="openCreateExpert">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          新增专家
        </button>
      </div>

      <!-- Expert Cards Grid -->
      <div class="modal-body">
        <div v-if="experts.length === 0" class="empty-state">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#667eea" stroke-width="1.5">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
          <h2>暂无专家</h2>
          <p>点击"新增专家"按钮创建一个专家智能体</p>
        </div>

        <div class="expert-grid">
          <div v-for="expert in experts" :key="expert.id" class="expert-card">
            <div class="card-header">
              <div class="avatar-circle">{{ expert.name.charAt(0).toUpperCase() }}</div>
              <div class="card-info">
                <h3>{{ expert.name }}</h3>
                <span class="title-tag">{{ expert.title }}</span>
              </div>
              <div class="card-actions">
                <button class="btn-icon" title="编辑" @click="openEditExpert(expert)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                </button>
                <button class="btn-icon danger" title="删除" @click="deleteExpert(expert)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </button>
              </div>
            </div>
            <p class="description">{{ expert.description || '暂无描述' }}</p>
            <div class="skills-section">
              <div class="skills-header">
                <span class="skills-label">技能 ({{ expert.skills.length }})</span>
                <button class="btn-sm" @click="openAddSkill(expert)">+ 添加技能</button>
              </div>
              <div v-if="expert.skills.length === 0" class="no-skills">暂无技能</div>
              <div v-for="skill in expert.skills" :key="skill.id" class="skill-item">
                <div class="skill-info">
                  <span class="skill-name">{{ skill.name }}</span>
                  <span v-if="skill.description" class="skill-desc"> - {{ skill.description }}</span>
                </div>
                <div class="skill-actions">
                  <button class="btn-icon" title="编辑技能" @click="openEditSkill(expert, skill)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>
                  <button class="btn-icon danger" title="删除技能" @click="deleteSkill(expert, skill)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <button class="btn btn-primary btn-select" @click="selectExpert(expert)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                选择此专家对话
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Expert Dialog -->
      <div v-if="showExpertDialog" class="sub-modal-overlay" @click.self="closeExpertDialog">
        <div class="sub-modal">
          <div class="sub-modal-header">
            <h2>{{ editingExpert ? '编辑专家' : '新增专家' }}</h2>
            <button class="btn-close" @click="closeExpertDialog">&times;</button>
          </div>
          <div class="sub-modal-body">
            <div class="form-group">
              <label>名称 *</label>
              <input v-model="expertForm.name" placeholder="例如：资深前端工程师" />
            </div>
            <div class="form-group">
              <label>头衔 *</label>
              <input v-model="expertForm.title" placeholder="例如：前端架构师" />
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea v-model="expertForm.description" placeholder="专家描述..."></textarea>
            </div>
          </div>
          <div class="sub-modal-footer">
            <button class="btn btn-secondary" @click="closeExpertDialog">取消</button>
            <button class="btn btn-primary" @click="saveExpert" :disabled="!expertForm.name || !expertForm.title">
              {{ editingExpert ? '保存' : '创建' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Skill Dialog -->
      <div v-if="showSkillDialog" class="sub-modal-overlay" @click.self="closeSkillDialog">
        <div class="sub-modal">
          <div class="sub-modal-header">
            <h2>{{ editingSkill ? '编辑技能' : '添加技能' }}</h2>
            <button class="btn-close" @click="closeSkillDialog">&times;</button>
          </div>
          <div class="sub-modal-body">
            <div class="form-group">
              <label>技能名称 *</label>
              <input v-model="skillForm.name" placeholder="例如：React" />
            </div>
            <div class="form-group">
              <label>描述</label>
              <input v-model="skillForm.description" placeholder="技能描述..." />
            </div>
          </div>
          <div class="sub-modal-footer">
            <button class="btn btn-secondary" @click="closeSkillDialog">取消</button>
            <button class="btn btn-primary" @click="saveSkill" :disabled="!skillForm.name">
              {{ editingSkill ? '保存' : '添加' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'select'])

const API_BASE = '/api/experts'
const experts = ref([])

const showExpertDialog = ref(false)
const editingExpert = ref(null)
const expertForm = ref({ name: '', title: '', description: '' })

const showSkillDialog = ref(false)
const editingSkill = ref(null)
const skillExpert = ref(null)
const skillForm = ref({ name: '', description: '' })

async function fetchExperts() {
  try {
    const res = await fetch(API_BASE)
    experts.value = await res.json()
  } catch (e) {
    console.error('Failed to fetch experts:', e)
  }
}

function selectExpert(expert) {
  emit('select', expert)
  emit('close')
}

function openCreateExpert() {
  editingExpert.value = null
  expertForm.value = { name: '', title: '', description: '' }
  showExpertDialog.value = true
}

function openEditExpert(expert) {
  editingExpert.value = expert
  expertForm.value = { name: expert.name, title: expert.title, description: expert.description }
  showExpertDialog.value = true
}

function closeExpertDialog() {
  showExpertDialog.value = false
  editingExpert.value = null
}

async function saveExpert() {
  try {
    if (editingExpert.value) {
      const res = await fetch(`${API_BASE}/${editingExpert.value.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(expertForm.value)
      })
      if (!res.ok) throw new Error('更新失败')
    } else {
      const res = await fetch(API_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(expertForm.value)
      })
      if (!res.ok) throw new Error('创建失败')
    }
    closeExpertDialog()
    await fetchExperts()
  } catch (e) {
    console.error('Save expert failed:', e)
    alert('保存专家失败: ' + e.message)
  }
}

async function deleteExpert(expert) {
  if (!confirm(`确定删除专家"${expert.name}"？该操作不可撤销。`)) return
  try {
    const res = await fetch(`${API_BASE}/${expert.id}`, { method: 'DELETE' })
    if (!res.ok) throw new Error('删除失败')
    await fetchExperts()
  } catch (e) {
    console.error('Delete expert failed:', e)
    alert('删除专家失败: ' + e.message)
  }
}

function openAddSkill(expert) {
  skillExpert.value = expert
  editingSkill.value = null
  skillForm.value = { name: '', description: '' }
  showSkillDialog.value = true
}

function openEditSkill(expert, skill) {
  skillExpert.value = expert
  editingSkill.value = skill
  skillForm.value = { name: skill.name, description: skill.description }
  showSkillDialog.value = true
}

function closeSkillDialog() {
  showSkillDialog.value = false
  editingSkill.value = null
  skillExpert.value = null
}

async function saveSkill() {
  try {
    const expertId = skillExpert.value.id
    if (editingSkill.value) {
      const res = await fetch(`${API_BASE}/${expertId}/skills/${editingSkill.value.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(skillForm.value)
      })
      if (!res.ok) throw new Error('更新技能失败')
    } else {
      const res = await fetch(`${API_BASE}/${expertId}/skills`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(skillForm.value)
      })
      if (!res.ok) throw new Error('添加技能失败')
    }
    closeSkillDialog()
    await fetchExperts()
  } catch (e) {
    console.error('Save skill failed:', e)
    alert('保存技能失败: ' + e.message)
  }
}

async function deleteSkill(expert, skill) {
  if (!confirm(`确定删除技能"${skill.name}"？`)) return
  try {
    const res = await fetch(`${API_BASE}/${expert.id}/skills/${skill.id}`, { method: 'DELETE' })
    if (!res.ok) throw new Error('删除技能失败')
    await fetchExperts()
  } catch (e) {
    console.error('Delete skill failed:', e)
    alert('删除技能失败: ' + e.message)
  }
}

watch(() => props.visible, (val) => {
  if (val) fetchExperts()
})
</script>

<style scoped>
.expert-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.expert-modal {
  background: #f0f2f5;
  border-radius: 16px;
  width: 90vw;
  max-width: 1100px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: white;
  border-radius: 16px 16px 0 0;
  border-bottom: 1px solid #eef0f5;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.btn-back {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #f0f0f5;
  border-radius: 8px;
  cursor: pointer;
  color: #555;
  transition: all 0.2s;
}
.btn-back:hover {
  background: #e0e0ea;
  color: #667eea;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* Sub-modal (nested) */
.sub-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.sub-modal {
  background: white;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 20px 40px rgba(0,0,0,0.15);
}

.sub-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 0;
}
.sub-modal-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.btn-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}
.btn-close:hover {
  background: #f0f0f5;
  color: #333;
}

.sub-modal-body {
  padding: 20px 24px;
}
.sub-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 24px 20px;
}

.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #555;
  margin-bottom: 6px;
}
.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-group input:focus,
.form-group textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}
.form-group textarea {
  min-height: 80px;
  resize: vertical;
}

/* Reuse global button styles */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}
.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.btn-secondary {
  background: #e8e8f0;
  color: #555;
}
.btn-secondary:hover {
  background: #ddd;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}
.empty-state svg {
  margin-bottom: 16px;
}
.empty-state h2 {
  font-size: 20px;
  color: #666;
  margin: 0 0 8px;
}
.empty-state p {
  color: #999;
}

.expert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
}

.expert-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  border: 1px solid #eef0f5;
  transition: box-shadow 0.2s;
}
.expert-card:hover {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.12);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
}
.card-info h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}
.title-tag {
  display: inline-block;
  font-size: 12px;
  color: #667eea;
  background: #f0f0ff;
  padding: 2px 10px;
  border-radius: 10px;
  margin-top: 4px;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.btn-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #888;
  transition: all 0.2s;
}
.btn-icon:hover {
  background: #f0f0f5;
  color: #667eea;
}
.btn-icon.danger:hover {
  background: #fff0f0;
  color: #e74c3c;
}

.description {
  font-size: 13px;
  color: #888;
  margin: 0 0 16px;
  line-height: 1.5;
}

.card-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eef0f5;
}

.btn-select {
  width: 100%;
  justify-content: center;
  padding: 10px;
  font-size: 14px;
}

.skills-section {
  border-top: 1px solid #eef0f5;
  padding-top: 12px;
}

.skills-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.skills-label {
  font-size: 13px;
  font-weight: 600;
  color: #555;
}

.btn-sm {
  font-size: 12px;
  padding: 4px 10px;
  border: 1px dashed #ccc;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  color: #888;
  transition: all 0.2s;
}
.btn-sm:hover {
  border-color: #667eea;
  color: #667eea;
  background: #f8f8ff;
}

.no-skills {
  font-size: 12px;
  color: #bbb;
  padding: 8px 0;
  text-align: center;
}

.skill-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f8;
}
.skill-item:last-child {
  border-bottom: none;
}

.skill-info {
  flex: 1;
  min-width: 0;
}
.skill-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}
.skill-desc {
  font-size: 12px;
  color: #aaa;
}

.skill-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}
.skill-actions .btn-icon {
  width: 28px;
  height: 28px;
}
</style>