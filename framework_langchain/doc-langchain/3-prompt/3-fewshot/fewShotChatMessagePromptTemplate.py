from langchain_core.prompts import ChatPromptTemplate, FewShotChatMessagePromptTemplate

# 定义示例数据，用于少样本学习
# 包含输入输出对，展示乘法运算的格式和结果
examples = [
    {"input": "1✖️2", "output": "2"},
    {"input": "2✖️2", "output": "4"},
]

# 创建示例提示模板，定义了人类提问和AI回答的交互格式
# human消息使用"{input}是多少"的模板
# ai消息使用"{output}"的模板
example_prompt = ChatPromptTemplate.from_messages([
    ("human", "{input}是多少"),
    ("ai", "{output}"),
])

# 创建少样本聊天消息提示模板
# 使用预定义的示例数据和示例提示模板
# 该模板将用于在最终提示中提供上下文示例
few_shot_prompt = FewShotChatMessagePromptTemplate(
    examples=examples,
    example_prompt=example_prompt,
)

# 构建最终的提示模板
# 组合系统角色设定、少样本示例和用户问题
# 系统设定AI为数学奇才，然后添加示例，最后是用户的具体问题
final_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一名百年一遇的数学奇才")]) + few_shot_prompt + ChatPromptTemplate.from_messages([
    ("human", "{question}"),
])

# 格式化并打印最终提示模板，传入具体问题"3✖️2"
print(final_prompt.format(question="3✖️2"))