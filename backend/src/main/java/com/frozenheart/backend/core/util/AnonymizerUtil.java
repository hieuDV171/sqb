package com.frozenheart.backend.core.util;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnonymizerUtil {

    private final Hashids hashids;

    public AnonymizerUtil(@Value("${app.salt}") String salt) {
        this.hashids = new Hashids(salt, 6);
    }

    public String encodeUserId(Long userId) {
        if (userId == null)
            return "000000";
        return hashids.encode(userId);
    }
}
