from langchain_core.tracers.langchain import LangChainTracer
from langgraph.graph import StateGraph, MessagesState
from langsmith import Client
from langsmith.anonymizer import create_anonymizer

# Use anonymizers to prevent logging of sensitive data in traces ===============================
anonymizer = create_anonymizer([
    # Matches SSNs
    { "pattern": r"\b\d{3}-?\d{2}-?\d{4}\b", "replace": "<ssn>" }
])

tracer_client = Client(anonymizer=anonymizer)
tracer = LangChainTracer(client=tracer_client)
# Define the graph
graph = (
    StateGraph(MessagesState)
    .compile()
    .with_config({'callbacks': [tracer]})
)