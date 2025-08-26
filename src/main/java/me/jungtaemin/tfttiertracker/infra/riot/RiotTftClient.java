package me.jungtaemin.tfttiertracker.infra.riot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiotTftClient {

    private final WebClient riotWebClient;

    @Value("${riot.platform:kr}")
    private String platform;

    private String baseUrl() {
        return "https://" + platform + ".api.riotgames.com";
    }

    public Mono<Map<String, Object>> getSummonerByName(String summonerName) {
        String url = baseUrl() + "/tft/summoner/v1/summoners/by-name/{name}";
        return riotWebClient.get()
                .uri(url, summonerName)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<List<Map<String, Object>>> getLeagueBySummonerId(String encryptedSummonerId) {
        String url = baseUrl() + "/tft/league/v1/entries/by-summoner/{id}";
        return riotWebClient.get()
                .uri(url, encryptedSummonerId)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList();
    }
}
