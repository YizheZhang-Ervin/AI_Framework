from typing import List

from langchain_core.callbacks import CallbackManagerForRetrieverRun
from langchain_core.documents import Document
from langchain_core.retrievers import BaseRetriever


class KeywordsRetriever(BaseRetriever):
    """自定义检索器
    
    该检索器根据查询中的关键词来检索相关文档，支持返回前k个匹配的文档
    
    Attributes:
        documents: 文档列表，用于检索的文档集合
        k: 返回文档数量，指定最多返回多少个相关文档
    """
    documents: List[Document]
    k: int

    def _get_relevant_documents(self, query: str, *, run_manager: CallbackManagerForRetrieverRun) -> List[Document]:
        """根据查询关键词检索相关文档
        
        Args:
            query: 查询字符串，将被拆分为多个关键词进行匹配
            run_manager: 回调管理器，用于处理检索过程中的回调
            
        Returns:
            List[Document]: 包含匹配文档的列表，最多返回k个文档
        """
        # 获取返回文档数量参数
        k = self.k if self.k is not None else 3
        documents_result = []

        # 将查询字符串按空格拆分为关键词列表
        query_keywords = query.split(" ")

        # 遍历所有文档，筛选包含任一关键词的文档
        for document in self.documents:
            if any(query_keyword in document.page_content for query_keyword in query_keywords):
                documents_result.append(document)

        # 返回前k个匹配的文档
        return documents_result[:k]


# 定义文档列表，包含用于检索的文本内容
documents = [
    Document("苹果是我最喜欢吃的水果"),
    Document("我喜欢吃苹果"),
    Document("我喜欢用苹果手机"),
]

# 创建关键词检索器实例，设置文档集合和返回文档数量
retriever = KeywordsRetriever(documents=documents, k=1)

# 执行检索操作，根据查询"手机"查找相关文档
result = retriever.invoke("手机")

# 输出检索结果，打印匹配文档的内容
for document in result:
    print(document.page_content)
    print("===========================")
