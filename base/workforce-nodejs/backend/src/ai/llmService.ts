import path from 'path';
import { fileURLToPath } from 'url';
import { readdirSync, existsSync } from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const defaultModelsDir = path.join(__dirname, '../../../models');

let llamaInstance: any = null;
let modelInstance: any = null;

// Get provider type from env
function getProvider(): 'local' | 'openai' {
  return (process.env.LLM_PROVIDER || 'local') as 'local' | 'openai';
}

// Resolve local model file path
function resolveModelPath(): string | null {
  const envPath = process.env.LLM_MODEL_PATH;
  if (envPath) {
    const resolved = path.resolve(envPath);
    if (existsSync(resolved)) {
      if (resolved.endsWith('.gguf')) return resolved;
      const files = readdirSync(resolved);
      const ggufFile = files.find(f => f.endsWith('.gguf'));
      if (ggufFile) return path.join(resolved, ggufFile);
    }
    console.warn(`LLM_MODEL_PATH "${envPath}" not found, falling back to default models/ directory`);
  }

  if (!existsSync(defaultModelsDir)) return null;
  const files = readdirSync(defaultModelsDir);
  const ggufFile = files.find(f => f.endsWith('.gguf'));
  return ggufFile ? path.join(defaultModelsDir, ggufFile) : null;
}

// Initialize LLM
export async function initLLM() {
  const provider = getProvider();
  console.log(`LLM Provider: ${provider}`);

  if (provider === 'openai') {
    const baseUrl = process.env.OPENAI_BASE_URL || 'https://api.openai.com/v1';
    const apiKey = process.env.OPENAI_API_KEY;
    const model = process.env.OPENAI_MODEL || 'gpt-3.5-turbo';
    console.log(`OpenAI-compatible API configured: ${baseUrl} / ${model}`);
    if (!apiKey || apiKey === 'sk-your-api-key-here') {
      console.warn('WARNING: OPENAI_API_KEY is not set or is a placeholder.');
      console.warn('  Set OPENAI_API_KEY and OPENAI_BASE_URL in .env file.');
    }
    return { llama: null, model: null };
  }

  // Local provider
  try {
    const modelPath = resolveModelPath();
    if (!modelPath) {
      console.log('AI Assistant: running in mock mode (no .gguf model found)');
      console.log(`  Place a .gguf model file in ${defaultModelsDir}`);
      return { llama: null, model: null };
    }

    console.log(`Loading model from: ${modelPath}`);
    const { getLlama, LlamaLogLevel } = await import('node-llama-cpp');
    llamaInstance = await getLlama({ logLevel: LlamaLogLevel.error });
    modelInstance = await llamaInstance.loadModel({ modelPath });
    console.log('AI Assistant: model loaded successfully!');
    return { llama: llamaInstance, model: modelInstance };
  } catch (error) {
    console.error('Failed to initialize LLM:', error);
    console.log('AI Assistant: running in mock mode');
    return { llama: null, model: null };
  }
}

// Build OpenAI-compatible messages array
function buildOpenAIMessages(
  userMessage: string,
  history: { role: string; content: string }[] = [],
  expert?: { id: string; name: string; title: string; description: string; skills: { id: number; name: string }[] } | null
): { role: string; content: string }[] {
  let systemPrompt = '你是一个智能AI助手，请用中文回答用户的问题，提供专业、准确的帮助。\n\n重要：在回答之前，请先用[思考过程]标签展示你的推理步骤，然后再给出最终答案。格式如下：\n\n[思考过程]\n你的推理过程...\n[/思考过程]\n\n最终答案内容...\n\n如果问题很简单，思考过程可以简短。';

  // If expert is provided, role-play as the expert
  if (expert) {
    const skillsText = expert.skills && expert.skills.length > 0
      ? expert.skills.map(s => s.name).join('、')
      : '无特定技能';
    systemPrompt = `你现在是一个专家角色扮演。你的身份信息如下：

姓名：${expert.name}
职称：${expert.title}
简介：${expert.description || '暂无描述'}
专长：${skillsText}

请完全以这个专家的身份和语气来回答用户的问题。根据你的专业领域提供专业、准确的回答。不要提及你是一个AI助手，而是以专家的身份直接回答问题。

重要：在回答之前，请先用[思考过程]标签展示你的推理步骤，然后再给出最终答案。格式如下：

[思考过程]
你的推理过程...
[/思考过程]

最终答案内容...`;
  }

  const messages: { role: string; content: string }[] = [
    {
      role: 'system',
      content: systemPrompt
    }
  ];

  for (const msg of history) {
    if (msg.role === 'user' || msg.role === 'assistant') {
      // Strip thinking markers from stored history to avoid accumulating them
      const cleanContent = msg.content.replace(/\[思考过程\][\s\S]*?\[\/思考过程\]\s*/g, '').trim();
      messages.push({ role: msg.role, content: cleanContent || msg.content });
    }
  }

  messages.push({ role: 'user', content: userMessage });
  return messages;
}

