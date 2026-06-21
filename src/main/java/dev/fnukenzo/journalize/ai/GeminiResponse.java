package dev.fnukenzo.journalize.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal mapping of the Gemini generateContent JSON response. We only model the
 * fields we actually read (the generated text) and ignore everything else.
 *
 * Example response shape:
 * {
 *   "candidates": [
 *     { "content": { "parts": [ { "text": "happy" } ] } }
 *   ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(List<Candidate> candidates) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {
    }
}
