# 调用Embedding模型计算词向量结果

import dashscope
import json
import os
from http import HTTPStatus
import dotenv

# 读取env配置
dotenv.load_dotenv()
dashscope.api_key = os.getenv("DASHSCOPE_API_KEY")

# 准备输入文本数据
text = "通用多模态表征模型示例"
input = [{'text': text}]

# 调用多模态embedding模型接口进行向量编码
resp = dashscope.MultiModalEmbedding.call(
    model="multimodal-embedding-v1",
    input=input
)

# 处理模型返回结果，提取关键信息并格式化输出
if resp.status_code == HTTPStatus.OK:
    result = {
        "status_code": resp.status_code,
        "request_id": getattr(resp, "request_id", ""),
        "code": getattr(resp, "code", ""),
        "message": getattr(resp, "message", ""),
        "output": resp.output,
        "usage": resp.usage
    }
    print(json.dumps(result, ensure_ascii=False, indent=4))
