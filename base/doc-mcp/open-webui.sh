# open-webui 自托管AI平台
docker run -d -p 8080:8080 -e HF_HUB_OFFLINE=1 --add-host=host.docker.internal:host-gateway -v $PWD/open-webui:/app/backend/data --name open-webui ghcr.io/open-webui/open-webui:main

# mcpo 本地运行的AI工具可以立刻支持云端部署、适配各种 UI
pip install uv
pip install mcpo
uvx mcpo --port 8888 -- uvx mcp-server-time --local-timezone=Asia/Shanghai