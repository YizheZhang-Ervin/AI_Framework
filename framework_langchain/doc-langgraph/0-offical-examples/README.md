# LangGraph

## 0 Doc
```sh
https://docs.langchain.com/oss/python/langgraph/overview
```

## 1 Basic
```sh
# install
pip install -U langgraph
pip install -U langchain

# test api
pip install langgraph-sdk
```

## 2 CLI
```sh
# install
pip install -U "langgraph-cli[inmem]"

# create
langgraph new path/to/your/app --template new-langgraph-project-python
cd path/to/your/app
pip install -e .
# .env file 
# LANGSMITH_API_KEY=lsv2...

# start
langgraph dev
# safari browser: langgraph dev --tunnel

# debug (launch.json)
pip install debugpy
langgraph dev --debug-port 5678

# LangGraph Studio Web UI
# https://smith.langchain.com/studio/?baseUrl=http://127.0.0.1:2024
```

## 3 Thinking in LangGraph
```sh
- LLM
- Data
- Action
- User Input
```

## 4 Workflows & Agents
```sh
- Augmentations
- Prompt chaining
- Parallelization
- Routing
- Orchestrator-worker
- Evaluator-optimizer
- Agents
```

## 5 Capabilities
```sh
- Persistence
    - Threads,checkpoints,memoryStore,checkpointer libraries
- Durable execution
- Streaming
- Interrupts
- Time travel
- Memory
- Subgraphs
```

## 6 Production
```sh
- pytest
- langsmith studio
- agent chat UI
- langsmith deployment
- langsmith observability
```

## 7 API
```sh
- Graph API
- Functional API
- Runtime
```

## 8 Ollama
```sh
- ChatOllama
- OllamaEmbeddings
```