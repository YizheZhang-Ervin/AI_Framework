from langchain_core.prompts import ChatPromptTemplate

# 创建系统消息模板，用于定义AI助手的身份信息
prompt_template1 = ChatPromptTemplate.from_messages([("system", "你是AI助手，你的名字叫{name}。")])

# 创建人类消息模板，用于定义用户提问的格式
prompt_template2 = ChatPromptTemplate.from_messages([("human", "请问：{question}")])

# 将系统消息模板和人类消息模板组合成完整的对话模板
chat_prompt = ChatPromptTemplate.from_messages([
    prompt_template1,
    prompt_template2
])

# 使用指定的参数格式化消息模板，生成实际的消息内容
message = chat_prompt.format_messages(name="亮仔", question="什么是LangChain")

# 打印生成的消息内容
print(message)