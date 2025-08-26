package me.jungtaemin.tfttiertracker.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient riotWebClient(@Value("${riot.api-key}") String apiKey) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS));
                });

        ExchangeFilterFunction auth = ExchangeFilterFunction.ofRequestProcessor(req ->
                Mono.just(ClientRequest.from(req)
                        .header("X-Riot-Token", apiKey)
                        .build()));

        ExchangeFilterFunction errorMap = ExchangeFilterFunction.ofResponseProcessor(resp -> {
            if (resp.statusCode().is2xxSuccessful()) return Mono.just(resp);
            return resp.bodyToMono(String.class).defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new WebClientResponseException(
                            "Riot API error: " + resp.statusCode() + " " + body,
                            resp.statusCode().value(),
                            null, // getReasonPhrase() 대신 null 또는 statusCode.toString() 사용
                            resp.headers().asHttpHeaders(),
                            null,
                            null
                    )));
        });


        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(auth)
                .filter(errorMap)
                .build();
    }
}