from langchain_ollama import ChatOllama

# 初始化
llm = ChatOllama(
    model="llama3.1",
    temperature=0,
    # other params...
)

# invoke
messages = [
    (
        "system",
        "You are a helpful assistant that translates English to French. Translate the user sentence.",
    ),
    ("human", "I love programming."),
]
ai_msg = llm.invoke(messages)
ai_msg

print(ai_msg.content)