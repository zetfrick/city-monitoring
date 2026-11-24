package com.city.graph.repository;

import com.city.graph.entity.GraphEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GraphEdgeRepository extends JpaRepository<GraphEdge, Long> {
    List<GraphEdge> findByObjectIdOrderBySourceFactIdAsc(String objectId);
    void deleteByObjectId(String objectId);
}
