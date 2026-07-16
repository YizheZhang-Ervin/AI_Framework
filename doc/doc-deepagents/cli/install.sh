# 1 install
uvx deepagents-cli 或 pip install deepagents-cli

# 2 Give the agent a task
> Create a Python script that prints "Hello, World!"

# 3 Teach the agent conventions once
uvx deepagents-cli --agent backend-dev
> Our API uses snake_case and includes created_at/updated_at timestamps
## remembers for future sessions
> Create a /users endpoint

# 4 Use remote sandboxes
## Runloop
export RUNLOOP_API_KEY="your-key"
## Daytona
export DAYTONA_API_KEY="your-key"
## Modal
modal setup
## run
uvx deepagents-cli --sandbox runloop --sandbox-setup ./setup.sh