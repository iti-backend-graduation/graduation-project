package com.bm.copilot.knowledge.pipeline;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SpringAiEmbedder implements Embedder {
    private final ObjectProvider<EmbeddingModel> embeddingModels;

    public SpringAiEmbedder(ObjectProvider<EmbeddingModel> embeddingModels) {
        this.embeddingModels = embeddingModels;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModels.getObject().embed(text);
    }
}
