from langgraph.constants import START, END
from langgraph.graph import StateGraph
from pydantic import BaseModel

class CalcState(BaseModel):
    """
    定义计算过程中使用的状态模型

    属性:
        x (int): 用于传递和更新的整型数值
    """
    x: int
    
builder = StateGraph(CalcState)

def addition(state):
    """
    执行加法运算的节点函数

    参数:
        state (CalcState): 包含输入数据的状态对象，必须包含属性"x"

    返回:
        CalcState: 返回更新后的状态对象，其中"x"的值增加1
    """
    print(f'加法节点收到的初始值:{state}')
    return CalcState(x=state.x + 1)


def subtraction(state):
    """
    执行减法运算的节点函数

    参数:
        state (CalcState): 包含输入数据的状态对象，必须包含属性"x"

    返回:
        CalcState: 返回更新后的状态对象，其中"x"的值减少2
    """
    print(f'减法节点收到的初始值:{state}')
    return CalcState(x=state.x - 2)


# 向图构建器中添加节点
# 添加加法运算节点和减法运算节点到构建器中
builder.add_node("addition", addition)
builder.add_node("subtraction", subtraction)

# 定义节点之间的执行顺序 edges
# 设置节点间的依赖关系，形成执行流程图
builder.add_edge(START, "addition")
builder.add_edge("addition", "subtraction")
builder.add_edge("subtraction", END)

# 编译图构建器生成计算图
graph = builder.compile()

# 打印图的边和节点信息
print(builder.edges)
print(builder.nodes)

# 打印图的可视化结构
print(graph.get_graph().print_ascii())

# 定义一个初始状态对象，包含属性"x"为5
initial_state = CalcState(x=5)

# 调用graph对象的invoke方法，传入初始状态，执行图计算流程
result = graph.invoke(initial_state)

print(f"最后的结果是:{result}")
