package com.city.graph.repository;

import com.city.graph.entity.ChainHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChainHistoryRepository extends JpaRepository<ChainHistory, Long> {
    List<ChainHistory> findByObjectIdOrderByChangedAtDesc(String objectId);
    List<ChainHistory> findByFactIdOrderByChangedAtAsc(Long factId);
}