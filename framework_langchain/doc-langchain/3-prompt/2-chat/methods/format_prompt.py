from langchain_core.prompts import ChatPromptTemplate

# 创建聊天提示模板，包含系统角色设定和用户问题格式
# 该模板定义了两个消息：系统消息用于设定AI助手的角色，人类消息用于接收用户的具体问题
chat_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}，请回答我提出的问题"),
    ("human", "请回答:{question}")
])

# 使用指定的角色和问题参数格式化聊天提示模板
# role: 指定AI助手的角色身份
# question: 用户提出的具体问题
# 返回格式化后的提示对象，可用于后续的模型调用
prompt = chat_prompt.format_prompt(role="python开发工程师", question="冒泡排序怎么写")

# 打印格式化后的提示内容
print(prompt)

# 将提示转换为消息列表并打印
print(prompt.to_messages())