# from langgraph.types import Command
# from typing import Literal

# def execute_tool(state: State) -> Command[Literal["agent", "execute_tool"]]:
#     try:
#         result = run_tool(state['tool_call'])
#         return Command(update={"tool_result": result}, goto="agent")
#     # except ToolError as e:
#     except Exception as e:
#         # Let the LLM see what went wrong and try again
#         return Command(
#             update={"tool_result": f"Tool error: {str(e)}"},
#             goto="agent"
#         )