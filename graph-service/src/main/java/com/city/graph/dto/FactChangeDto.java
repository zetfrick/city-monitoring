package com.city.graph.dto;

import java.time.LocalDateTime;

public record FactChangeDto(
        Long seq,
        String changeType,
        Long factId,
        LocalDateTime changedAt,
        LocalDateTime eventTime,
        String objectId,
        String objectType,
        String description,
        Double latitude,
        Double longitude,
        String status
) {}
