from langchain_ollama import ChatOllama
from tools import get_weather, write_file
from langgraph.prebuilt import create_react_agent

# 初始化本地大语言模型，配置基础URL、模型名称和推理模式
llm = ChatOllama( model="qwen3:14b", reasoning=False)

# 定义工具列表，包含天气查询和结果写入工具
tools = [get_weather, write_file]

# 创建ReAct代理，结合语言模型和工具函数
agent = create_react_agent(model=llm, tools=tools)

# 调用代理处理用户查询，获取北京天气信息
response = agent.invoke({"messages": [{"role": "user", "content": "请问北京天气怎么样？然后把回答结果写入文件。"}]})
# 输出完整响应结果和最终回答内容
print(response)
response["messages"][-1].pretty_print()