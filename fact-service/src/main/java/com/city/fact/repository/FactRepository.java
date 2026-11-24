package com.city.fact.repository;

import com.city.fact.entity.Fact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FactRepository extends JpaRepository<Fact, Long> {
    List<Fact> findByIdGreaterThanOrderByIdAsc(Long id);
}