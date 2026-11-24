package com.city.graph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "graph_edges")
@Data
public class GraphEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String objectId;

    @Column(name = "source_fact_id", nullable = false)
    private Long sourceFactId;

    @Column(name = "target_fact_id", nullable = false)
    private Long targetFactId;

    private Double distanceMeters;
    private Long timeGapSeconds;
}