// Retry delay helper
function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// Call OpenAI-compatible API with a specific model (non-streaming)
async function callOpenAIModel(
  messages: { role: string; content: string }[],
  model: string,
  maxTokens: number = 2048
): Promise<string> {
  const baseUrl = process.env.OPENAI_BASE_URL || 'https://api.openai.com/v1';
  const apiKey = process.env.OPENAI_API_KEY;

  if (!apiKey || apiKey === 'sk-your-api-key-here') {
    throw new Error('OPENAI_API_KEY is not configured');
  }

  const response = await fetch(`${baseUrl}/chat/completions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model,
      messages,
      temperature: 0.7,
      max_tokens: maxTokens
    })
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`OpenAI API error (${response.status}): ${errorBody}`);
  }

  const data = await response.json() as any;
  return data.choices[0]?.message?.content || '';
}

// Call OpenAI-compatible API with streaming (SSE). Yields partial content chunks.
async function* callOpenAIModelStream(
  messages: { role: string; content: string }[],
  model: string
): AsyncGenerator<string, void, unknown> {
  const baseUrl = process.env.OPENAI_BASE_URL || 'https://api.openai.com/v1';
  const apiKey = process.env.OPENAI_API_KEY;

  if (!apiKey || apiKey === 'sk-your-api-key-here') {
    throw new Error('OPENAI_API_KEY is not configured');
  }

  const response = await fetch(`${baseUrl}/chat/completions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model,
      messages,
      temperature: 0.7,
      max_tokens: 2048,
      stream: true
    })
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`OpenAI API error (${response.status}): ${errorBody}`);
  }

  const reader = response.body!.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || ''; // keep incomplete line in buffer

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed || trimmed === 'data: [DONE]') continue;
      if (trimmed.startsWith('data: ')) {
        try {
          const json = JSON.parse(trimmed.slice(6));
          const delta = json.choices?.[0]?.delta?.content;
          if (delta) {
            yield delta;
          }
        } catch {
          // skip malformed JSON chunks
        }
      }
    }
  }
}

// Generate text response via OpenAI-compatible API with retry + backup model fallback (non-streaming)
async function generateOpenAIResponse(
  messages: { role: string; content: string }[]
): Promise<string> {
  const primaryModel = process.env.OPENAI_MODEL || 'gpt-3.5-turbo';
  const backupModel = process.env.OPENAI_BACKUP_MODEL;
  const maxRetries = 3;

  // Try primary model with retries
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      console.log(`OpenAI API attempt ${attempt}/${maxRetries} (model: ${primaryModel})`);
      const result = await callOpenAIModel(messages, primaryModel);
      return result;
    } catch (error) {
      const errMsg = (error as Error).message;
      const isOverload = errMsg.includes('429') || errMsg.includes('访问量过大') || errMsg.includes('rate limit') || errMsg.includes('too many');
      console.error(`OpenAI API attempt ${attempt}/${maxRetries} failed: ${errMsg}${isOverload ? ' (overload/rate limit)' : ''}`);
      if (attempt < maxRetries) {
        const delay = 2000 * attempt; // exponential backoff: 2s, 4s, 6s
        console.log(`Retrying in ${delay}ms...`);
        await sleep(delay);
      }
    }
  }

  // Primary model failed after retries, try backup model
  if (backupModel) {
    const backupRetries = 2;
    for (let attempt = 1; attempt <= backupRetries; attempt++) {
      try {
        console.log(`Primary model failed, switching to backup model: ${backupModel} (attempt ${attempt}/${backupRetries})`);
        const result = await callOpenAIModel(messages, backupModel);
        return result;
      } catch (error) {
        const errMsg = (error as Error).message;
        console.error(`Backup model attempt ${attempt}/${backupRetries} failed: ${errMsg}`);
        if (attempt < backupRetries) {
          await sleep(2000);
        }
      }
    }
  }

  throw new Error(`All models failed. Primary: ${primaryModel} after ${maxRetries} retries${backupModel ? `, Backup: ${backupModel} after 2 retries` : ', no backup model configured'}.`);
}

