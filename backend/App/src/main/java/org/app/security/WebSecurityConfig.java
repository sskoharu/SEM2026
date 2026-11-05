package org.app.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.app.ratelimit.RateLimitFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Log4j2
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RateLimitFilter rateLimitFilter) throws Exception {

        log.info("Configurando seguridad de la aplicación");
        http.addFilterBefore(rateLimitFilter, BasicAuthenticationFilter.class);
        http.headers(head -> head.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        log.info("Configurando CSRF");
        http.csrf(CsrfConfigurer::disable);

        log.info("Configurando CORS");
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        log.info("Configurando autorización de solicitudes HTTP");
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        HttpMethod.POST, "/api/v1/claims/**"
                ).permitAll()
                .requestMatchers(
                        HttpMethod.GET, "/api/v1/claims/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
        );

        log.info("Configurando autenticación");
        http.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        log.info("Configurando filtro de autenticación básica");
        http.httpBasic(basic -> basic.authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage())));

        return http.build();
    }

    public CorsConfigurationSource corsConfigurationSource() {

        var source = new UrlBasedCorsConfigurationSource();
        var configuration = new CorsConfiguration();

        log.info("Configurando origen cruzado para desarrollo");
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
