# 把 用户问题 + 检索召回的上下文 拼接成一个高质量的 Prompt 送给大模型。

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
# 设置 DashScope API Key
dashscope.api_key = os.getenv("DASHSCOPE_API_KEY")

# Redis 向量索引名称
INDEX_NAME = "faq_index"
# 向量维度
VECTOR_DIM = 1024
# 相似度搜索返回的最相似结果数量
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
        question (str): 用户输入的问题文本。

    返回:
        bytes: 问题对应的向量表示（以字节形式存储）。

    异常:
        RuntimeError: 当调用嵌入服务失败时抛出异常。
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
    在 Redis 中基于向量相似度搜索与用户问题最相关的 FAQ 文档。

    参数:
        question (str): 用户提出的问题。
        top_k (int): 返回最相似的前 K 个文档，默认使用 TOP_K 常量。

    返回:
        list: 包含匹配文档对象的列表，每个对象包含字段如 question、answer、source 等。
    """
    q_vector = embed_question(question)

    # 构造 Redis 向量搜索查询语句
    query = (
        Query(f"*=>[KNN {top_k} @embedding $vec AS score]")
        .sort_by("score")
        .return_fields("question", "answer", "source", "category", "crawl_time", "score")
        .dialect(2)
    )

    # 执行搜索并返回结果
    results = redis_client.ft(INDEX_NAME).search(query, query_params={"vec": q_vector})
    return results.docs

# ========== 构建 Prompt ==========
def build_prompt(user_question: str, retrieved_docs, top_k=TOP_K) -> str:
    """
    根据用户问题和检索到的相关文档构建用于大模型推理的 Prompt。

    参数:
        user_question (str): 用户提出的问题。
        retrieved_docs (list): 检索到的相关文档列表。
        top_k (int): 使用的文档数量上限，默认为 TOP_K。

    返回:
        str: 构建完成的 Prompt 字符串。
    """
    context_parts = []
    for i, doc in enumerate(retrieved_docs[:top_k], start=1):
        context_parts.append(
            f"【文档片段{i}】\nQ: {doc.question}\nA: {doc.answer}"
        )
    context_text = "\n\n".join(context_parts)

    prompt = f"""
你是一个智能问答助手，请仅根据提供的文档片段回答用户问题。
如果文档片段中没有相关内容，请回答“未找到相关信息”。

用户问题：
{user_question}

可用文档片段：
{context_text}

请基于以上信息，生成简洁明了的回答：
"""
    return prompt.strip()

# ========== 主函数 ==========
if __name__ == "__main__":
    # 循环接收用户输入并进行问答处理
    while True:
        user_question = input("\n请输入问题（输入 exit 退出）：")
        if user_question.lower() in ["exit", "quit"]:
            break

        docs = search_faq(user_question, top_k=TOP_K)
        if not docs:
            print("⚠️ 未检索到相关文档")
            continue

        prompt = build_prompt(user_question, docs)
        print("\n===== 构建的 Prompt =====\n")
        print(prompt)
        print("\n=========================\n")

