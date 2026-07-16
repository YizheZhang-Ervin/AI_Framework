from langchain.retrievers import MultiQueryRetriever
from langchain_core.prompts import PromptTemplate
from langchain_ollama import OllamaEmbeddings, ChatOllama
from langchain_redis import RedisConfig, RedisVectorStore
import dotenv

# 读取env配置
dotenv.load_dotenv()

# 初始化 Embedding 模型
embedding = OllamaEmbeddings(model="deepseek-r1:14b")
# 初始化大语言模型
llm = ChatOllama(model="deepseek-r1:14b", reasoning=False)
# 配置Redis连接参数和索引名称
config = RedisConfig(
    index_name="newsgroups",
    redis_url="redis://localhost:6379",
)

# 创建Redis向量存储实例
vector_store = RedisVectorStore(embedding, config=config)

# 创建多查询检索器
retriever = vector_store.as_retriever()
retriever_from_llm = MultiQueryRetriever.from_llm(
    retriever=retriever, llm=llm,
    prompt=PromptTemplate(
        input_variables=["question"],
        template="""你是一个 AI 语言模型助手。你的任务是：
        为给定的用户问题生成 3 个不同的版本，以便从向量数据库中检索相关文档。
        通过生成用户问题的多种视角（改写版本），
        你的目标是帮助用户克服基于距离的相似性搜索的某些局限性。
        请将这些改写后的问题用换行符分隔开。原始问题：{question}""")
)

# 5.进行数据检索
documents = retriever_from_llm.invoke("介绍一下我喜欢使用的手机")

for document in documents:
    print(document.page_content)
    print(document.metadata)
    print("=================================")
