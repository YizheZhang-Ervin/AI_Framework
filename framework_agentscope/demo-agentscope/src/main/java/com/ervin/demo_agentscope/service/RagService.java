package com.ervin.demo_agentscope.service;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.store.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    public Knowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    Knowledge knowledge;

    public RagService() {
        // 1. 创建知识库
        EmbeddingModel embeddingModel = DashScopeTextEmbedding.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("text-embedding-v3")
                .dimensions(1024)
                .build();
        this.knowledge = SimpleKnowledge.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(InMemoryStore.builder().dimensions(1024).build())
                .build();
    }

    public void add() {
        // 2. 添加文档
        TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
        List<Document> docs = reader.read(ReaderInput.fromString("文本内容...")).block();
        knowledge.addDocuments(docs).block();
    }

    public List<Document> get() {
        // 3. 检索
        List<Document> results = knowledge.retrieve("查询内容",
                RetrieveConfig.builder().limit(3).scoreThreshold(0.5).build()).block();
        return results;
    }
}
