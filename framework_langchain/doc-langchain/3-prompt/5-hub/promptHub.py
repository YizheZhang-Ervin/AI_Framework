from langchain import hub

prompt = hub.pull("hwchase17/openai-tools-agent")

# 查看结构（Langchain PromptTemplate 的 repr）
print(prompt)

# 或者访问具体字段
print(prompt.messages)