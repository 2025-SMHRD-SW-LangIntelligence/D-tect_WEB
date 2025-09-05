//package com.smhrd.dtect.config;
//
//import io.netty.channel.ChannelOption;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.reactive.ReactorClientHttpConnector;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.netty.http.client.HttpClient;
//
//import java.time.Duration;
//
//@Configuration
//@EnableConfigurationProperties(ModelProperties.class)
//@RequiredArgsConstructor
//public class ModelClientConfig {
//
//    private final ModelProperties props;
//
//    @Bean
//    public WebClient modelWebClient() {
//        HttpClient http = HttpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
//                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
//        return WebClient.builder()
//                .baseUrl(props.getBaseUrl())
//                .clientConnector(new ReactorClientHttpConnector(http))
//                .build();
//    }
//}
