package com.era.backend.era.model;

import lombok.Data;

import java.util.Map;

@Data
public class EraCommand {
    private String command;
    private String mode;            // "text" | "voice"
    private Map<String, Object> context;
}
