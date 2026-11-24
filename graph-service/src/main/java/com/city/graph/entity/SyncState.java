package com.city.graph.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Table(name = "sync_state")
@Data
public class SyncState {
    public static final long SINGLE_ID = 1L;

    @Id
    private Long id = SINGLE_ID;

    private Long lastSeq = 0L;
}
