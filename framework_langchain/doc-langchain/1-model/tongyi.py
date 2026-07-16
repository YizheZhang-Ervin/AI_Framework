import os
from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
load_dotenv(override=True)
qwen_api_key = os.getenv("QWEN_API_KEY")

# 初始化 deepseek
model = ChatTongyi(
    model="deepseek-chat",
    temperature=0,
    max_tokens=None,
    timeout=None,
    max_retries=2,
    api_key=qwen_api_key,
)

# 打印结果
print(model.invoke("什么是LangChain?"))