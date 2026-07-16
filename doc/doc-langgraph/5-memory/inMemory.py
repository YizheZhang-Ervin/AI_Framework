import dotenv
from langchain_ollama import ChatOllama
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.prebuilt import create_react_agent

# 加载环境变量配置文件
dotenv.load_dotenv()

# 初始化本地大语言模型，模型名称和推理模式
llm = ChatOllama(model="qwen3:14b", reasoning=False)

# 定义工具列表，
tools = []
# 定义短期记忆使用内存（生产可以换 RedisSaver/PostgresSaver）
checkpointer = InMemorySaver()
# 创建ReAct代理，结合语言模型和工具函数
agent = create_react_agent(model=llm, tools=tools, checkpointer=checkpointer)
# 多轮对话配置，同一 thread_id 即同一会话
config = {"configurable": {"thread_id": "user-001"}}

msg1 = agent.invoke({"messages": [("user", "你好，我叫XXX，喜欢学习。")]}, config)
msg1["messages"][-1].pretty_print()

# 6. 第二轮（继续同一 thread）
msg2 = agent.invoke({"messages": [("user", "我叫什么？我喜欢做什么？")]}, config)
msg2["messages"][-1].pretty_print()