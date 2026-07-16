# built-in backends

# ==================================== StateBackend (ephemeral)
# By default we provide a StateBackend
agent = create_deep_agent()
# Under the hood, it looks like
from deepagents.backends import StateBackend
agent = create_deep_agent(
    backend=(lambda rt: StateBackend(rt))   # Note that the tools access State through the runtime.state
)

# ==================================== FilesystemBackend (local disk)
from deepagents.backends import FilesystemBackend
agent = create_deep_agent(
    backend=FilesystemBackend(root_dir=".", virtual_mode=True)
)


# ==================================== StoreBackend (LangGraph store)
from langgraph.store.memory import InMemoryStore
from deepagents.backends import StoreBackend
agent = create_deep_agent(
    backend=(lambda rt: StoreBackend(rt)),   # Note that the tools access Store through the runtime.store
    store=InMemoryStore()
)

# ==================================== CompositeBackend (router)
from deepagents import create_deep_agent
from deepagents.backends import CompositeBackend, StateBackend, StoreBackend
from langgraph.store.memory import InMemoryStore

composite_backend = lambda rt: CompositeBackend(
    default=StateBackend(rt),
    routes={
        "/memories/": StoreBackend(rt),
    }
)

agent = create_deep_agent(
    backend=composite_backend,
    store=InMemoryStore()  # Store passed to create_deep_agent, not backend
)