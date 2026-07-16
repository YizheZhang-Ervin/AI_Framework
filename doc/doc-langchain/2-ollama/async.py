# 异步调用

import asyncio
from langchain_ollama import ChatOllama

# 设置本地模型，不使用深度思考
model = ChatOllama(base_url="http://localhost:11434", model="qwen3:14b", reasoning=False)

async def main():
    # 异步调用一条请求
    response = await model.ainvoke("解释一下LangChain是什么")
    print(response)
    
# 运行异步程序的入口点
asyncio.run(main())