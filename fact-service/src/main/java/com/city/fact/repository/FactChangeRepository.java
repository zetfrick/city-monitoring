package com.city.fact.repository;

import com.city.fact.entity.FactChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FactChangeRepository extends JpaRepository<FactChange, Long> {
    /** Все изменения новее указанного номера, в порядке их возникновения. */
    List<FactChange> findBySeqGreaterThanOrderBySeqAsc(Long seq);
}
