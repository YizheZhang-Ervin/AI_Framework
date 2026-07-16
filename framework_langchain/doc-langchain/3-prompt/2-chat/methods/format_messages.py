from langchain_core.prompts import ChatPromptTemplate

# 创建聊天提示模板，包含系统角色设定和用户问题格式
# 系统消息定义了AI助手的角色，人类消息定义了用户问题的格式
chat_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}，请回答我提出的问题"),
    ("human", "请回答:{question}")
])

# 格式化聊天提示模板，填充角色和问题参数
# 参数role: 指定AI助手的角色身份
# 参数question: 用户提出的具体问题
# 返回值: 格式化后的消息列表
prompt_value = chat_prompt.format_messages(role="python开发工程师", question="冒泡排序怎么写")

# 打印格式化后的提示消息
print(prompt_value)