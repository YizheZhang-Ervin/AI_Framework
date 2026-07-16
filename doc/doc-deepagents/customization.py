from langchain_ollama import ChatOllama
from langchain.chat_models import init_chat_model
from deepagents import create_deep_agent
import os
from typing import Literal
from tavily import TavilyClient

# ==================================== model
model = init_chat_model(
    model=ChatOllama(
        model="llama3.1",
        temperature=0,
        # other params...
    )
)

# ==================================== prompt
research_instructions = """\
You are an expert researcher. Your job is to conduct \
thorough research, and then write a polished report. \
"""

# ==================================== tools
tavily_client = TavilyClient(api_key=os.environ["TAVILY_API_KEY"])

def internet_search(
    query: str,
    max_results: int = 5,
    topic: Literal["general", "news", "finance"] = "general",
    include_raw_content: bool = False,
):
    """Run a web search"""
    return tavily_client.search(
        query,
        max_results=max_results,
        include_raw_content=include_raw_content,
        topic=topic,
    )

agent = create_deep_agent(model=model,system_prompt=research_instructions,tools=[internet_search])