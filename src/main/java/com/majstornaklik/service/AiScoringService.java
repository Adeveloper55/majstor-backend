package com.majstornaklik.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiScoringService {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-3-haiku-20240307}")
    private String model;

    private final ObjectMapper objectMapper;

    public record AiScoreResult(int score, String reason) {}

    public AiScoreResult scoreJob(String categoryName, String description) {
        if (apiKey == null || apiKey.isBlank()) {
            return new AiScoreResult(2, "Podrazumevana ocena (AI nije konfigurisan)");
        }

        String prompt = """
                You are a home repair expert. Based on the following job description and category, rate the complexity of the job on a scale from 1 to 5, where:
                1 = Very simple (under 1 hour, no special tools)
                2 = Simple (1-3 hours, basic tools)
                3 = Moderate (half day, some expertise needed)
                4 = Complex (full day, professional expertise required)
                5 = Very complex (multiple days, specialized expertise)

                Category: %s
                Description: %s

                Respond ONLY with a JSON object in this format:
                {"score": 3, "reason": "brief explanation"}
                """.formatted(categoryName, description);

        try {
            RestClient client = RestClient.create();
            String response = client.post()
                    .uri("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "max_tokens", 256,
                            "messages", new Object[]{
                                    Map.of("role", "user", "content", prompt)
                            }
                    ))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String text = root.path("content").get(0).path("text").asText();
            JsonNode parsed = objectMapper.readTree(text.trim());
            int score = Math.max(1, Math.min(5, parsed.path("score").asInt(2)));
            String reason = parsed.path("reason").asText("Ocena složenosti posla");
            return new AiScoreResult(score, reason);
        } catch (Exception e) {
            log.warn("AI scoring failed, using fallback: {}", e.getMessage());
            return new AiScoreResult(2, "Podrazumevana ocena (AI greška)");
        }
    }
}
