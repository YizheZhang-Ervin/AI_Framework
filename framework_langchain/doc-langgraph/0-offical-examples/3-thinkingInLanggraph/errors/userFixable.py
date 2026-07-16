# from langgraph.types import Command
# from typing import Literal
# from langgraph.types import interrupt

# def lookup_customer_history(state: State) -> Command[Literal["draft_response"]]:
#     if not state.get('customer_id'):
#         user_input = interrupt({
#             "message": "Customer ID needed",
#             "request": "Please provide the customer's account ID to look up their subscription history"
#         })
#         return Command(
#             update={"customer_id": user_input['customer_id']},
#             goto="lookup_customer_history"
#         )
#     # Now proceed with the lookup
#     customer_data = fetch_customer_history(state['customer_id'])
#     return Command(update={"customer_history": customer_data}, goto="draft_response")