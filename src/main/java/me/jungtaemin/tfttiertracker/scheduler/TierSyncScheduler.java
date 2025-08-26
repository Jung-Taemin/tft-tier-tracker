package me.jungtaemin.tfttiertracker.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class TierSyncScheduler {

    @Value("${scheduler.interval-ms:300000}")
    private long interval;

    // 컨트롤러의 /api/tier/refresh를 바로 호출해도 되지만,
    // 실제로는 서비스 빈을 주입해 직접 호출하는 게 정석.
    private final RestClient rest = RestClient.create("http://localhost:8080");

    @PostConstruct
    void init() { /* 로그만 남겨도 OK */ }

    @Scheduled(fixedDelayString = "${scheduler.interval-ms:300000}")
    public void sync() {
        try {
            rest.post().uri("/api/tier/refresh").retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            // 로그만 남기고 조용히 패스 (과도한 에러로 비용/장애 유발 방지)
        }
    }
}