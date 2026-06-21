package dev.fnukenzo.journalize.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps Gemini's embedContent response:
 * { "embedding": { "values": [0.013, -0.021, ...] } }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbeddingResponse(Embedding embedding) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedding(List<Double> values) {
    }
}
