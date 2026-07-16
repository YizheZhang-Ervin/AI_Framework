# 文档向量数据写入数据库后，接下来就是测试验证召回数据准确性，主要内容包括：

# 用户提问后，将问题转换为向量，与向量数据库中的文档进行相似性匹配。
# 召回与问题最相关的文档片段（如退款流程、配送延误规则），并返回给上层系统。

import os
import dotenv
import dashscope
import redis
import numpy as np
from http import HTTPStatus
from redis.commands.search.query import Query

# ========== 配置 ==========
# 加载环境变量
dotenv.load_dotenv()
# 设置 DashScope API 密钥
dashscope.api_key = os.getenv("DASHSCOPE_API_KEY")

# Redis 向量索引名称
INDEX_NAME = "faq_index"
# 向量维度，用于模型 "multimodal-embedding-v1"
VECTOR_DIM = 1024
# 默认返回最相似的前 K 条结果
TOP_K = 3

# 初始化 Redis 客户端连接
redis_client = redis.Redis(
    host="localhost",
    port=6379,
    password=None,
    decode_responses=False
)

# ========== 将问题转为向量 ==========
def embed_question(question: str):
    """
    使用 DashScope 的多模态嵌入模型将文本问题转换为向量表示。

    参数:
        question (str): 需要转换为向量的文本问题。

    返回:
        bytes: 问题对应的向量表示（以字节形式返回）。

    异常:
        RuntimeError: 如果调用嵌入服务失败，则抛出运行时错误。
    """
    resp = dashscope.MultiModalEmbedding.call(
        model="multimodal-embedding-v1",
        input=[{"text": question}]
    )
    if resp.status_code == HTTPStatus.OK:
        embedding = resp.output["embeddings"][0]["embedding"]
        return np.array(embedding, dtype=np.float32).tobytes()
    else:
        raise RuntimeError(f"❌ Embedding 调用失败: {resp.code}, {resp.message}")

# ========== 相似度搜索 ==========
def search_faq(question: str, top_k=TOP_K):
    """
    根据用户输入的问题，在 Redis 中进行向量相似度搜索，返回最相关的 FAQ 条目。

    参数:
        question (str): 用户提出的问题。
        top_k (int): 返回最相似的前 K 条结果，默认值为 TOP_K。
    """
    # 将问题转换为向量表示
    q_vector = embed_question(question)

    # 构造 RediSearch 的 KNN 查询语句
    query = (
        Query(f"*=>[KNN {top_k} @embedding $vec AS score]")
        .sort_by("score")
        .return_fields("question", "answer", "source", "category", "crawl_time", "score")
        .dialect(2)
    )

    # 执行查询并获取结果
    results = redis_client.ft(INDEX_NAME).search(query, query_params={"vec": q_vector})

    print(f"\n🔎 用户问题: {question}")
    print(f"📊 召回 {len(results.docs)} 条结果\n")

    # 打印每条匹配结果的详细信息
    for i, doc in enumerate(results.docs, start=1):
        print(f"--- Top {i} ---")
        print(f"相似度分数: {doc.score}")
        print(f"Q: {doc.question}")
        print(f"A: {doc.answer}")
        print(f"来源: {doc.source}")
        print(f"类别: {doc.category}")
        print(f"时间: {doc.crawl_time}")
        print()

# ========== 主函数 ==========
if __name__ == "__main__":
    # 测试用例：模拟用户提问
    test_question = "为什么会出现无法下单的情况？"
    search_faq(test_question, top_k=3)
