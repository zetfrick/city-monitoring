package com.city.graph.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Цепочка одного объекта на указанную дату: узлы по порядку и рёбра между соседними. */
public record ChainSnapshot(
        String objectId,
        LocalDateTime date,
        List<ChainNodeDto> nodes,
        List<GraphLink> edges
) {}
