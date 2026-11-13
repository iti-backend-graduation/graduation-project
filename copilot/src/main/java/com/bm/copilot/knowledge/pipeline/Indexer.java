package com.bm.copilot.knowledge.pipeline;

import com.bm.copilot.knowledge.model.Chunk;
import com.bm.copilot.knowledge.model.Document;
import com.bm.copilot.knowledge.repository.ChunkRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class Indexer {
    private final ChunkRepository chunkRepository;

    public Indexer(ChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public int index(Document document, List<Chunker.ChunkDraft> drafts, Embedder embedder) {
        List<Chunk> chunks = new java.util.ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            Chunker.ChunkDraft draft = drafts.get(index);
            chunks.add(new Chunk(UUID.randomUUID(), document, index, draft.text(), draft.page(),
                    draft.section(), draft.tokenCount(), embedder.embed(draft.text())));
        }
        chunkRepository.saveAll(chunks);
        return chunks.size();
    }
}
