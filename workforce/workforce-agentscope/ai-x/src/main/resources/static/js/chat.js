/**
 * AI-X 智能体聊天前端
 * 支持流式SSE输出、技能展示、会话管理、文件附件上传
 */

// ===== 状态管理 =====
const state = {
    sessionId: generateSessionId(),
    userId: 'user-' + Math.random().toString(36).substring(2, 8),
    isStreaming: false,
    currentMessageElement: null,
    currentMessageText: '',
    currentThinkingText: '',
    isThinking: false,
    abortController: null,
    attachedFiles: [] // 已选择的附件文件列表
};

// DOM引用
const dom = {
    messages: document.getElementById('chatMessages'),
    input: document.getElementById('messageInput'),
    sendBtn: document.getElementById('sendBtn'),
    charCount: document.getElementById('charCount'),
    clearBtn: document.getElementById('clearChatBtn'),
    skillsList: document.getElementById('skillsList'),
    statusIndicator: document.getElementById('statusIndicator'),
    statusText: document.getElementById('statusText'),
    fileInput: document.getElementById('fileUpload'),
    filePreview: document.getElementById('filePreview')
};

// ===== 工具函数 =====

/** 生成唯一会话ID */
function generateSessionId() {
    const stored = sessionStorage.getItem('ai-x-session-id');
    if (stored) return stored;
    const id = 'session-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8);
    sessionStorage.setItem('ai-x-session-id', id);
    return id;
}

/** 转义HTML防止XSS */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/** 安全地解析Markdown为HTML（简易版） */
function renderMarkdown(text) {
    if (!text) return '';
    
    let html = escapeHtml(text);
    
    // 代码块 (```xxx```)
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (match, lang, code) => {
        const langClass = lang ? ` class="language-${escapeHtml(lang)}"` : '';
        return `<pre><code${langClass}>${code.trim()}</code></pre>`;
    });
    
    // 行内代码 (`code`)
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    // 粗体 (**text**)
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    
    // 斜体 (*text*)
    html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    
    // 链接 [text](url)
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
    
    // 换行符 -> <br>
    html = html.replace(/\n/g, '<br>');
    
    return html;
}

