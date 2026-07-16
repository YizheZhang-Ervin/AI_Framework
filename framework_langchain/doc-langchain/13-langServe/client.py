from langserve import RemoteRunnable

client = RemoteRunnable("http://127.0.0.1:8000/translation")
print(client.invoke({"input": "你好", "language": "法语"}))
