package com.era.backend.era.service;

import com.era.backend.era.model.EraCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Bridges the mobile client's "Hey Era" voice/text commands (received over
 * STOMP) to the FastAPI AI service's SSE streaming endpoint, then relays
 * each chunk (tool_call / text / done) back to the client over the
 * /user/queue/era WebSocket queue. See architecture doc section 7.
 */
@Service
@Slf4j
public class EraService {

    private final WebClient webClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EraService(@Value("${fastapi.url}") String fastapiUrl,
                       SimpMessagingTemplate messagingTemplate) {
        this.webClient = WebClient.builder().baseUrl(fastapiUrl).build();
        this.messagingTemplate = messagingTemplate;
    }

    public void processCommand(String userId, EraCommand command) {
        webClient.post()
                .uri("/era/command/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "command", command.getCommand(),
                        "user_id", userId,
                        "context", command.getContext() == null ? Map.of() : command.getContext()
                ))
                .retrieve()
                .bodyToFlux(String.class) // SSE stream, one "data: {...}" line per chunk
                .subscribe(
                        rawChunk -> messagingTemplate.convertAndSendToUser(
                                userId, "/queue/era", parseChunk(rawChunk)),
                        error -> {
                            log.error("Era SSE stream failed for user {}", userId, error);
                            messagingTemplate.convertAndSendToUser(
                                    userId, "/queue/era", Map.of("type", "error", "message", "Era is unavailable right now"));
                        }
                );
    }

    private Object parseChunk(String rawChunk) {
        try {
            String payload = rawChunk.startsWith("data:") ? rawChunk.substring(5).trim() : rawChunk.trim();
            if ("[DONE]".equals(payload)) {
                return Map.of("type", "done");
            }
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.warn("Could not parse Era SSE chunk: {}", rawChunk, e);
            return Map.of("type", "error", "message", "Malformed response chunk");
        }
    }
}
