package dev.fnukenzo.journalize.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Turns text into an embedding vector via Gemini, for semantic search.
 *
 * An embedding is a fixed-length list of numbers that captures the *meaning* of
 * text — two pieces of text with similar meaning produce vectors that point in a
 * similar direction (high cosine similarity), even if they share no words.
 *
 * Fail-soft: returns {@code null} on any error so journaling never breaks.
 */
@Slf4j
@Service
public class EmbeddingService {

    private final RestClient restClient = RestClient.create();

    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public EmbeddingService(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.embedding-model:text-embedding-004}") String model,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}") String apiUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key not configured; skipping embedding");
            return null;
        }
        try {
            Map<String, Object> body = Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text))));

            String uri = apiUrl + "/" + model + ":embedContent?key=" + apiKey;

            EmbeddingResponse response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.embedding() == null || response.embedding().values() == null) {
                return null;
            }
            List<Double> values = response.embedding().values();
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return vector;

        } catch (Exception e) {
            log.warn("Embedding failed: {}", e.getMessage());
            return null;
        }
    }

    /** Serialize a vector to a compact comma-separated string for DB storage. */
    public static String serialize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    /** Parse a stored comma-separated string back into a vector. */
    public static float[] parse(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String[] parts = stored.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
