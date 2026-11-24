package com.city.fact.service;

import com.city.fact.entity.ChangeType;
import com.city.fact.entity.Fact;
import com.city.fact.entity.FactChange;
import com.city.fact.repository.FactChangeRepository;
import com.city.fact.repository.FactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactBatchService {

    private final FactRepository factRepository;
    private final FactChangeRepository changeRepository;
    private final Random random = new Random();

    private static final String[] OBJECT_IDS = {"SENSOR-001", "SENSOR-002", "TRAFFIC-01", "PARK-05", "LIGHT-12"};
    private static final String[] OBJECT_TYPES = {"SENSOR", "INCIDENT", "STATUS"};
    private static final String[] DESCRIPTIONS = {
            "Показатель PM2.5 превышен", "Дорожная пробка", "Освещение работает нормально",
            "Обнаружено ДТП", "Уровень шума в норме"
    };
    private static final String[] STATUSES = {"NORMAL", "WARNING", "CRITICAL"};

    /** Создаёт порцию случайных фактов. Время события — случайно в последние 60 минут. */
    @Transactional
    public List<Fact> addBatch(int size) {
        List<Fact> facts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Fact fact = new Fact();
            fact.setEventTime(LocalDateTime.now().minusMinutes(random.nextInt(60)));
            fact.setObjectId(OBJECT_IDS[random.nextInt(OBJECT_IDS.length)]);
            fact.setObjectType(OBJECT_TYPES[random.nextInt(OBJECT_TYPES.length)]);
            fact.setDescription(DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)]);
            fact.setLatitude(55.75 + random.nextDouble() * 0.1);
            fact.setLongitude(37.61 + random.nextDouble() * 0.1);
            fact.setStatus(STATUSES[random.nextInt(STATUSES.length)]);
            facts.add(fact);
        }
        // saveAll присваивает id; без него в журнал нечего писать
        List<Fact> saved = factRepository.saveAll(facts);
        saved.forEach(f -> writeLog(ChangeType.ADDED, f));
        log.info("Добавлена порция из {} фактов", saved.size());
        return saved;
    }

    /** Удаляет порцию случайных существующих фактов. Возвращает id удалённых. */
    @Transactional
    public List<Long> deleteRandomBatch(int size) {
        List<Fact> all = new ArrayList<>(factRepository.findAll());
        Collections.shuffle(all, random);
        return deleteFacts(new ArrayList<>(all.subList(0, Math.min(size, all.size()))));
    }

    /** Удаляет один факт по id. false — такого факта нет. */
    @Transactional
    public boolean deleteById(Long id) {
        return factRepository.findById(id)
                .map(f -> !deleteFacts(List.of(f)).isEmpty())
                .orElse(false);
    }

    private List<Long> deleteFacts(List<Fact> victims) {
        List<Long> ids = new ArrayList<>();
        for (Fact f : victims) {
            writeLog(ChangeType.DELETED, f);   // снимок пишем до физического удаления
            factRepository.delete(f);
            ids.add(f.getId());
        }
        log.info("Удалена порция из {} фактов: {}", ids.size(), ids);
        return ids;
    }

    private void writeLog(ChangeType type, Fact f) {
        FactChange c = new FactChange();
        c.setChangeType(type);
        c.setFactId(f.getId());
        c.setChangedAt(LocalDateTime.now());
        c.setEventTime(f.getEventTime());
        c.setObjectId(f.getObjectId());
        c.setObjectType(f.getObjectType());
        c.setDescription(f.getDescription());
        c.setLatitude(f.getLatitude());
        c.setLongitude(f.getLongitude());
        c.setStatus(f.getStatus());
        changeRepository.save(c);
    }
}
