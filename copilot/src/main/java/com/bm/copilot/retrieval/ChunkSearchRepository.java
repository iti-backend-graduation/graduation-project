package com.bm.copilot.retrieval;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The two raw searches over the chunk table. Every value is a bound parameter, never concatenated into SQL.
 */
@Repository
public class ChunkSearchRepository {

    private static final String SELECT_COLUMNS = """
            SELECT c.id, c.text, c.page, c.section, d.file_name
            """;

    private final JdbcClient jdbcClient;

    public ChunkSearchRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Closest chunks by cosine distance. The score is the cosine similarity (1 = identical meaning). */
    public List<RetrievedChunk> semanticSearch(float[] queryEmbedding, int limit) {
        String sql = SELECT_COLUMNS + """
                     , 1 - (c.embedding <=> CAST(:embedding AS vector)) AS score
                FROM chunk c
                JOIN document d ON d.id = c.document_id
                ORDER BY c.embedding <=> CAST(:embedding AS vector)
                LIMIT :limit
                """;
        return jdbcClient.sql(sql)
                .param("embedding", toVectorLiteral(queryEmbedding))
                .param("limit", limit)
                .query((rs, rowNum) -> toChunk(rs))
                .list();
    }

    /**
     * Full-text search. Words are OR-ed so a long natural question still matches chunks that contain
     * only some of its words; ts_rank puts chunks with more (and rarer) matches first.
     */
    public List<RetrievedChunk> keywordSearch(String query, int limit) {
        String tsQuery = toOrQuery(query);
        if (tsQuery.isEmpty()) {
            return List.of();
        }
        String sql = SELECT_COLUMNS + """
                     , ts_rank(c.tsv, to_tsquery('english', :query)) AS score
                FROM chunk c
                JOIN document d ON d.id = c.document_id
                WHERE c.tsv @@ to_tsquery('english', :query)
                ORDER BY score DESC
                LIMIT :limit
                """;
        return jdbcClient.sql(sql)
                .param("query", tsQuery)
                .param("limit", limit)
                .query((rs, rowNum) -> toChunk(rs))
                .list();
    }

    static String toOrQuery(String query) {
        if (query == null) {
            return "";
        }
        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(word -> word.length() > 1)
                .distinct()
                .collect(Collectors.joining(" | "));
    }

    static String toVectorLiteral(float[] embedding) {
        StringBuilder literal = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(embedding[i]);
        }
        return literal.append(']').toString();
    }

    private static RetrievedChunk toChunk(java.sql.ResultSet rs) throws java.sql.SQLException {
        Integer page = (Integer) rs.getObject("page");
        Citation citation = new Citation(rs.getString("file_name"), page, rs.getString("section"));
        return new RetrievedChunk(rs.getObject("id", UUID.class), rs.getString("text"), citation,
                rs.getDouble("score"));
    }
}
