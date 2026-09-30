package com.frozenheart.backend.modules.ai.constant;

import org.springframework.beans.factory.annotation.Value;

public class Ai {
    @Value("${app.ai-server.model}")
    public static String modelName;
}
