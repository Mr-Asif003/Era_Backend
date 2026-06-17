package com.era.backend.era.controller;

import com.era.backend.era.model.EraCommand;
import com.era.backend.era.service.EraService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class EraController {

    private final EraService eraService;

    /**
     * Mobile -> STOMP PUBLISH /app/era.command
     * { command: "Hey Era, what's the weather in Patna?", mode: "text", context: {...} }
     *
     * Streamed chunks are pushed back to the client on /user/queue/era.
     */
    @MessageMapping("era.command")
    public void handleEraCommand(@Payload EraCommand command, Principal principal) {
        eraService.processCommand(principal.getName(), command);
    }
}
