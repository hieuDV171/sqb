package com.frozenheart.backend.modules.session.dto;

import java.util.List;

public record AssignStudentsRequest(
        List<String> studentCodes,
        List<Long> studentUserIds
) {}
