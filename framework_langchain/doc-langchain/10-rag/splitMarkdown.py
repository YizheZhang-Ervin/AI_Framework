from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import MarkdownHeaderTextSplitter, RecursiveCharacterTextSplitter

# 1.文档加载
# 创建文本加载器并加载Markdown文档
loader = TextLoader(file_path="RAG入门.md")
documents = loader.load()
document_text = documents[0].page_content

# 2.定义文本分割器，设置指定要分割的标题
# 配置Markdown标题分割规则，指定不同级别的标题标记及其对应的元数据标签
headers_to_split_on = [
    ("#", "Header 1"),
    ("##", "Header 2")
]
headers_text_splitter = MarkdownHeaderTextSplitter(headers_to_split_on=headers_to_split_on)

# 3.按标题分割文档
# 使用标题分割器将文档按Markdown标题结构进行分割
headers_splitter_documents = headers_text_splitter.split_text(document_text)

print(f"按标题分割文档数量：{len(headers_splitter_documents)}")
for splitter_document in headers_splitter_documents:
    print(f"按标题分割文档片段大小：{len(splitter_document.page_content)}, 文档元数据：{splitter_document.metadata}")

# 4.定义递归文本分割器
# 创建递归字符分割器，用于进一步细分过大的文档片段
# chunk_size: 每个文本块的目标大小为100个字符
# chunk_overlap: 相邻文本块之间的重叠字符数为30
# length_function: 使用len函数计算文本长度
text_splitter = RecursiveCharacterTextSplitter(chunk_size=100,
                                               chunk_overlap=30,
                                               length_function=len
                                              )

# 5.递归分割文本
# 对已按标题分割的文档片段进行二次递归分割，确保每个片段不超过指定大小
recursive_documents = text_splitter.split_documents(headers_splitter_documents)
print(f"第二次递归文本分割文档数量：{len(recursive_documents)}")
for recursive_document in recursive_documents:
    print(
        f"第二次递归文本分割文档片段大小：{len(recursive_document.page_content)}, 文档元数据：{recursive_document.metadata}")
