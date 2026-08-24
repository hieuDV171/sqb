package com.frozenheart.backend.core.constant;

import org.springframework.beans.factory.annotation.Value;

public class Ai {
    @Value("${app.ai-server.model}")
    public static String modelName;
}
