package com.bm.copilot.knowledge.pipeline;

public interface Embedder {
    float[] embed(String text);
}
