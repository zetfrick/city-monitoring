package com.city.graph.repository;

import com.city.graph.entity.SyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStateRepository extends JpaRepository<SyncState, Long> {
}
