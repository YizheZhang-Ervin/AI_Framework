from typing import TypedDict, Annotated
from langgraph.checkpoint.memory import MemorySaver
from langgraph.constants import START, END
from langgraph.graph import StateGraph
from langgraph.graph.message import add_messages
from langchain_ollama import ChatOllama

class State(TypedDict):
    """
    定义图结构中节点间传递的状态结构

    Attributes:
        messages: 消息列表，使用add_messages函数进行合并
    """
    messages: Annotated[list, add_messages]

# 创建状态图构建器
graph_builder = StateGraph(State)

# 初始化本地大语言模型，配置基础URL、模型名称和推理模式
llm = ChatOllama(base_url="http://localhost:11434", model="qwen3:14b", reasoning=False)

def chatbot(state: State):
    """
    聊天机器人节点函数，处理输入消息并生成回复

    Args:
        state (State): 包含消息历史的状态字典

    Returns:
        dict: 包含新生成消息的字典，格式为{"messages": [回复消息]}
    """
    return {"messages": [llm.invoke(state["messages"])]}

# 将聊天机器人节点添加到图中
graph_builder.add_node("chatbot", chatbot)

# 添加从开始节点到聊天机器人节点的边
graph_builder.add_edge(START, "chatbot")

# 添加从聊天机器人节点到结束节点的边
graph_builder.add_edge("chatbot", END)

# 创建内存保存器用于保存对话状态
memory = MemorySaver()

# 编译图结构并设置检查点保存器
graph = graph_builder.compile(checkpointer=memory)

# 绘制图结构并保存为PNG图片
graph.get_graph().draw_png('./graph.png')

# 配置对话线程ID
config = {"configurable": {"thread_id": "chat-1"}}

# 第一次对话：发送初始消息
msg1 = graph.invoke({"messages": ["你好，我叫XXX，喜欢学习。"]}, config=config)
msg1["messages"][-1].pretty_print()

# 第二次对话：基于上下文询问用户信息
msg2 = graph.invoke({"messages": ["我叫什么？我喜欢做什么？"]}, config=config)
msg2["messages"][-1].pretty_print()
