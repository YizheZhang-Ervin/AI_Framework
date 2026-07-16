# New LangGraph Project

## Usage
```sh
# init
cd path/to/your/app
pip install -e . "langgraph-cli[inmem]"

# env
cp .env.example .env
# .env
LANGSMITH_API_KEY=lsv2...

# run
langgraph dev
```