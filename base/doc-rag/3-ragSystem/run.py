import os
import dotenv
import dashscope
import redis
import numpy as np
from http import HTTPStatus
from redis.commands.search.query import Query
from openai import OpenAI

# ========== 配置 ==========
dotenv.load_dotenv()
dashscope.api_key = os.getenv("DASHSCOPE_API_KEY")

INDEX_NAME = "faq_index"
VECTOR_DIM = 1024
TOP_K = 3

redis_client = redis.Redis(
    host="localhost",
    port=6379,
    password=None,
    decode_responses=False
)

# 初始化 OpenAI 客户端（兼容 DashScope）
client = OpenAI(
    api_key=os.getenv("BAILIAN_API_KEY"),
    base_url=os.getenv("BAILIAN_BASE_URL")
)

# ========== 将问题转为向量 ==========
def embed_question(question: str):
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
    q_vector = embed_question(question)

    query = (
        Query(f"*=>[KNN {top_k} @embedding $vec AS score]")
        .sort_by("score")
        .return_fields("question", "answer", "source", "category", "crawl_time", "score")
        .dialect(2)
    )

    results = redis_client.ft(INDEX_NAME).search(query, query_params={"vec": q_vector})
    return results.docs

# ========== 构建 Prompt ==========
def build_prompt(user_question: str, retrieved_docs, top_k=TOP_K) -> str:
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

# ========== 调用大模型 ==========
def ask_llm(prompt: str) -> str:
    completion = client.chat.completions.create(
        model="deepseek-r1-distill-llama-70b",  # 也可以换成 qwen-turbo / qwen-plus 等
        messages=[{"role": "user", "content": prompt}]
    )

    # 输出最终答案
    return completion.choices[0].message.content

# ========== 主程序 ==========
if __name__ == "__main__":
    while True:
        user_question = input("\n请输入问题（输入 exit 退出）：")
        if user_question.lower() in ["exit", "quit"]:
            break

        docs = search_faq(user_question, top_k=TOP_K)
        if not docs:
            print("⚠️ 未检索到相关文档")
            continue

        prompt = build_prompt(user_question, docs)
        # print("\n===== 构建的 Prompt =====\n")
        # print(prompt)
        # print("\n=========================\n")

        answer = ask_llm(prompt)
        print("💡 大模型回答：")
        print(answer)
