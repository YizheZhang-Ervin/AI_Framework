# 我们将 FAQ 数据格式化成 json 数据后，接下来就要转成向量数据并存储到向量数据库中，此处以 redis 为例，操作内容包括：

# 使用 向量化模型（Embedding Model，如 BGE、OpenAI Embedding） 将文档片段转换为向量表示。
# 存储至向量数据库（如 Milvus、Weaviate、Redis Vector、Faiss），支持高效的相似度搜索。

import os
import json
import dotenv
import dashscope
import redis
import numpy as np
from http import HTTPStatus
from redis.commands.search.field import TextField, VectorField
from redis.commands.search.index_definition import IndexDefinition

# ========== 配置 ==========
# 加载环境变量
dotenv.load_dotenv()
# 设置 DashScope API Key
dashscope.api_key = os.getenv("DASHSCOPE_API_KEY")

# 定义索引名称、向量维度和距离度量方式
INDEX_NAME = "faq_index"
VECTOR_DIM = 1024
DISTANCE_METRIC = "COSINE"

# 初始化 Redis 客户端连接
redis_client = redis.Redis(
    host="localhost",
    port=6379,
    password=None,
    decode_responses=False
)

# ========== 创建索引（只执行一次） ==========
def create_index():
    """
    创建 Redis 向量搜索索引。
    
    如果索引已存在，则跳过创建并提示信息；
    否则根据预定义的字段结构创建一个新的索引，用于支持 FAQ 的文本与向量混合检索。
    """
    try:
        redis_client.ft(INDEX_NAME).info()
        print("✅ 索引已存在")
    except Exception:
        redis_client.ft(INDEX_NAME).create_index(
            [
                TextField("question"),
                TextField("answer"),
                TextField("source"),
                TextField("category"),
                TextField("crawl_time"),
                VectorField(
                    "embedding",
                    "HNSW",
                    {"TYPE": "FLOAT32", "DIM": VECTOR_DIM, "DISTANCE_METRIC": DISTANCE_METRIC}
                )
            ],
            definition=IndexDefinition(prefix=["faq:"])
        )
        print("✅ 已创建向量索引")

# ========== 插入一条 FAQ ==========
def insert_faq(doc: dict):
    """
    将单条 FAQ 数据插入 Redis，并生成对应的文本嵌入向量。

    参数:
        doc (dict): 包含问题、答案及元数据的字典。
            - question (str): 问题内容
            - answer (str): 回答内容
            - metadata (dict): 元数据，包括 source, category, crawl_time 等字段

    返回值:
        无返回值。结果通过打印输出表示操作是否成功。
    """
    # 拼接问题和答案作为嵌入模型的输入文本
    text_for_embedding = doc["question"] + " " + doc["answer"]

    # 调用 DashScope 多模态嵌入模型获取向量表示
    resp = dashscope.MultiModalEmbedding.call(
        model="multimodal-embedding-v1",
        input=[{"text": text_for_embedding}]
    )

    if resp.status_code == HTTPStatus.OK:
        # 提取嵌入向量并转换为字节格式以便存储到 Redis 中
        embedding = resp.output["embeddings"][0]["embedding"]
        vector = np.array(embedding, dtype=np.float32).tobytes()

        # 构造 Redis 键名
        key = f"faq:{resp.request_id}"
        # 存储 FAQ 数据及其向量表示到 Redis Hash 结构中
        redis_client.hset(key, mapping={
            "question": doc["question"],
            "answer": doc["answer"],
            "source": doc["metadata"]["source"],
            "category": doc["metadata"]["category"],
            "crawl_time": doc["metadata"]["crawl_time"],
            "embedding": vector
        })
        print(f"✅ 已写入 Redis, key={key}")
    else:
        print(f"❌ Embedding 调用失败: {resp.code}, {resp.message}")

# ========== 批量处理 ==========
def insert_from_file(file_path="faq_processed.json"):
    """
    从指定 JSON 文件中读取 FAQ 数据并逐条插入 Redis。

    参数:
        file_path (str): JSON 格式的 FAQ 数据文件路径，默认为 "faq_processed.json"

    返回值:
        无返回值。每条数据插入后会打印状态信息。
    """
    with open(file_path, "r", encoding="utf-8") as f:
        docs = json.load(f)

    for doc in docs:
        insert_faq(doc)

if __name__ == "__main__":
    # 程序入口：先创建索引再批量插入数据
    create_index()
    insert_from_file("faq_processed.json")
