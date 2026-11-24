package com.city.graph.dto;

import java.time.LocalDateTime;

/**
 * Узел цепочки в срезе «на дату».
 * positionOnDate — номер в цепочке именно на запрошенную дату;
 * currentPosition — номер сейчас (null, если факт уже удалён).
 */
public record ChainNodeDto(
        Long factId,
        String objectId,
        String objectType,
        String description,
        String status,
        Double latitude,
        Double longitude,
        LocalDateTime eventTime,
        LocalDateTime registeredAt,
        LocalDateTime deletedAt,
        Integer positionOnDate,
        Integer currentPosition
) {}
