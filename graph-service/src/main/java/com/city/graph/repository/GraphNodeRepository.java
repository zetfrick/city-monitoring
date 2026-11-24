package com.city.graph.repository;

import com.city.graph.entity.GraphNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GraphNodeRepository extends JpaRepository<GraphNode, Long> {
    List<GraphNode> findByObjectIdAndDeletedAtIsNullOrderByEventTimeAscFactIdAsc(String objectId);
    List<GraphNode> findByObjectIdOrderByEventTimeAscFactIdAsc(String objectId);
    List<GraphNode> findByDeletedAtIsNullOrderByObjectIdAscEventTimeAscFactIdAsc();
    Optional<GraphNode> findTopByOrderByFactIdDesc();
}