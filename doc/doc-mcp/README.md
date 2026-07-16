

 ## MCP

- python
```python
 # 安装依赖
 # sudo apt install python3-venv
 # python -m venv mcp-env
 # source mcp-env/bin/activate  #Linux/Mac
 # pip config set global.index-url https://mirrors.aliyun.com/pypi/simple/
 # pip install mcp fastapi uvicorn  #安装SDK及Web框架

from mcp.server.fastmcp import FastMCP
mcp = FastMCP("数据查询服务")

@mcp.tool()
def get _weather(label: str) -> str:
    return f"{label}数据"

if  _ _name _ _ == " _ _main _ _":
    # stdio本地调试、sse远程HTTP
    mcp.run(transport="stdio")
```

- nodejs
```javascript
// 安装依赖
// npm install @modelcontextprotocol/sdk

import { Server } from "@modelcontextprotocol/sdk/server";
const server = new Server({
  name: "数据查询服务",
  version: "1.0.0"
});

server.defineTool({
  name: "query _data",
  description: "查询数据",
  inputSchema: { type: "object", properties: { status: { type: "string" } } },
  handler: async (args) => {
    return { results:  ["data1", "data2"] };
  }
});

server.start();
```