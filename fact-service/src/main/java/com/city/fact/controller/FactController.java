package com.city.fact.controller;

import com.city.fact.entity.Fact;
import com.city.fact.entity.FactChange;
import com.city.fact.notify.GraphNotifier;
import com.city.fact.repository.FactChangeRepository;
import com.city.fact.repository.FactRepository;
import com.city.fact.service.FactBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facts")
@RequiredArgsConstructor
@Tag(name = "Fact Service", description = "Генератор фактов: хранит факты, ведёт журнал изменений, уведомляет Graph Service")
public class FactController {

    private final FactRepository factRepository;
    private final FactChangeRepository changeRepository;
    private final FactBatchService batchService;
    private final GraphNotifier notifier;

    @GetMapping
    @Operation(summary = "Текущие факты",
            description = "Все факты, которые сейчас есть в базе Fact Service (удалённые не показываются).")
    public List<Fact> getFacts() {
        return factRepository.findAll();
    }

    @GetMapping("/changes")
    @Operation(summary = "Журнал изменений",
            description = "Контракт взаимодействия с Graph Service: все добавления и удаления фактов с номером "
                    + "seq больше afterSeq, в порядке возникновения. Graph Service вызывает его после уведомления.")
    public List<FactChange> getChanges(
            @Parameter(description = "Последний уже обработанный номер изменения; 0 — с самого начала")
            @RequestParam(defaultValue = "0") Long afterSeq) {
        return changeRepository.findBySeqGreaterThanOrderBySeqAsc(afterSeq);
    }

    @PostMapping("/batch")
    @Operation(summary = "Добавить порцию фактов",
            description = "Создаёт size случайных фактов одной порцией, пишет их в журнал и уведомляет Graph Service.")
    public List<Fact> addBatch(@Parameter(description = "Размер порции, 1–10")
                               @RequestParam(defaultValue = "3") int size) {
        List<Fact> saved = batchService.addBatch(clamp(size));
        notifier.notifyGraph();
        return saved;
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Удалить порцию случайных фактов",
            description = "Удаляет size случайных существующих фактов одной порцией и уведомляет Graph Service.")
    public Map<String, Object> deleteBatch(@Parameter(description = "Размер порции, 1–10")
                                           @RequestParam(defaultValue = "2") int size) {
        List<Long> ids = batchService.deleteRandomBatch(clamp(size));
        notifier.notifyGraph();
        return Map.of("deletedFactIds", ids);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить конкретный факт",
            description = "Удаляет факт по id (порция из одного факта) и уведомляет Graph Service. 404 — факта нет.")
    public ResponseEntity<Void> deleteFact(@PathVariable Long id) {
        if (!batchService.deleteById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fact not found");
        }
        notifier.notifyGraph();
        return ResponseEntity.noContent().build();
    }

    private int clamp(int size) {
        return Math.max(1, Math.min(10, size));
    }
}
