from langchain_core.prompts import ChatPromptTemplate

# 创建聊天提示模板，包含系统角色设定、用户询问和AI回答的对话历史
# 以及用户当前输入的占位符
prompt_template = ChatPromptTemplate([
    ("system", "你是一个AI助手，你的名字是{name}"),
    ("human", "你能做什么事"),
    ("ai", "我可以陪你聊天，讲笑话，写代码"),
    ("human", "{user_input}"),
])

# 使用指定的参数格式化提示模板，生成最终的提示字符串
# name: AI助手的名称
# user_input: 用户的当前输入
prompt = prompt_template.format(name="小张", user_input="你可以做什么")
print(prompt)