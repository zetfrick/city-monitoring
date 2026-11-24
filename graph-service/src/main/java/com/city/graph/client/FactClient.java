package com.city.graph.client;

import com.city.graph.dto.FactChangeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FactClient {

    private final RestTemplate restTemplate;

    @Value("${fact.service.url:http://localhost:8081}")
    private String factServiceUrl;

    /** Запрашивает изменения новее afterSeq */
    public List<FactChangeDto> fetchChanges(long afterSeq) {
        var uri = UriComponentsBuilder.fromUriString(factServiceUrl + "/api/facts/changes")
                .queryParam("afterSeq", afterSeq)
                .build()
                .toUri();
        List<FactChangeDto> body = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<FactChangeDto>>() {}
        ).getBody();
        return body == null ? List.of() : body;
    }
}
