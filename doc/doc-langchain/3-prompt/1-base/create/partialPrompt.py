from datetime import datetime
from langchain_core.prompts import PromptTemplate

# 创建一个包含时间变量的模板，时间变量使用partial_variables预设为当前时间
# 然后格式化问题生成最终提示词
template1 = PromptTemplate.from_template("现在时间是：{time},请对我的问题给出答案，我的问题是：{question}",
                                         partial_variables={"time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")})
prompt1 = template1.format(question="今天是几号？")
print(prompt1)

# 创建一个包含时间变量的模板，通过partial方法预设时间变量为当前时间
# 然后格式化问题生成最终提示词
template2 = PromptTemplate.from_template("现在时间是：{time},请对我的问题给出答案，我的问题是：{question}")
partial = template2.partial(time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
prompt2 = partial.format(question="今天是几号？")
print(prompt2)
