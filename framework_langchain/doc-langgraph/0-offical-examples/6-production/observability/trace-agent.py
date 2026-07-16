from langchain_core.tracers.langchain import LangChainTracer
from langgraph.graph import StateGraph, MessagesState
from langsmith import Client
from langsmith.anonymizer import create_anonymizer

import langsmith as ls

# agent 


# Trace selectively ===================
# This WILL be traced
with ls.tracing_context(enabled=True):
    agent.invoke({"messages": [{"role": "user", "content": "Send a test email to alice@example.com"}]})
# This will NOT be traced (if LANGSMITH_TRACING is not set)
agent.invoke({"messages": [{"role": "user", "content": "Send another email"}]})

# log to project dynamically ===================
with ls.tracing_context(project_name="email-agent-test", enabled=True):
    # Add metadata to traces
    response = agent.invoke(
        {"messages": [{"role": "user", "content": "Send a welcome email"}]},
        config={
            "tags": ["production", "email-assistant", "v1.0"],
            "metadata": {
                "user_id": "user_123",
                "session_id": "session_456",
                "environment": "production"
            }
        }
    )

# with ls.tracing_context(
#     project_name="email-agent-test",
#     enabled=True,
#     tags=["production", "email-assistant", "v1.0"],
#     metadata={"user_id": "user_123", "session_id": "session_456", "environment": "production"}):
#     response = agent.invoke(
#         {"messages": [{"role": "user", "content": "Send a welcome email"}]}
#     )