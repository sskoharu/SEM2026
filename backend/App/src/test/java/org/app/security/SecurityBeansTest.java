package org.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityBeansTest {

    private final SecurityBeans securityBeans = new SecurityBeans();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void configuredRagCredentialsAreStoredAsBcryptHash() {
        var userDetailsService = securityBeans.ragUserDetailsService(
                "rag-evaluator",
                "Clave-Segura-2026",
                passwordEncoder);

        UserDetails user = userDetailsService.loadUserByUsername("rag-evaluator");

        assertTrue(passwordEncoder.matches("Clave-Segura-2026", user.getPassword()));
        assertTrue(user.getPassword().startsWith("$2a$")
                || user.getPassword().startsWith("$2b$")
                || user.getPassword().startsWith("$2y$"));
        assertTrue(user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RAG_USER")));
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("otro-usuario"));
    }

    @Test
    void missingCredentialsCreateAnUnusableFallbackIdentity() {
        var userDetailsService = securityBeans.ragUserDetailsService(
                "",
                "",
                passwordEncoder);

        UserDetails fallbackUser = userDetailsService.loadUserByUsername("rag-disabled");

        assertNotNull(fallbackUser);
        assertFalse(passwordEncoder.matches("", fallbackUser.getPassword()));
        assertTrue(fallbackUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RAG_USER")));
    }
}
