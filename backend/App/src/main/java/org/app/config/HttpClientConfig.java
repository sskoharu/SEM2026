package org.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Tiempo de espera de conexión: 5 segundos
        factory.setConnectTimeout(5000);
        // Tiempo de espera de lectura: 5 segundos
        factory.setReadTimeout(5000);

        return RestClient.builder().requestFactory(factory);
    }
}
