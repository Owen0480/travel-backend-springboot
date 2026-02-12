package com.travel.planner.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    /** FastAPI(LangGraph) 일정 생성 등 장시간 호출용 타임아웃: 20분 */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(20);
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_WRITE_TIMEOUT_SEC = (int) RESPONSE_TIMEOUT.getSeconds();

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(RESPONSE_TIMEOUT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(READ_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
