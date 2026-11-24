package com.city.graph.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chain_history")
@Data
public class ChainHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String objectId;
    private Long factId;

    @Enumerated(EnumType.STRING)
    private ChangeType changeType;

    private Integer oldPosition;
    private Integer newPosition;

    private LocalDateTime changedAt;
}