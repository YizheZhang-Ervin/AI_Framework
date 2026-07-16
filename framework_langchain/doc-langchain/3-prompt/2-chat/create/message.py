from langchain_core.prompts import ChatPromptTemplate

# 创建聊天提示模板，包含系统角色设定和用户问题格式
# 系统消息定义了AI的角色，人类消息定义了问题的输入格式
chat_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}，请回答我提出的问题"),
    ("human", "请回答:{question}")
])

# 使用指定的角色和问题参数填充模板，生成具体的提示内容
# role: 指定AI扮演的角色
# question: 用户提出的具体问题
prompt_value = chat_prompt.invoke({"role": "python开发工程师", "question": "冒泡排序怎么写"})

# 输出生成的提示内容
print(prompt_value.to_string())