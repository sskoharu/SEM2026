package org.app.security;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService ragUserDetailsService(
            @Value("${app.security.rag.username:}") String configuredUsername,
            @Value("${app.security.rag.password:}") String configuredPassword,
            PasswordEncoder passwordEncoder) {

        boolean credentialsConfigured = configuredUsername != null
                && !configuredUsername.isBlank()
                && configuredPassword != null
                && !configuredPassword.isBlank();

        String username = credentialsConfigured
                ? configuredUsername.trim()
                : "rag-disabled";
        String rawPassword = credentialsConfigured
                ? configuredPassword
                : UUID.randomUUID().toString();

        UserDetails ragUser = User.withUsername(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles("RAG_USER")
                .build();

        return new InMemoryUserDetailsManager(ragUser);
    }
}
