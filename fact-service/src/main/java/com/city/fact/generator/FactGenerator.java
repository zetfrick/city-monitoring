package com.city.fact.generator;

import com.city.fact.notify.GraphNotifier;
import com.city.fact.repository.FactRepository;
import com.city.fact.service.FactBatchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;

/**
 * Фоновый генератор. Раз в 10–20 секунд (случайный интервал) выполняет одну порцию:
 * чаще — добавление 1–3 фактов, иногда — удаление 1–2 случайных фактов.
 * После каждой порции синхронно уведомляет Graph Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FactGenerator {

    private final FactBatchService batchService;
    private final FactRepository factRepository;
    private final GraphNotifier notifier;
    private final TaskScheduler taskScheduler;
    private final Random random = new Random();

    /** Удаляем только когда в базе накопилось достаточно фактов. */
    private static final long MIN_FACTS_FOR_DELETE = 15;

    @PostConstruct
    public void start() {
        taskScheduler.schedule(this::runAndScheduleNext, Instant.now());
    }

    private void runAndScheduleNext() {
        try {
            runBatch();
        } finally {
            // следующий запуск планируем после завершения текущего: интервал случайный
            long delayMillis = 10_000L + random.nextInt(10_001);
            taskScheduler.schedule(this::runAndScheduleNext, Instant.now().plusMillis(delayMillis));
        }
    }

    public void runBatch() {
        try {
            boolean doDelete = factRepository.count() >= MIN_FACTS_FOR_DELETE && random.nextInt(100) < 25;
            if (doDelete) {
                batchService.deleteRandomBatch(random.nextInt(2) + 1);
            } else {
                batchService.addBatch(random.nextInt(3) + 1);
            }
            notifier.notifyGraph();
        } catch (Exception e) {
            // сбой одной порции не должен останавливать генератор
            log.error("Сбой порции: {}", e.getMessage(), e);
        }
    }
}
