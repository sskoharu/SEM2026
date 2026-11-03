package org.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.app.repositories.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthorizationFilter extends OncePerRequestFilter {
    private static final Pattern SESSION_PATH = Pattern.compile(
            "^/api/v1/claims/(?:(?:evaluate|accept|batch-update|select-action|submit-procedural-form)/)?([a-fA-F0-9-]{36})(?:/.*)?$");
    private final Object[] locks = java.util.stream.IntStream.range(0, 64).mapToObj(index -> new Object()).toArray();
    private final SessionRepository sessions;

    public SessionAuthorizationFilter(SessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/claims/") || "OPTIONS".equals(request.getMethod())
                || (uri.equals("/api/v1/claims/start") && "POST".equals(request.getMethod()))) {
            chain.doFilter(request, response);
            return;
        }
        var match = SESSION_PATH.matcher(uri);
        if (!match.matches()) {
            deny(response);
            return;
        }
        String sessionId = match.group(1);
        synchronized (locks[Math.floorMod(sessionId.hashCode(), locks.length)]) {
            if (!SessionAccess.permits(
                    sessions.findById(sessionId).orElse(null),
                    request.getHeader(SessionAccess.HEADER))) {
                deny(response);
                return;
            }
            response.setHeader("Cache-Control", "no-store");
            chain.doFilter(request, response);
        }
    }

    private void deny(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Acceso a la sesión no autorizado.\"}");
    }
}
