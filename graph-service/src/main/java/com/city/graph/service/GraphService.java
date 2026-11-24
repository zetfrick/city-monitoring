package com.city.graph.service;

import com.city.graph.dto.ChainNodeDto;
import com.city.graph.dto.ChainSnapshot;
import com.city.graph.dto.FactChangeDto;
import com.city.graph.dto.GraphLink;
import com.city.graph.dto.GraphSnapshot;
import com.city.graph.entity.ChainHistory;
import com.city.graph.entity.ChangeType;
import com.city.graph.entity.GraphEdge;
import com.city.graph.entity.GraphNode;
import com.city.graph.entity.SyncState;
import com.city.graph.repository.ChainHistoryRepository;
import com.city.graph.repository.GraphEdgeRepository;
import com.city.graph.repository.GraphNodeRepository;
import com.city.graph.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final GraphNodeRepository nodeRepo;
    private final GraphEdgeRepository edgeRepo;
    private final ChainHistoryRepository historyRepo;
    private final SyncStateRepository syncStateRepo;

    /**
     * Применяет порцию изменений по порядку seq и запоминает последний обработанный номер.
     * Всё в одной транзакции: либо граф и lastSeq обновились вместе, либо ничего.
     */
    @Transactional
    public void applyChanges(List<FactChangeDto> changes) {
        long maxSeq = 0;
        for (FactChangeDto change : changes) {
            switch (change.changeType()) {
                case "ADDED" -> addFact(change);
                case "DELETED" -> removeFact(change.factId());
                default -> { /* неизвестный тип пропускаем */ }
            }
            maxSeq = Math.max(maxSeq, change.seq());
        }
        SyncState state = syncStateRepo.findById(SyncState.SINGLE_ID).orElseGet(SyncState::new);
        state.setLastSeq(Math.max(maxSeq, state.getLastSeq()));
        syncStateRepo.save(state);
    }

    /** Добавляет факт в цепочку объекта по времени события, сдвигая номера следующих узлов. */
    private void addFact(FactChangeDto dto) {
        if (nodeRepo.existsById(dto.factId())) {
            return;   // повторная доставка того же изменения безопасна
        }

        GraphNode node = new GraphNode();
        node.setFactId(dto.factId());
        node.setEventTime(dto.eventTime());
        node.setObjectId(dto.objectId());
        node.setObjectType(dto.objectType());
        node.setDescription(dto.description());
        node.setLatitude(dto.latitude());
        node.setLongitude(dto.longitude());
        node.setStatus(dto.status());
        node.setRegisteredAt(LocalDateTime.now());

        // активная цепочка объекта + новый узел, затем сортировка по времени события
        List<GraphNode> chain = new ArrayList<>(
                nodeRepo.findByObjectIdAndDeletedAtIsNullOrderByEventTimeAscFactIdAsc(dto.objectId()));
        chain.add(node);
        chain.sort(Comparator.comparing(GraphNode::getEventTime).thenComparing(GraphNode::getFactId));

        for (int i = 0; i < chain.size(); i++) {
            GraphNode current = chain.get(i);
            int newPosition = i + 1;
            if (Objects.equals(current.getFactId(), dto.factId())) {
                current.setChainPosition(newPosition);
                current.setInitialPosition(newPosition);
                nodeRepo.save(current);
                saveHistory(dto.objectId(), dto.factId(), ChangeType.ADDED, null, newPosition);
            } else if (!Objects.equals(current.getChainPosition(), newPosition)) {
                int oldPosition = current.getChainPosition();
                current.setChainPosition(newPosition);
                nodeRepo.save(current);
                saveHistory(dto.objectId(), current.getFactId(), ChangeType.POSITION_CHANGED, oldPosition, newPosition);
            }
        }
        rebuildEdges(dto.objectId(), chain);
    }

    /** Мягкое удаление: узел остаётся в таблице с deletedAt, цепочка и рёбра пересчитываются. */
    private void removeFact(Long factId) {
        GraphNode node = nodeRepo.findById(factId).orElse(null);
        if (node == null || node.getDeletedAt() != null) {
            return;   // неизвестный или уже удалённый факт
        }
        Integer removedPosition = node.getChainPosition();
        node.setDeletedAt(LocalDateTime.now());
        nodeRepo.save(node);
        saveHistory(node.getObjectId(), factId, ChangeType.REMOVED, removedPosition, null);

        // номера оставшихся узлов «закрывают» образовавшуюся дыру
        List<GraphNode> chain = nodeRepo.findByObjectIdAndDeletedAtIsNullOrderByEventTimeAscFactIdAsc(node.getObjectId());
        for (int i = 0; i < chain.size(); i++) {
            GraphNode current = chain.get(i);
            int newPosition = i + 1;
            if (!Objects.equals(current.getChainPosition(), newPosition)) {
                int oldPosition = current.getChainPosition();
                current.setChainPosition(newPosition);
                nodeRepo.save(current);
                saveHistory(node.getObjectId(), current.getFactId(), ChangeType.POSITION_CHANGED, oldPosition, newPosition);
            }
        }
        rebuildEdges(node.getObjectId(), chain);
    }

    /** Текущее состояние: активные узлы и все существующие рёбра. */
    @Transactional(readOnly = true)
    public GraphSnapshot currentGraph() {
        List<GraphNode> nodes = nodeRepo.findByDeletedAtIsNullOrderByObjectIdAscEventTimeAscFactIdAsc();
        List<GraphLink> edges = edgeRepo.findAll().stream().map(this::toLink).toList();
        return new GraphSnapshot(nodes, edges);
    }

    /** История структурных изменений цепочки объекта, новые записи сверху. */
    @Transactional(readOnly = true)
    public List<ChainHistory> history(String objectId) {
        return historyRepo.findByObjectIdOrderByChangedAtDesc(objectId);
    }

    /** Жизненный цикл факта: регистрация, начальный и текущий номер, сдвиги, удаление. */
    @Transactional(readOnly = true)
    public Map<String, Object> lifecycle(Long factId) {
        GraphNode node = nodeRepo.findById(factId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fact not found"));
        List<ChainHistory> changes = historyRepo.findByFactIdOrderByChangedAtAsc(factId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factId", factId);
        result.put("objectId", node.getObjectId());
        result.put("state", node.getDeletedAt() == null ? "ACTIVE" : "DELETED");
        result.put("registeredAt", node.getRegisteredAt());
        result.put("initialPosition", node.getInitialPosition());
        result.put("currentPosition", node.getDeletedAt() == null ? node.getChainPosition() : null);
        result.put("deletedAt", node.getDeletedAt());
        result.put("changes", changes);
        return result;
    }

    /**
     * Цепочка объекта на дату: что граф «знал» в этот момент. В срез входят факты, которые
     * к дате уже зарегистрированы в графе и ещё не удалены. Номера считаются заново
     * по этому набору, а не берутся из текущих значений.
     */
    @Transactional(readOnly = true)
    public ChainSnapshot chainAt(String objectId, LocalDateTime date) {
        List<GraphNode> active = nodeRepo.findByObjectIdOrderByEventTimeAscFactIdAsc(objectId).stream()
                .filter(n -> !n.getRegisteredAt().isAfter(date))
                .filter(n -> n.getDeletedAt() == null || n.getDeletedAt().isAfter(date))
                .toList();

        List<ChainNodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            GraphNode n = active.get(i);
            nodes.add(new ChainNodeDto(
                    n.getFactId(), n.getObjectId(), n.getObjectType(), n.getDescription(), n.getStatus(),
                    n.getLatitude(), n.getLongitude(), n.getEventTime(), n.getRegisteredAt(), n.getDeletedAt(),
                    i + 1,
                    n.getDeletedAt() == null ? n.getChainPosition() : null));
        }
        List<GraphLink> links = new ArrayList<>();
        for (int i = 1; i < active.size(); i++) {
            links.add(createLink(active.get(i - 1), active.get(i)));
        }
        return new ChainSnapshot(objectId, date, nodes, links);
    }

    /** Пересоздаёт рёбра объекта: каждый узел цепочки соединяется со следующим. */
    private void rebuildEdges(String objectId, List<GraphNode> chain) {
        edgeRepo.deleteByObjectId(objectId);
        List<GraphEdge> edges = new ArrayList<>();
        for (int i = 1; i < chain.size(); i++) {
            GraphLink link = createLink(chain.get(i - 1), chain.get(i));
            GraphEdge edge = new GraphEdge();
            edge.setObjectId(objectId);
            edge.setSourceFactId(link.sourceFactId());
            edge.setTargetFactId(link.targetFactId());
            edge.setDistanceMeters(link.distanceMeters());
            edge.setTimeGapSeconds(link.timeGapSeconds());
            edges.add(edge);
        }
        edgeRepo.saveAll(edges);
    }

    private GraphLink createLink(GraphNode source, GraphNode target) {
        return new GraphLink(
                source.getFactId(),
                target.getFactId(),
                distanceMeters(source, target),
                Math.max(0, Duration.between(source.getEventTime(), target.getEventTime()).getSeconds()));
    }

    /** Расстояние между точками в метрах по формуле гаверсинусов. */
    private Double distanceMeters(GraphNode source, GraphNode target) {
        if (source.getLatitude() == null || source.getLongitude() == null
                || target.getLatitude() == null || target.getLongitude() == null) {
            return null;
        }
        double latitudeDelta = Math.toRadians(target.getLatitude() - source.getLatitude());
        double longitudeDelta = Math.toRadians(target.getLongitude() - source.getLongitude());
        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(source.getLatitude()))
                * Math.cos(Math.toRadians(target.getLatitude()))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        a = Math.min(1, Math.max(0, a));
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private GraphLink toLink(GraphEdge edge) {
        return new GraphLink(edge.getSourceFactId(), edge.getTargetFactId(),
                edge.getDistanceMeters(), edge.getTimeGapSeconds());
    }

    private void saveHistory(String objectId, Long factId, ChangeType type, Integer oldPosition, Integer newPosition) {
        ChainHistory h = new ChainHistory();
        h.setObjectId(objectId);
        h.setFactId(factId);
        h.setChangeType(type);
        h.setOldPosition(oldPosition);
        h.setNewPosition(newPosition);
        h.setChangedAt(LocalDateTime.now());
        historyRepo.save(h);
    }
}
