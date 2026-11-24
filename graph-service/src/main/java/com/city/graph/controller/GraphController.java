package com.city.graph.controller;

import com.city.graph.dto.ChainSnapshot;
import com.city.graph.dto.GraphSnapshot;
import com.city.graph.entity.ChainHistory;
import com.city.graph.service.ChangeSyncService;
import com.city.graph.service.GraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
@Tag(name = "Graph Service", description = "Построитель графа: цепочки фактов по объектам, история изменений, запросы к графу")
public class GraphController {

    private final GraphService graphService;
    private final ChangeSyncService syncService;

    @PostMapping("/notify")
    @Operation(summary = "Уведомление от Fact Service",
            description = "Служебный эндпоинт. Fact Service вызывает его после каждого изменения своей БД. "
                    + "Graph Service запрашивает журнал изменений (GET /api/facts/changes) и обновляет граф. "
                    + "Ответ 204.")
    public ResponseEntity<Void> receiveNotification() {
        syncService.sync();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/current")
    @Operation(summary = "Текущее состояние графа",
            description = "Все активные (не удалённые) узлы и рёбра графа на данный момент.")
    public GraphSnapshot getCurrentGraph() {
        return graphService.currentGraph();
    }

    @GetMapping("/history/{objectId}")
    @Operation(summary = "История структурных изменений цепочки объекта",
            description = "Журнал событий ADDED, POSITION_CHANGED и REMOVED по объекту, новые записи сверху.")
    public List<ChainHistory> getHistory(
            @Parameter(description = "Идентификатор объекта, например SENSOR-001") @PathVariable String objectId) {
        return graphService.history(objectId);
    }

    @GetMapping("/lifecycle/{factId}")
    @Operation(summary = "Жизненный цикл факта (запрос 1 по заданию)",
            description = "Когда факт зарегистрирован и с каким номером в цепочке, когда менялся номер, "
                    + "когда удалён. Если факта нет — 404.")
    public Map<String, Object> getLifecycle(
            @Parameter(description = "Идентификатор факта") @PathVariable Long factId) {
        return graphService.lifecycle(factId);
    }

    @GetMapping("/chain/{objectId}")
    @Operation(summary = "Актуальная цепочка объекта на дату (запрос 2 по заданию)",
            description = "Цепочка фактов объекта так, как её знал граф в указанный момент: факты, уже "
                    + "зарегистрированные и ещё не удалённые к этой дате. Номера (positionOnDate) считаются "
                    + "заново на эту дату. Формат даты: 2026-11-23T18:03:41 (время UTC).")
    public ChainSnapshot getChainAtDate(
            @Parameter(description = "Идентификатор объекта, например SENSOR-001") @PathVariable String objectId,
            @Parameter(description = "Дата и время среза, ISO-8601 без часового пояса", example = "2026-11-23T18:03:41")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return graphService.chainAt(objectId, date);
    }
}
