from loguru import logger
from pydantic import BaseModel
from typing import Optional
from langgraph.graph import StateGraph, START, END


class BranchLoopState(BaseModel):
    """
    状态模型，用于保存当前流程中的变量状态。

    属性:
        x (int): 当前数值。
        done (Optional[bool]): 标记流程是否已完成，默认为 False。
    """
    x: int
    done: Optional[bool] = False


def check_x(state: BranchLoopState) -> BranchLoopState:
    """
    打印当前状态中 x 的值，用于调试和跟踪流程执行。

    参数:
        state (BranchLoopState): 包含当前 x 值的状态对象。

    返回:
        BranchLoopState: 返回未修改的原始状态对象。
    """
    logger.info(f"[check_x] 当前 x = {state.x}")
    return state


def is_even(state: BranchLoopState) -> bool:
    """
    判断当前状态中的 x 是否为偶数。

    参数:
        state (BranchLoopState): 包含当前 x 值的状态对象。

    返回:
        bool: 如果 x 是偶数则返回 True，否则返回 False。
    """
    return state.x % 2 == 0


def increment(state: BranchLoopState) -> BranchLoopState:
    """
    将当前状态中的 x 加一，并记录日志。

    参数:
        state (BranchLoopState): 包含当前 x 值的状态对象。

    返回:
        BranchLoopState: 返回更新后的状态对象（x+1）。
    """
    logger.info(f"[increment] x 是偶数，执行 +1 → {state.x + 1}")
    return BranchLoopState(x=state.x + 1)


def done(state: BranchLoopState) -> BranchLoopState:
    """
    标记流程完成，并记录日志。

    参数:
        state (BranchLoopState): 包含当前 x 值的状态对象。

    返回:
        BranchLoopState: 返回标记为完成的状态对象。
    """
    logger.info(f"[done] x 是奇数，流程结束")
    return BranchLoopState(x=state.x, done=True)


# 创建状态图并定义节点与边的关系
builder = StateGraph(BranchLoopState)
builder.add_node("check_x", check_x)
builder.add_node("increment", increment)
builder.add_node("done_node", done)

# 添加条件边：根据 is_even 函数的结果决定走向 increment 或 done_node
builder.add_conditional_edges("check_x", is_even, {
    True: "increment", False: "done_node"
})

# 定义流程路径：increment 节点之后回到 check_x 形成循环
builder.add_edge("increment", "check_x")

# 设置起始和结束节点连接
builder.add_edge(START, "check_x")
builder.add_edge("done_node", END)

# 编译状态图
graph = builder.compile()

# 绘制流程图为 PNG 图片
graph.get_graph().draw_png('./graph.png')

# 测试用例1：从偶数开始，进入循环直到变为奇数
logger.info("初始 x=6（偶数，进入循环）")
final_state1 = graph.invoke(BranchLoopState(x=6))
logger.info("[最终结果1] ->", final_state1)

# 测试用例2：从奇数开始，直接结束流程
logger.info("初始 x=3（奇数，直接 done）")
final_state2 = graph.invoke(BranchLoopState(x=3))
logger.info("[最终结果2] ->", final_state2)
