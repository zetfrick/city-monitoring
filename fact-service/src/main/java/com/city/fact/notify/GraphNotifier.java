package com.city.fact.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphNotifier {

    private final RestTemplate restTemplate;

    @Value("${graph.service.url:http://localhost:8082}")
    private String graphServiceUrl;

    public void notifyGraph() {
        try {
            restTemplate.postForEntity(graphServiceUrl + "/api/graph/notify", null, Void.class);
            log.info("Graph Service уведомлён об изменении");
        } catch (Exception e) {
            // обработка сбоев по условию задания не рассматривается
            log.error("Ошибка уведомления Graph Service: {}", e.getMessage());
        }
    }
}