// Generate text response via OpenAI-compatible API with streaming (SSE)
// Yields content chunks. Retries up to 3 times with exponential backoff, then falls back to backup model.
async function* generateOpenAIResponseStream(
  messages: { role: string; content: string }[]
): AsyncGenerator<string, void, unknown> {
  const primaryModel = process.env.OPENAI_MODEL || 'gpt-3.5-turbo';
  const backupModel = process.env.OPENAI_BACKUP_MODEL;
  const maxRetries = 3;

  // Try primary model with retries
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      console.log(`OpenAI API stream attempt ${attempt}/${maxRetries} (model: ${primaryModel})`);
      for await (const chunk of callOpenAIModelStream(messages, primaryModel)) {
        yield chunk;
      }
      return;
    } catch (error) {
      const errMsg = (error as Error).message;
      const isOverload = errMsg.includes('429') || errMsg.includes('访问量过大') || errMsg.includes('rate limit') || errMsg.includes('too many');
      console.error(`OpenAI API stream attempt ${attempt}/${maxRetries} failed: ${errMsg}${isOverload ? ' (overload/rate limit)' : ''}`);
      if (attempt < maxRetries) {
        const delay = 2000 * attempt; // exponential backoff: 2s, 4s, 6s
        console.log(`Retrying in ${delay}ms...`);
        await sleep(delay);
      }
    }
  }

  // Primary model failed after retries, try backup model
  if (backupModel) {
    const backupRetries = 2;
    for (let attempt = 1; attempt <= backupRetries; attempt++) {
      try {
        console.log(`Primary model failed, switching to backup model: ${backupModel} (attempt ${attempt}/${backupRetries})`);
        for await (const chunk of callOpenAIModelStream(messages, backupModel)) {
          yield chunk;
        }
        return;
      } catch (error) {
        const errMsg = (error as Error).message;
        console.error(`Backup model attempt ${attempt}/${backupRetries} failed: ${errMsg}`);
        if (attempt < backupRetries) {
          await sleep(2000);
        }
      }
    }
  }

  // Last resort: mock streaming
  console.warn('All models failed, falling back to mock streaming response');
  const mockText = `抱歉，AI模型暂时不可用（访问量过大），请稍后再试。`;
  for (const char of mockText) {
    yield char;
    await sleep(10);
  }
}

// Generate text response via local model
async function generateLocalResponse(
  userMessage: string,
  history: { role: string; content: string }[] = []
): Promise<string> {
  const systemPrompt = '你是一个智能AI助手，请用中文回答用户的问题，提供专业、准确的帮助。';

  let prompt = systemPrompt + '\n\n';
  for (const msg of history) {
    const role = msg.role === 'user' ? '用户' : 'AI';
    prompt += `${role}: ${msg.content}\n`;
  }
  prompt += `用户: ${userMessage}\nAI:`;

  if (modelInstance) {
    try {
      const { LlamaCompletion } = await import('node-llama-cpp');
      const context = await modelInstance.createContext();
      const sequence = context.getSequence();
      const completion = new LlamaCompletion({ contextSequence: sequence });
      const response = await completion.generateCompletion(prompt, {
        temperature: 0.7,
        maxTokens: 2048
      });
      return response;
    } catch (error) {
      console.error('Model inference failed, falling back to mock:', (error as Error).message);
    }
  }

  // Mock fallback
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(`你好！我是AI助手。关于"${userMessage}"，我可以提供以下帮助：\n\n1. 详细分析问题背景\n2. 提供专业解决方案\n3. 给出具体建议和步骤\n\n请告诉我更多细节，我会为你提供更精准的帮助！`);
    }, 500);
  });
}

// Generate text response (main entry point)
export async function generateResponse(
  userMessage: string,
  history: { role: string; content: string }[] = [],
  expert?: { id: string; name: string; title: string; description: string; skills: { id: number; name: string }[] } | null
): Promise<string> {
  const provider = getProvider();

  if (provider === 'openai') {
    const messages = buildOpenAIMessages(userMessage, history, expert);
    return generateOpenAIResponse(messages);
  }

  return generateLocalResponse(userMessage, history);
}



// Cleanup
export async function shutdownLLM() {
  if (modelInstance) {
    await modelInstance.dispose();
    modelInstance = null;
    llamaInstance = null;
    console.log('LLM shut down');
  }
}

// Generate streaming response (main entry point) — matches generateResponse signature
export async function* generateResponseStream(
  userMessage: string,
  history: { role: string; content: string }[] = [],
  expert?: { id: string; name: string; title: string; description: string; skills: { id: number; name: string }[] } | null
): AsyncGenerator<string, void, unknown> {
  const provider = getProvider();

  if (provider === 'openai') {
    const messages = buildOpenAIMessages(userMessage, history, expert);
    for await (const chunk of generateOpenAIResponseStream(messages)) {
      yield chunk;
    }
    return;
  }

  // For local provider, just yield the full response at once
  const full = await generateLocalResponse(userMessage, history);
  // Yield character by character for realistic streaming
  for (const char of full) {
    yield char;
    await sleep(10);
  }
}

export default { initLLM, generateResponse, generateResponseStream, shutdownLLM };