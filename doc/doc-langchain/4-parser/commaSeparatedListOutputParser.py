from langchain_core.output_parsers import CommaSeparatedListOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from loguru import logger

# 创建逗号分隔列表输出解析器实例
parser = CommaSeparatedListOutputParser()

# 获取格式化指令，用于指导模型输出格式
format_instructions = parser.get_format_instructions()

# 创建聊天提示模板，包含系统消息和人类消息
# 系统消息定义了AI助手的行为规范和输出格式要求
# 人类消息定义了具体的任务请求，使用占位符{topic}表示主题
chat_prompt = ChatPromptTemplate.from_messages([
    ("system", f"你是一个AI助手，你只能输出结构化列表数据。{format_instructions}"),
    ("human", "请生成5个关于{topic}的内容")
])

# 格式化聊天提示消息，将占位符替换为实际值
prompt = chat_prompt.format_messages(topic="小米", format_instructions=format_instructions)

# 记录格式化后的提示消息
logger.info(prompt)

# 创建ChatOllama模型实例，指定使用的模型名称和推理模式
model = ChatOllama(model="qwen3:14b", reasoning=False)

# 调用模型执行推理，传入格式化的提示消息
result = model.invoke(prompt)

# 记录模型返回的原始结果
logger.info(f"模型原始输出:\n{result}")

# 使用解析器处理模型返回的结果，将其转换为结构化列表
response = parser.invoke(result)
logger.info(f"解析后的结构化结果:\n{response}")

# 打印类型
logger.info(f"结果类型: {type(response)}")