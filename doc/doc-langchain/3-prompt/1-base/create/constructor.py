# 创建提示词

from langchain_core.prompts import PromptTemplate

# 创建一个PromptTemplate对象，用于生成格式化的提示词模板
# 该模板包含两个变量：role（角色）和question（问题）
template = PromptTemplate(template="你是一个专业的{role}工程师，请回答我的问题给出回答，我的问题是：{question}",
                        input_variables=['role', 'question'])

# 使用模板格式化具体的提示词内容
# 将role替换为"python开发"，question替换为"冒泡排序怎么写？"
prompt = template.format(role="python开发",question="冒泡排序怎么写？")

# 输出格式化后的提示词内容
print(prompt)