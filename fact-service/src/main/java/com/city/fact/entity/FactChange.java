package com.city.fact.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "fact_changes")
@Data
public class FactChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Enumerated(EnumType.STRING)
    private ChangeType changeType;

    private Long factId;
    private LocalDateTime changedAt;

    // снимок атрибутов факта на момент изменения
    private LocalDateTime eventTime;
    private String objectId;
    private String objectType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double latitude;
    private Double longitude;
    private String status;
}
