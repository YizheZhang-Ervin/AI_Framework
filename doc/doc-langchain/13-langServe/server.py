from fastapi import FastAPI
from langserve import add_routes
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

# Prompt
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个翻译助手。请将输入翻译成{language}。"),
    ("human", "{input}")
])

# LLM
llm = ChatOllama(base_url="http://localhost:11434", model="qwen3:14b", reasoning=False)
parser = StrOutputParser()

# 构建 chain 对象
translation_chain = prompt | llm | parser

app = FastAPI(
    title="翻译助手",
    version="v1.0",
    description="基于LangChain框架构建的翻译服务"
)

# 直接传 Chain 对象，不要 invoke
add_routes(app, translation_chain, path="/trans")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

# 访问：http://127.0.0.1:8000/docs
# web：http://127.0.0.1:8000/translation/playground/