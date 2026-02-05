package com.fluxpay.engine.infrastructure.external.pg;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class TossPaymentsConfig {

    @Bean
    public WebClient tossPaymentsWebClient(TossPaymentsProperties properties) {
        // Basic Auth: secretKey with colon suffix, Base64 encoded
        String credentials = properties.secretKey() + ":";
        String encodedCredentials = Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                (int) properties.connectTimeout().toMillis())
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(
                    properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(
                    properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
            .baseUrl(properties.apiUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Bean
    public TossPaymentsClient tossPaymentsClient(WebClient tossPaymentsWebClient) {
        return new TossPaymentsClient(tossPaymentsWebClient);
    }
}
