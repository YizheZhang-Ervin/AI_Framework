<template>
  <div class="chat-container">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h1 class="logo">AI 助手</h1>
        <button class="new-chat-btn" @click="createNewSession">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          新对话
        </button>
      </div>
      <!-- Navigation -->
      <div class="sidebar-nav">
        <a class="nav-item" :class="{ 'nav-active': !showExpertModal }" @click="showExpertModal = false">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <span>对话</span>
        </a>
        <a class="nav-item" :class="{ 'nav-active': showExpertModal }" @click="showExpertModal = true">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
          <span>专家库</span>
        </a>
      </div>
      <!-- Selected Expert Indicator -->
      <div v-if="selectedExpert" class="sidebar-expert">
        <div class="expert-indicator">
          <div class="expert-mini-avatar">{{ selectedExpert.name.charAt(0).toUpperCase() }}</div>
          <div class="expert-mini-info">
            <span class="expert-mini-name">{{ selectedExpert.name }}</span>
            <span class="expert-mini-title">{{ selectedExpert.title }}</span>
          </div>
          <button class="expert-clear-btn" @click="clearExpert" title="切换为普通AI对话">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="expert-switch-hint">
          <span>正在与专家对话</span>
          <button class="switch-to-ai-btn" @click="clearExpert">切换为普通AI</button>
        </div>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === currentSessionId }"
          @click="switchSession(session.id)"
        >
          <div class="session-info">
            <span class="session-title">
              <template v-if="session.expert_info">
                <span class="session-expert-badge">{{ session.expert_info.name.charAt(0).toUpperCase() }}</span>
              </template>
              {{ session.title }}
            </span>
            <span class="session-date">{{ formatDate(session.updated_at) }}</span>
          </div>
          <button
            class="delete-btn"
            @click.stop="deleteSession(session.id)"
            title="删除对话"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>

      </div>
    </aside>
    <button class="toggle-sidebar" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开侧边栏' : '折叠侧边栏'">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline :points="sidebarCollapsed ? '9 18 15 12 9 6' : '15 18 9 12 15 6'"></polyline>
      </svg>
    </button>

    <!-- Main Chat Area -->
    <main class="main-content">
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0 && !selectedExpert" class="welcome">
          <div class="welcome-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#667eea" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
              <path d="M2 17l10 5 10-5"></path>
              <path d="M2 12l10 5 10-5"></path>
            </svg>
          </div>
          <h2>你好！我是AI助手</h2>
          <p>我可以帮你解答问题、分析数据、编写代码等</p>
          <div class="suggestions">
            <button class="suggestion-btn" @click="sendSuggested('帮我分析一下人工智能的发展趋势')">
              分析AI发展趋势
            </button>
            <button class="suggestion-btn" @click="sendSuggested('用JavaScript写一个排序算法')">
              写一个排序算法
            </button>
            <button class="suggestion-btn" @click="sendSuggested('解释一下什么是RESTful API')">
              解释RESTful API
            </button>
          </div>
        </div>
        <div v-else-if="messages.length === 0 && selectedExpert" class="welcome expert-welcome">
          <div class="expert-avatar-circle">{{ selectedExpert.name.charAt(0).toUpperCase() }}</div>
          <h2>{{ selectedExpert.name }}</h2>
          <span class="expert-title-tag">{{ selectedExpert.title }}</span>
          <p>{{ selectedExpert.description || '暂无描述' }}</p>
          <div v-if="selectedExpert.skills && selectedExpert.skills.length > 0" class="expert-skills">
            <span v-for="skill in selectedExpert.skills" :key="skill.id" class="expert-skill-tag">{{ skill.name }}</span>
          </div>
          <p class="expert-hint">开始与专家对话...</p>
        </div>

        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message"
          :class="msg.role"
        >
          <div class="avatar">
            {{ msg.role === 'user' ? 'U' : (selectedExpert ? selectedExpert.name.charAt(0).toUpperCase() : 'AI') }}
          </div>
          <div class="bubble">
            <template v-if="msg.role === 'assistant' && msg.content.includes('[思考过程]')">
              <div class="reasoning-section">
                <button class="reasoning-toggle" @click="toggleReasoning(msg.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline :points="expandedReasoning.has(msg.id) ? '18 15 12 9 6 15' : '6 9 12 15 18 9'"></polyline>
                  </svg>
                  {{ expandedReasoning.has(msg.id) ? '隐藏思考过程' : '查看思考过程' }}
                </button>
                <div v-if="expandedReasoning.has(msg.id)" class="reasoning-content" v-html="renderContent(extractReasoning(msg.content))"></div>
              </div>
              <div v-html="renderContent(extractAnswer(msg.content))"></div>
            </template>
            <template v-else-if="msg.role === 'assistant' && !msg.content">
              <div class="streaming-indicator">
                <span class="streaming-dot"></span>
                <span class="streaming-dot"></span>
                <span class="streaming-dot"></span>
              </div>
            </template>
            <template v-else>
              <div v-html="renderContent(msg.content)"></div>
            </template>
          </div>
        </div>

        <!-- Loading indicator is handled by the streaming placeholder message above -->
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <textarea
            ref="inputRef"
            v-model="inputMessage"
            @keydown="handleKeydown"
            placeholder="输入你的问题..."
            rows="1"
            :disabled="loading"
          ></textarea>
          <button
            class="send-btn"
            :disabled="!inputMessage.trim() || loading"
            @click="sendMessage"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </div>
        <p class="hint">Ctrl+Enter 发送，Enter 换行</p>
      </div>
    </main>
  </div>
  <ExpertManagement :visible="showExpertModal" @close="showExpertModal = false" @select="onSelectExpert" />
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { marked } from 'marked'
import ExpertManagement from './ExpertManagement.vue'

