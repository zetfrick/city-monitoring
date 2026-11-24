package com.city.graph.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "graph_nodes")
@Data
public class GraphNode {
    @Id
    private Long factId;

    private LocalDateTime eventTime;
    private String objectId;
    private String objectType;
    private String description;
    private Double latitude;
    private Double longitude;
    private String status;

    @Column(name = "chain_position")
    private Integer chainPosition;

    @Column(name = "initial_position")
    private Integer initialPosition;

    private LocalDateTime registeredAt;
    private LocalDateTime deletedAt;
}