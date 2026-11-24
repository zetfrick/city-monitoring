package com.city.graph.service;

import com.city.graph.client.FactClient;
import com.city.graph.dto.FactChangeDto;
import com.city.graph.entity.SyncState;
import com.city.graph.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeSyncService {

    private final FactClient factClient;
    private final SyncStateRepository syncStateRepository;
    private final GraphService graphService;

    public synchronized void sync() {
        long lastSeq = syncStateRepository.findById(SyncState.SINGLE_ID)
                .map(SyncState::getLastSeq)
                .orElse(0L);
        List<FactChangeDto> changes = factClient.fetchChanges(lastSeq);
        if (changes.isEmpty()) {
            return;
        }
        graphService.applyChanges(changes);
        log.info("Применено изменений: {} (последний seq = {})", changes.size(),
                changes.get(changes.size() - 1).seq());
    }
}