/** 格式化时间为本地时间字符串 */
function formatTime() {
    return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

// ===== 状态更新 =====

/** 更新连接状态指示器 */
function setStatus(type, text) {
    dom.statusIndicator.className = 'status-indicator' + (type ? ' ' + type : '');
    dom.statusText.textContent = text || '就绪';
}

// ===== 技能管理 =====

/** 加载技能列表 */
async function loadSkills() {
    try {
        const response = await fetch('/api/skills');
        if (!response.ok) throw new Error('HTTP ' + response.status);
        
        const data = await response.json();
        renderSkills(data.skills || []);
    } catch (error) {
        console.error('加载技能失败:', error);
        dom.skillsList.innerHTML = '<div class="empty">加载失败</div>';
    }
}

/** 渲染技能列表 */
function renderSkills(skills) {
    if (!skills || skills.length === 0) {
        dom.skillsList.innerHTML = '<div class="empty">暂无可用技能</div>';
        return;
    }
    
    dom.skillsList.innerHTML = skills.map(skill => `
        <div class="skill-item" title="${escapeHtml(skill.description || '')}">
            <div class="skill-name">${escapeHtml(skill.name)}</div>
            <div class="skill-desc">${escapeHtml(truncateText(skill.description, 60))}</div>
            <div class="skill-files">${(skill.files || []).join(', ')}</div>
        </div>
    `).join('');
}

/** 截断文本 */
function truncateText(text, maxLen) {
    if (!text || text.length <= maxLen) return text || '';
    return text.substring(0, maxLen) + '...';
}

// ===== 文件附件管理 =====

/** 格式化文件大小 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

/** 获取文件图标（根据MIME类型） */
function getFileIcon(file) {
    const type = file.type;
    if (type.startsWith('image/')) return '🖼️';
    if (type.startsWith('text/')) return '📄';
    if (type.includes('pdf')) return '📕';
    if (type.includes('zip') || type.includes('rar') || type.includes('tar')) return '📦';
    if (type.includes('json') || type.includes('xml')) return '📋';
    if (type.includes('javascript') || type.includes('python') || type.includes('java')) return '💻';
    if (type.includes('csv') || type.includes('excel')) return '📊';
    return '📎';
}

/** 更新文件预览区域 */
function updateFilePreview() {
    const files = state.attachedFiles;
    const preview = dom.filePreview;
    
    if (!files || files.length === 0) {
        preview.style.display = 'none';
        return;
    }
    
    preview.style.display = 'flex';
    preview.innerHTML = files.map((file, index) => {
        const icon = getFileIcon(file);
        const size = formatFileSize(file.size);
        let previewHtml = '';
        
        // 如果是图片，生成缩略图预览
        if (file.type.startsWith('image/')) {
            previewHtml = `<div class="file-preview-thumb"><img src="${URL.createObjectURL(file)}" alt="${file.name}"></div>`;
        }
        
        return `
            <div class="file-preview-item">
                ${previewHtml}
                <div class="file-preview-info">
                    <span class="file-preview-icon">${icon}</span>
                    <span class="file-preview-name">${escapeHtml(file.name)}</span>
                    <span class="file-preview-size">${size}</span>
                </div>
                <button class="file-preview-remove" onclick="removeFile(${index})" title="移除文件">&times;</button>
            </div>
        `;
    }).join('');
}

/** 移除已选文件 */
function removeFile(index) {
    state.attachedFiles.splice(index, 1);
    updateFilePreview();
    // 重置fileInput以便重新选择相同文件
    dom.fileInput.value = '';
}

/** 处理文件选择事件 */
function handleFileSelect(e) {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    
    // 限制总文件大小（10MB）
    const MAX_TOTAL_SIZE = 10 * 1024 * 1024;
    let totalSize = state.attachedFiles.reduce((sum, f) => sum + f.size, 0);
    
    for (const file of files) {
        totalSize += file.size;
        if (totalSize > MAX_TOTAL_SIZE) {
            alert(`附件总大小不能超过 ${formatFileSize(MAX_TOTAL_SIZE)}，请减少文件数量或选择较小的文件。`);
            dom.fileInput.value = '';
            return;
        }
        state.attachedFiles.push(file);
    }
    
    updateFilePreview();
    // 重置fileInput以便可以重新选择相同文件
    dom.fileInput.value = '';
}

// ===== 消息管理 =====

/** 添加用户消息（支持显示附件） */
function addUserMessage(text, files) {
    const div = document.createElement('div');
    div.className = 'message user';
    
    let filesHtml = '';
    if (files && files.length > 0) {
        filesHtml = `<div class="message-files">${files.map(f => `
            <span class="message-file-tag" title="${escapeHtml(f.name)} (${formatFileSize(f.size)})">
                ${getFileIcon(f)} ${escapeHtml(f.name)}
            </span>
        `).join('')}</div>`;
    }
    
    div.innerHTML = `
        <div class="avatar user-avatar">U</div>
        <div class="message-content">
            <div class="message-header">
                <span class="sender">你</span>
                <span class="time" style="font-size:11px;color:var(--text-secondary);margin-left:8px;">${formatTime()}</span>
            </div>
            ${filesHtml}
            <div class="message-text">${escapeHtml(text)}</div>
        </div>
    `;
    dom.messages.appendChild(div);
    scrollToBottom();
}

/** 创建智能体消息（用于流式输出） */
function createAgentMessage() {
    const div = document.createElement('div');
    div.className = 'message agent';
    div.innerHTML = `
        <div class="avatar agent-avatar">AI</div>
        <div class="message-content">
            <div class="message-header">
                <span class="sender">AI-X 助手</span>
            </div>
            <div class="thinking-panel" style="display:none;">
                <div class="thinking-header" onclick="toggleThinkingPanel(this)">
                    <span class="thinking-toggle">▼</span>
                    <span class="thinking-label">推理过程</span>
                </div>
                <div class="thinking-body"></div>
            </div>
            <div class="message-text">
                <span class="thinking-indicator">
                    <span class="thinking-dot"></span>
                    <span class="thinking-dot"></span>
                    <span class="thinking-dot"></span>
                </span>
            </div>
        </div>
    `;
    dom.messages.appendChild(div);
    scrollToBottom();
    return div;
}

/** 切换推理面板展开/折叠 */
function toggleThinkingPanel(header) {
    const body = header.parentElement.querySelector('.thinking-body');
    const toggle = header.querySelector('.thinking-toggle');
    if (body.style.display === 'none' || !body.style.display) {
        body.style.display = 'block';
        toggle.textContent = '▲';
    } else {
        body.style.display = 'none';
        toggle.textContent = '▼';
    }
}

/** 显示推理过程开始 */
function showThinkingStart(messageElement) {
    const panel = messageElement.querySelector('.thinking-panel');
    if (panel) {
        panel.style.display = 'block';
        const body = panel.querySelector('.thinking-body');
        if (body) {
            body.style.display = 'block';
            body.textContent = '';
        }
        // 设置展开箭头
        const toggle = panel.querySelector('.thinking-toggle');
        if (toggle) toggle.textContent = '▲';
    }
    state.currentThinkingText = '';
    state.isThinking = true;
    setStatus('waiting', '推理中...');
}

/** 更新推理过程内容 */
function updateThinkingDelta(messageElement, delta) {
    const body = messageElement.querySelector('.thinking-body');
    if (body) {
        state.currentThinkingText += delta;
        body.textContent = state.currentThinkingText;
        scrollToBottom();
    }
}

/** 推理过程结束 */
function showThinkingEnd(messageElement) {
    state.isThinking = false;
    setStatus('waiting', '处理中...');
    // 推理结束自动折叠面板，用户可点击展开查看完整推理
    const panel = messageElement.querySelector('.thinking-panel');
    if (panel) {
        const body = panel.querySelector('.thinking-body');
        const toggle = panel.querySelector('.thinking-toggle');
        if (body) body.style.display = 'none';
        if (toggle) toggle.textContent = '▼';
    }
}

/** 获取当前智能体消息的文本容器 */
function getAgentMessageTextDiv(messageElement) {
    return messageElement.querySelector('.message-text');
}

/** 从流式文本中解析推理过程和最终回答 */
function parseThinkingAndAnswer(messageElement, fullText) {
    const thinkingStartMarker = '【推理过程】';
    const answerStartMarker = '【最终回答】';
    
    const hasThinking = fullText.includes(thinkingStartMarker);
    const hasAnswer = fullText.includes(answerStartMarker);
    
    const panel = messageElement.querySelector('.thinking-panel');
    const body = panel ? panel.querySelector('.thinking-body') : null;
    const textDiv = getAgentMessageTextDiv(messageElement);
    
    let thinkingText = '';
    let answerText = fullText;
    
    if (hasThinking) {
        const afterThinkingStart = fullText.split(thinkingStartMarker)[1] || '';
        if (hasAnswer) {
            const parts = afterThinkingStart.split(answerStartMarker);
            thinkingText = parts[0].trim();
            answerText = (parts[1] || '').trim();
        } else {
            // 推理开始但回答标记还没出现
            thinkingText = afterThinkingStart.trim();
            answerText = '';
        }
    }
    
    // 更新推理面板
    if (panel && thinkingText) {
        panel.style.display = 'block';
        if (body) {
            body.style.display = 'block';
            body.textContent = thinkingText;
        }
        const toggle = panel.querySelector('.thinking-toggle');
        if (toggle) toggle.textContent = '▲';
        
        // 推理面板和文本之间显示分隔线
        if (textDiv) {
            textDiv.style.borderTop = '1px dashed var(--border)';
            textDiv.style.paddingTop = '10px';
            textDiv.style.marginTop = '4px';
        }
    }
    
    return answerText;
}

/** 更新流式输出文本 */
function updateStreamingText(messageElement, delta) {
    const textDiv = getAgentMessageTextDiv(messageElement);
    
    // 移除thinking指示器
    const thinkingIndicator = textDiv.querySelector('.thinking-indicator');
    if (thinkingIndicator) {
        thinkingIndicator.remove();
    }
    
    // 追加文本
    state.currentMessageText += delta;
    
    // 尝试解析推理过程
    const answerText = parseThinkingAndAnswer(messageElement, state.currentMessageText);
    
    if (answerText) {
        // 只显示最终回答部分
        textDiv.innerHTML = renderMarkdown(answerText) + '<span class="typing-cursor"></span>';
    } else {
        textDiv.innerHTML = renderMarkdown(state.currentMessageText) + '<span class="typing-cursor"></span>';
    }
    scrollToBottom();
}

/** 完成流式输出 */
function finalizeStreaming(messageElement) {
    const textDiv = getAgentMessageTextDiv(messageElement);
    
    // 移除thinking指示器和光标
    const thinkingIndicator = textDiv.querySelector('.thinking-indicator');
    if (thinkingIndicator) thinkingIndicator.remove();
    const cursor = textDiv.querySelector('.typing-cursor');
    if (cursor) cursor.remove();
    
    // 最终解析（确保推理面板完整）
    const answerText = parseThinkingAndAnswer(messageElement, state.currentMessageText);
    
    // 最终渲染
    textDiv.innerHTML = renderMarkdown(answerText || state.currentMessageText);
    scrollToBottom();
}

/** 显示工具调用指示 */
function showToolCall(messageElement, toolName) {
    const textDiv = getAgentMessageTextDiv(messageElement);
    const toolIndicator = document.createElement('div');
    toolIndicator.className = 'tool-indicator';
    toolIndicator.innerHTML = `<span class="tool-icon">🔧</span> 正在使用工具: ${escapeHtml(toolName)}...`;
    textDiv.appendChild(toolIndicator);
    scrollToBottom();
}

/** 自动滚动到底部 */
function scrollToBottom() {
    requestAnimationFrame(() => {
        dom.messages.scrollTop = dom.messages.scrollHeight;
    });
}

// ===== 聊天API调用 =====

/** 发送消息（流式输出，支持文件附件） */
async function sendMessage() {
    const text = dom.input.value.trim();
    if (!text && state.attachedFiles.length === 0) return;
    if (state.isStreaming) return;
    
    // 获取当前附件的副本
    const files = [...state.attachedFiles];
    
    // 清空输入
    dom.input.value = '';
    dom.charCount.textContent = '0/10000';
    
    // 清空文件预览
    state.attachedFiles = [];
    updateFilePreview();
    
    // 显示用户消息（带附件信息）
    addUserMessage(text, files);
    
    // 创建智能体消息容器
    const agentMessage = createAgentMessage();
    state.currentMessageElement = agentMessage;
    state.currentMessageText = '';
    state.isStreaming = true;
    state.abortController = new AbortController();
    
    // 禁用发送按钮
    dom.sendBtn.disabled = true;
    dom.sendBtn.innerHTML = '<span style="font-size:12px;">●</span>';
    setStatus('waiting', '思考中...');
    
    try {
        let response;
        
        if (files.length > 0) {
            // 有文件附件，使用 multipart/form-data 上传
            const formData = new FormData();
            formData.append('message', text || '(上传了附件)');
            formData.append('sessionId', state.sessionId);
            formData.append('userId', state.userId);
            
            for (const file of files) {
                formData.append('file', file);
            }
            
            response = await fetch('/api/chat/stream/upload', {
                method: 'POST',
                body: formData,
                signal: state.abortController.signal
            });
        } else {
            // 无文件，使用普通JSON请求
            const requestBody = JSON.stringify({
                message: text,
                sessionId: state.sessionId,
                userId: state.userId
            });
            
            response = await fetch('/api/chat/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: requestBody,
                signal: state.abortController.signal
            });
        }
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'HTTP ' + response.status);
        }
        
        // 读取SSE流
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            
            buffer += decoder.decode(value, { stream: true });
            
            // 解析SSE事件
            const events = buffer.split('\n\n');
            buffer = events.pop() || ''; // 保留最后一个不完整的事件
            
            for (const eventStr of events) {
                if (!eventStr.trim()) continue;
                
                const lines = eventStr.split('\n');
                let eventType = '';
                let eventData = '';
                
                for (const line of lines) {
                    if (line.startsWith('event:')) {
                        eventType = line.substring(6).trim();
                    } else if (line.startsWith('data:')) {
                        eventData = line.substring(5).trim();
                    }
                }
                
                if (!eventData) continue;
                
                try {
                    const data = JSON.parse(eventData);
                    
                    switch (eventType) {
                        case 'session':
                            state.sessionId = data.sessionId;
                            sessionStorage.setItem('ai-x-session-id', data.sessionId);
                            break;
                            
                        case 'start':
                            setStatus('waiting', '处理中...');
                            break;
                            
                        case 'delta':
                            // 如果还在推理中，先结束推理模式
                            if (state.isThinking) {
                                showThinkingEnd(agentMessage);
                            }
                            updateStreamingText(agentMessage, data.text);
                            break;
                            
                        case 'thinking_start':
                            showThinkingStart(agentMessage);
                            break;
                            
                        case 'thinking_delta':
                            updateThinkingDelta(agentMessage, data.text);
                            break;
                            
                        case 'thinking_end':
                            showThinkingEnd(agentMessage);
                            break;
                            
                        case 'tool_start':
                            showToolCall(agentMessage, data.tool);
                            break;
                            
                        case 'tool_delta':
                            // 工具输出，作为补充信息
                            break;
                            
                        case 'done':
                            finalizeStreaming(agentMessage);
                            setStatus('success', '就绪');
                            break;
                            
                        case 'error':
                            throw new Error(data.error || '处理出错');
                    }
                } catch (parseError) {
                    if (eventType === 'error') {
                        throw parseError;
                    }
                    console.warn('解析SSE事件失败:', eventStr, parseError);
                }
            }
        }
        
        // 确保消息完成
        finalizeStreaming(agentMessage);
        
    } catch (error) {
        if (error.name === 'AbortError') {
            finalizeStreaming(agentMessage);
            setStatus('success', '已取消');
        } else {
            console.error('发送消息失败:', error);
            
            // 显示错误信息
            const textDiv = getAgentMessageTextDiv(agentMessage);
            textDiv.innerHTML = `
                <div style="color: var(--error);">
                    <strong>出错了:</strong> ${escapeHtml(error.message || '未知错误')}
                </div>
            `;
            setStatus('error', '错误');
        }
    } finally {
        state.isStreaming = false;
        state.currentMessageElement = null;
        state.currentMessageText = '';
        state.abortController = null;
        dom.sendBtn.disabled = false;
        dom.sendBtn.innerHTML = '<span>➤</span>';
        dom.input.focus();
    }
}

