from langchain_ollama import OllamaEmbeddings
from langchain_redis import RedisConfig, RedisVectorStore
import dotenv

# 读取env配置
dotenv.load_dotenv()

# 初始化 Embedding 模型
embedding = OllamaEmbeddings(model="deepseek-r1:14b")

# 配置Redis连接参数和索引名称
config = RedisConfig(
    index_name="newsgroups",
    redis_url="redis://localhost:6379",
)

# 创建Redis向量存储实例
vector_store = RedisVectorStore(embedding, config=config)


# 创建检索器，进行数据检索
retriever = vector_store.as_retriever()
# retriever = vector_store.as_retriever(search_type="mmr")
# retriever = vector_store.as_retriever(search_type="mmr", search_kwargs={"k": 3})
documents = retriever.invoke("介绍一下我喜欢用什么手机")

for document in documents:
    print(document.page_content)
    print(document.metadata)
    print("=================================")