const API_BASE = '/api/chat'
const STREAM_API = 'http://localhost:3001/api/chat'

const showExpertModal = ref(false)
const selectedExpert = ref(null)
const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const sidebarCollapsed = ref(false)
const messagesRef = ref(null)
const inputRef = ref(null)
const expandedReasoning = ref(new Set())

function onSelectExpert(expert) {
  selectedExpert.value = expert
  showExpertModal.value = false
  // Create a new session for the expert conversation (force to bypass empty session guard)
  createNewSession(true)
}

// Clear expert selection, switch back to normal AI
function clearExpert() {
  selectedExpert.value = null
  // Create a new session for normal AI conversation (force to bypass empty session guard)
  createNewSession(true)
}

// Toggle reasoning visibility
function toggleReasoning(msgId) {
  if (expandedReasoning.value.has(msgId)) {
    expandedReasoning.value.delete(msgId)
  } else {
    expandedReasoning.value.add(msgId)
  }
  // Trigger reactivity by replacing the Set
  expandedReasoning.value = new Set(expandedReasoning.value)
}

// Extract [思考过程] section
function extractReasoning(content) {
  const match = content.match(/\[思考过程\]([\s\S]*?)\[\/思考过程\]/)
  return match ? match[1].trim() : ''
}

// Extract answer after [思考过程]
function extractAnswer(content) {
  return content.replace(/\[思考过程\][\s\S]*?\[\/思考过程\]\s*/g, '').trim()
}

// Fetch sessions
async function fetchSessions() {
  try {
    const res = await fetch(`${API_BASE}/sessions`)
    sessions.value = await res.json()
  } catch (e) {
    console.error('Failed to fetch sessions:', e)
  }
}

// Fetch messages for a session
async function fetchMessages(sessionId) {
  try {
    const res = await fetch(`${API_BASE}/sessions/${sessionId}/messages`)
    messages.value = await res.json()
  } catch (e) {
    console.error('Failed to fetch messages:', e)
  }
}