// ===== 事件绑定 =====

/** 初始化事件 */
function initEvents() {
    // 发送按钮点击
    dom.sendBtn.addEventListener('click', sendMessage);
    
    // 输入框键盘事件
    dom.input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    
    // 输入框自动调整高度
    dom.input.addEventListener('input', () => {
        dom.input.style.height = 'auto';
        dom.input.style.height = Math.min(dom.input.scrollHeight, 150) + 'px';
        
        // 更新字符计数
        const len = dom.input.value.length;
        dom.charCount.textContent = `${len}/10000`;
    });
    
    // 清空对话
    dom.clearBtn.addEventListener('click', () => {
        // 保留欢迎消息
        const messages = dom.messages.querySelectorAll('.message:not(:first-child)');
        messages.forEach(el => el.remove());
        
        // 重置会话
        state.sessionId = generateSessionId();
        
        // 清空附件
        state.attachedFiles = [];
        updateFilePreview();
        
        // 滚动到底部
        scrollToBottom();
    });
    
    // 文件选择事件
    dom.fileInput.addEventListener('change', handleFileSelect);
}

// ===== 启动 =====

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    initEvents();
    loadSkills();
    
    // 每30秒刷新技能列表
    setInterval(loadSkills, 30000);
    
    // 聚焦输入框
    dom.input.focus();
});