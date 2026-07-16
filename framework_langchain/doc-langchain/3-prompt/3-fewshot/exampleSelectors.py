from langchain_core.example_selectors import SemanticSimilarityExampleSelector
from langchain_core.prompts import PromptTemplate, FewShotPromptTemplate
from langchain_ollama import OllamaEmbeddings
from langchain_community.vectorstores import FAISS

# 创建示例模板，用于格式化输入输出对
example_prompt = PromptTemplate.from_template(template="Input:{input},Output:{output}")

# 定义示例数据集，包含输入词和对应的反义词
examples = [
    {"input": "高", "output": "矮"},
    {"input": "高兴", "output": "悲伤"},
    {"input": "高级", "output": "低级"},
    {"input": "高楼大厦", "output": "低矮茅屋"},
    {"input": "高瞻远瞩", "output": "鼠目寸光"}
]

# 初始化嵌入模型，用于将文本转换为向量表示
embedding = OllamaEmbeddings(
    model="qwen3:8b"  # 或其他 embedding 模型
)

# 创建语义相似度示例选择器，用于根据输入选择最相似的示例
# 该选择器使用FAISS向量数据库存储示例嵌入，并返回最相似的k个示例
example_selector = SemanticSimilarityExampleSelector.from_examples(
    examples,
    embedding,
    FAISS,
    k=2,
)

# 创建少样本提示模板，结合示例选择器和提示模板生成最终提示
# 该模板会根据输入选择相似示例，并按照指定格式组合成完整提示
similar_prompt = FewShotPromptTemplate(
    example_selector=example_selector,
    example_prompt=example_prompt,
    prefix="给出每个词语的反义词",
    suffix="输入:{input}",
    input_variables=["input"]
)

# 格式化提示模板，将"开心"作为输入生成最终提示字符串
prompt = similar_prompt.format(input="开心")
print(prompt)