// Create a new session — only allowed when current session has been used (has messages)
async function createNewSession(force = false) {
  // If the current session has no messages yet, don't create a new one (unless force is true)
  if (!force && messages.value.length === 0 && currentSessionId.value) {
    return
  }
  try {
    const res = await fetch(`${API_BASE}/sessions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: '新对话',
        expert: selectedExpert.value ? {
          id: selectedExpert.value.id,
          name: selectedExpert.value.name,
          title: selectedExpert.value.title,
          description: selectedExpert.value.description,
          skills: selectedExpert.value.skills || []
        } : null
      })
    })
    const session = await res.json()
    sessions.value.unshift(session)
    await switchSession(session.id)
  } catch (e) {
    console.error('Failed to create session:', e)
  }
}

// Switch to a session
async function switchSession(sessionId) {
  currentSessionId.value = sessionId
  // Find the session in the list to check if it has expert_info
  const session = sessions.value.find(s => s.id === sessionId)
  if (session && session.expert_info) {
    // Restore expert from session's stored expert_info
    selectedExpert.value = session.expert_info
  } else if (!session || !session.expert_info) {
    // If the session has no expert_info and we haven't explicitly selected an expert, clear it
    // But only if the session exists and has no expert (don't clear during initial load)
    selectedExpert.value = null
  }
  await fetchMessages(sessionId)
  await nextTick()
  scrollToBottom()
}

// Delete a session
async function deleteSession(sessionId) {
  try {
    await fetch(`${API_BASE}/sessions/${sessionId}`, { method: 'DELETE' })
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      if (sessions.value.length > 0) {
        await switchSession(sessions.value[0].id)
      } else {
        currentSessionId.value = null
        messages.value = []
      }
    }
  } catch (e) {
    console.error('Failed to delete session:', e)
  }
}

// Send a message via SSE streaming
async function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text || loading.value) return

  const sessionId = currentSessionId.value || 'session-' + Date.now()
  if (!currentSessionId.value) {
    currentSessionId.value = sessionId
  }

  inputMessage.value = ''
  loading.value = true
  resetTextareaHeight()

  // Optimistically add user message
  const tempUserMsg = { id: 'temp-' + Date.now(), role: 'user', content: text }
  messages.value.push(tempUserMsg)

  // Create placeholder for AI message that will be filled via stream
  const aiMsgId = 'ai-' + Date.now()
  const aiPlaceholder = { id: aiMsgId, role: 'assistant', content: '' }
  messages.value.push(aiPlaceholder)
  scrollToBottom()

  let fullContent = ''

  try {
    // Use direct backend URL for SSE to bypass Vite proxy buffering
    const res = await fetch(`${STREAM_API}/sessions/${sessionId}/messages/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        session_id: sessionId,
        message: text,
        expert: selectedExpert.value ? {
          id: selectedExpert.value.id,
          name: selectedExpert.value.name,
          title: selectedExpert.value.title,
          description: selectedExpert.value.description,
          skills: selectedExpert.value.skills || []
        } : null
      })
    })

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`)
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || !trimmed.startsWith('data: ')) continue

        try {
          const event = JSON.parse(trimmed.slice(6))

          if (event.type === 'user_message') {
            // Replace temp message with real user message
            const idx = messages.value.indexOf(tempUserMsg)
            if (idx !== -1) {
              messages.value[idx] = { id: event.id, role: 'user', content: event.content }
            }
          } else if (event.type === 'chunk') {
            fullContent += event.content
            // Update the AI placeholder content reactively
            const msgIdx = messages.value.findIndex(m => m.id === aiMsgId)
            if (msgIdx !== -1) {
              messages.value[msgIdx] = { ...messages.value[msgIdx], content: fullContent }
            }
            scrollToBottom()
          } else if (event.type === 'done') {
            // Update with final message ID
            const msgIdx = messages.value.findIndex(m => m.id === aiMsgId)
            if (msgIdx !== -1) {
              messages.value[msgIdx] = { ...messages.value[msgIdx], id: event.ai_msg_id }
            }
          } else if (event.type === 'error') {
            console.error('SSE error:', event.message)
          }
        } catch (e) {
          // Skip malformed JSON
        }
      }
    }

    await fetchSessions()
  } catch (e) {
    console.error('Failed to send message:', e)
    // Remove temp messages on failure
    messages.value = messages.value.filter(m => m.id !== tempUserMsg.id && m.id !== aiMsgId)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// Send a suggested question
async function sendSuggested(text) {
  inputMessage.value = text
  await sendMessage()
}

// Handle keyboard events — Ctrl+Enter to send, Enter for new line
function handleKeydown(e) {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    sendMessage()
  }
}

// Auto-resize textarea
function autoResizeTextarea() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

function resetTextareaHeight() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
}

watch(inputMessage, () => {
  nextTick(autoResizeTextarea)
})

// Render markdown content synchronously
function renderContent(content) {
  if (!content) return ''
  // marked v12+ parse() returns a Promise; use parseSync for v-html
  if (typeof marked.parseSync === 'function') {
    return marked.parseSync(content, { breaks: true })
  }
  return marked.parse(content, { breaks: true })
}

// Scroll to bottom
function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

// Format date
function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

onMounted(async () => {
  await fetchSessions()
  if (sessions.value.length > 0) {
    await switchSession(sessions.value[0].id)
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f0f2f5;
  height: 100vh;
}

.chat-container {
  display: flex;
  height: 100vh;
  background: #f0f2f5;
}

/* Sidebar */
.sidebar {
  width: 280px;
  background: linear-gradient(180deg, #1a1a2e, #16213e);
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 0;
  overflow: hidden;
}

.sidebar-nav {
  padding: 8px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: rgba(255,255,255,0.7);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
  margin: 2px 0;
}
.nav-item:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}
.nav-item.nav-active {
  background: rgba(102, 126, 234, 0.3);
  color: #fff;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logo {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 16px;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: rgba(255,255,255,0.2);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  margin: 4px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.session-item:hover {
  background: rgba(255,255,255,0.1);
}

.session-item.active {
  background: rgba(102, 126, 234, 0.3);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-expert-badge {
  display: inline-flex;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.session-date {
  display: block;
  font-size: 12px;
  color: rgba(255,255,255,0.5);
  margin-top: 4px;
}

.delete-btn {
  opacity: 0;
  background: none;
  border: none;
  color: rgba(255,255,255,0.5);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #ff4d4f;
  background: rgba(255,77,79,0.1);
}

.toggle-sidebar {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  left: 280px;
  width: 28px;
  height: 48px;
  background: #1a1a2e;
  border: none;
  border-radius: 0 8px 8px 0;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  transition: left 0.3s, background 0.2s;
  box-shadow: 2px 0 6px rgba(0,0,0,0.15);
}

.sidebar.collapsed ~ .toggle-sidebar {
  left: 0;
}

.toggle-sidebar:hover {
  background: #2a2a4e;
}

/* Main Content */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 40px 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Welcome */
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 12px;
  color: #666;
}

.welcome h2 {
  font-size: 24px;
  color: #1a1a2e;
  margin-top: 16px;
}

.welcome p {
  font-size: 16px;
  color: #999;
}

.suggestions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.suggestion-btn {
  padding: 10px 20px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: #fff;
  color: #667eea;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.suggestion-btn:hover {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

/* Message */
.message {
  display: flex;
  gap: 16px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.message.user .avatar {
  background: #667eea;
  color: #fff;
}

.message.assistant .avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}

.bubble {
  max-width: 80%;
  padding: 14px 20px;
  border-radius: 14px;
  line-height: 1.7;
  font-size: 15px;
}

.message.user .bubble {
  background: #667eea;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message.assistant .bubble {
  background: #fff;
  color: #333;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  border-bottom-left-radius: 4px;
}

.bubble :deep(pre) {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
}

.bubble :deep(code) {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  font-size: 13px;
}

.bubble :deep(p) {
  margin: 8px 0;
}

.bubble :deep(p:first-child) {
  margin-top: 0;
}

.bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.bubble :deep(ul), .bubble :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.bubble :deep(li) {
  margin: 4px 0;
}

.bubble :deep(h1), .bubble :deep(h2), .bubble :deep(h3) {
  margin: 16px 0 8px;
}

.bubble :deep(h1) { font-size: 20px; }
.bubble :deep(h2) { font-size: 18px; }
.bubble :deep(h3) { font-size: 16px; }

.bubble :deep(blockquote) {
  border-left: 3px solid #667eea;
  padding-left: 12px;
  color: #666;
  margin: 8px 0;
}

.bubble :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.bubble :deep(th), .bubble :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 8px 12px;
  text-align: left;
}

.bubble :deep(th) {
  background: #f5f5f5;
  font-weight: 600;
}

.message.user .bubble :deep(code) {
  background: rgba(255,255,255,0.2);
  padding: 2px 6px;
  border-radius: 4px;
}

/* Reasoning Section */
.reasoning-section {
  margin-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
  padding-bottom: 8px;
}

.reasoning-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #667eea;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.reasoning-toggle:hover {
  background: rgba(102, 126, 234, 0.08);
}

.reasoning-content {
  margin-top: 8px;
  padding: 10px 12px;
  background: #f8f9ff;
  border-radius: 8px;
  border-left: 3px solid #667eea;
  font-size: 13px;
  color: #555;
  line-height: 1.6;
}

.reasoning-content :deep(p) {
  margin: 4px 0;
}

/* Streaming Indicator (used inside the placeholder bubble) */
.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
  min-height: 20px;
}

.streaming-dot {
  width: 6px;
  height: 6px;
  background: #667eea;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.streaming-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.streaming-dot:nth-child(3) {
  animation-delay: 0.4s;
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 16px 20px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  30% {
    opacity: 1;
    transform: scale(1);
  }
}

/* Input Area */
.input-area {
  padding: 20px 80px 28px;
  background: transparent;
}

.input-wrapper {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: #fff;
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.input-wrapper textarea {
  flex: 1;
  border: none;
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
  resize: none;
  max-height: 200px;
  line-height: 1.5;
  font-family: inherit;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: none;
  background: #667eea;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #5a6fd6;
  transform: scale(1.05);
}

.send-btn:disabled {
  background: #d0d0d0;
  cursor: not-allowed;
}

.hint {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}

/* Expert Welcome */
.expert-welcome {
  gap: 8px;
}
.expert-avatar-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  flex-shrink: 0;
  margin-bottom: 8px;
}
.expert-title-tag {
  display: inline-block;
  font-size: 14px;
  color: #667eea;
  background: #f0f0ff;
  padding: 4px 14px;
  border-radius: 12px;
}
.expert-skills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 8px;
}
.expert-skill-tag {
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 4px 12px;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}
.expert-hint {
  margin-top: 16px;
  color: #aaa;
  font-size: 14px;
  animation: pulse 2s infinite;
}

/* Sidebar Expert Indicator */
.sidebar-expert {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  background: rgba(102, 126, 234, 0.1);
}
.expert-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
}
.expert-mini-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}
.expert-mini-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.expert-mini-name {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.expert-mini-title {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.expert-clear-btn {
  background: none;
  border: none;
  color: rgba(255,255,255,0.4);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}
.expert-clear-btn:hover {
  color: #ff4d4f;
  background: rgba(255,77,79,0.15);
}
.expert-switch-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 11px;
  color: rgba(255,255,255,0.5);
}
.switch-to-ai-btn {
  background: none;
  border: 1px solid rgba(255,255,255,0.2);
  color: rgba(255,255,255,0.6);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
}
.switch-to-ai-btn:hover {
  background: rgba(102, 126, 234, 0.3);
  border-color: #667eea;
  color: #fff;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}
</style>