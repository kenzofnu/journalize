package dev.fnukenzo.journalize.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Calls Google's Gemini API to detect the dominant mood of a journal entry.
 *
 * Designed to fail soft: if the API key is missing, the network call fails, or the
 * response can't be parsed, this returns {@code null} instead of throwing. Saving a
 * journal entry must never break just because mood detection is unavailable.
 */
@Slf4j
@Service
public class MoodService {

    private final RestClient restClient = RestClient.create();

    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public MoodService(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}") String apiUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    public String detectMood(String content) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key not configured; skipping mood detection");
            return null;
        }

        try {
            String prompt = """
                    Analyze the overall mood of the following journal entry.
                    Respond with exactly ONE lowercase word naming the dominant emotion
                    (for example: happy, sad, anxious, calm, angry, excited, grateful, stressed, content, hopeful).
                    Do not add punctuation or any explanation.

                    Journal entry:
                    """ + content;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)))));

            String uri = apiUrl + "/" + model + ":generateContent?key=" + apiKey;

            GeminiResponse response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            String text = extractText(response);
            if (text == null) {
                return null;
            }

            // Normalise to a single lowercase word of letters only
            String mood = text.trim().toLowerCase().split("\\s+")[0].replaceAll("[^a-z]", "");
            return mood.isBlank() ? null : mood;

        } catch (Exception e) {
            log.warn("Mood detection failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        GeminiResponse.Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().get(0).text();
    }
}
