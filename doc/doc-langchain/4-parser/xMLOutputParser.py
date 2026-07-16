from langchain_core.output_parsers import XMLOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from loguru import logger

# 创建 XML 输出解析器实例
parser = XMLOutputParser()

# 获取格式化指令（这会告诉模型如何以 XML 格式输出）
format_instructions = parser.get_format_instructions()

# 创建提示模板
chat_prompt = ChatPromptTemplate.from_messages([
    ("system", f"你是一个AI助手，只能输出XML格式的结构化数据。{format_instructions}"),
    ("human", "请生成5个关于{topic}的内容，每个内容包含<name>和<description>两个字段")
])

# 格式化提示，将 {topic} 替换为实际主题
prompt = chat_prompt.format_messages(topic="小米", format_instructions=format_instructions)

# 打印提示消息
logger.info(prompt)

# 创建 ChatOllama 模型实例
model = ChatOllama(model="qwen3:14b", reasoning=False)

# 执行推理
result = model.invoke(prompt)

# 记录模型原始输出
logger.info(f"模型原始输出:\n{result.content}")

# 解析 XML 输出为结构化 Python 对象（例如字典或列表）
response = parser.invoke(result)

# 打印解析后的结构化结果
logger.info(f"解析后的结构化结果:\n{response}")

# 打印类型
logger.info(f"结果类型: {type(response)}")
