package com.city.graph.dto;

public record GraphLink(
        Long sourceFactId,
        Long targetFactId,
        Double distanceMeters,
        Long timeGapSeconds
) {}
