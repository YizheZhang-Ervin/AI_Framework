from typing import List

from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import TextSplitter


class CustomTextSplitter(TextSplitter):
    """
    自定义文本分割器类
    
    该类继承自TextSplitter，用于将文本按照特定规则进行分割
    分割策略：首先按段落分割，然后对每个段落提取第一句话
    """

    def split_text(self, text: str) -> List[str]:
        """
        将输入文本分割成多个文本片段
        
        参数:
            text (str): 需要分割的原始文本字符串
            
        返回:
            List[str]: 分割后的文本片段列表，每个片段为段落的第一句话
        """
        text = text.strip()
        # 1.按段落进行分割
        text_array = text.split("\n\n")

        result_texts = []
        for text_item in text_array:
            strip_text_item = text_item.strip()
            if strip_text_item is None:
                continue
            # 2.按句进行分割
            result_texts.append(strip_text_item.split("。")[0])
        return result_texts


# 1.文档加载
loader = TextLoader(file_path="RAG入门.md")
documents = loader.load()
document_text = documents[0].page_content

# 2.定义文本分割器
splitter = CustomTextSplitter()

# 3.文本分割
splitter_texts = splitter.split_text(document_text)
for splitter_text in splitter_texts:
    print(
        f"文本分割片段大小：{len(splitter_text)}, 文本内容：{splitter_text}")
