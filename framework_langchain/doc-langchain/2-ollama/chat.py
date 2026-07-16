# 对话模型

from langchain_core.messages import SystemMessage, HumanMessage
from langchain_ollama import ChatOllama

# 设置本地模型，不使用深度思考
model = ChatOllama(base_url="http://localhost:11434", model="qwen3:14b", reasoning=False)
# 构建消息列表
messages = [SystemMessage(content="你叫小亮，是一个乐于助人的人工助手"),
            HumanMessage(content="你是谁")
            ]
# 调用大模型
response = model.invoke(messages)
# 打印结果
print(response.content)
print(type(response))