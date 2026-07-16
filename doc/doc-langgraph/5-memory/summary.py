import dotenv
from langchain_core.messages.utils import trim_messages, count_tokens_approximately
from langchain_ollama import ChatOllama
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.prebuilt import create_react_agent
from langgraph.prebuilt.chat_agent_executor import AgentState
from langmem.short_term import SummarizationNode, RunningSummary

# 加载环境变量配置
dotenv.load_dotenv()

# 初始化本地大语言模型，配置模型名称和推理模式
model = ChatOllama(model="qwen3:14b", reasoning=False)

# 定义工具列表，
tools = []

# 创建一个SummarizationNode实例，用于处理文本摘要任务
# 参数说明:
#   token_counter: 用于估算文本token数量的函数，这里使用count_tokens_approximately函数
#   model: 指定使用的语言模型实例
#   max_tokens: 限制处理文本的最大token数量为300
#   max_summary_tokens: 限制生成摘要的最大token数量为128
#   output_messages_key: 指定输出消息在结果中的键名，设置为"llm_input_messages"
summarization_node = SummarizationNode(
    token_counter=count_tokens_approximately,
    model=model,
    max_tokens=300,
    max_summary_tokens=128,
    output_messages_key="llm_input_messages",
)


# 自定义状态类，继承自AgentState，添加上下文字段用于存储运行时摘要信息
class State(AgentState):
    context: dict[str, RunningSummary]

# 初始化内存检查点保存器，用于持久化代理状态
checkpointer = InMemorySaver()

# 创建React代理，整合模型、工具、摘要节点和状态管理器
agent = create_react_agent(
    model=model,
    tools=tools,
    pre_model_hook=summarization_node,
    state_schema=State,
    checkpointer=checkpointer,
)

# 配置线程ID，用于标识用户会话
config = {"configurable": {"thread_id": "user-001"}}

# 启动对话，发送用户自我介绍消息并获取模型响应
msg1 = agent.invoke({"messages": [("user", "你好，我叫XXX")]}, config)
msg1["messages"][-1].pretty_print()

# 定义用户兴趣列表
like_list = ['唱', '跳', 'rap', '篮球']

# 循环发送用户兴趣信息，逐条更新上下文
for i in like_list:
    msg = "我喜欢做的事是：" + i
    print(msg)
    agent.invoke({"messages": [("user", msg)]}, config)

# 查询用户姓名和兴趣，测试模型对上下文的理解能力
msg2 = agent.invoke({"messages": [("user", "我叫什么？我喜欢做的事是什么？")]}, config)
msg2["messages"][-1].pretty_print()
