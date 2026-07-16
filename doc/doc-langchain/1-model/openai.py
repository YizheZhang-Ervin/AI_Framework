import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
load_dotenv(override=True)
openai_api_key = os.getenv("OPEN_API_KEY")

model = ChatOpenAI(
    api_key=openai_api_key,
    model="gpt-4",  # 或者 gpt-3.5-turbo
    temperature=0.3,  # 可调
)

# 打印结果
print(model.invoke("什么是LangChain?"))