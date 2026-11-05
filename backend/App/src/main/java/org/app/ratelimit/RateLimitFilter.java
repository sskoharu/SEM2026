package org.app.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.app.exceptions.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Log4j2
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final RateLimitProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RateLimitFilter(RateLimiter limiter, RateLimitProperties props) {
        this.limiter = limiter;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.enabled() || "OPTIONS".equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return props.excludedPaths().stream().anyMatch(p -> matcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean llm = isLlmRequest(request);
        String bucket = llm ? "llm" : "general";
        int limit = llm ? props.llmLimit() : props.generalLimit();

        RateLimiter.Result result;
        try {
            result = limiter.tryAcquire(bucket, request.getRemoteAddr(), limit, props.windowSeconds());
        } catch (Exception e) {
            log.error("Rate limiter no disponible, se rechaza la solicitud (fail-closed)", e);
            write(response, HttpStatus.SERVICE_UNAVAILABLE, "Servicio temporalmente no disponible.", props.windowSeconds());
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - result.used())));

        if (!result.allowed()) {
            log.warn("Rate limit excedido: bucket={}, ip={}", bucket, request.getRemoteAddr());
            write(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas solicitudes. Intenta de nuevo en unos segundos.", result.retryAfterSeconds());
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isLlmRequest(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return props.llmPaths().stream().anyMatch(p -> matcher.match(p, path));
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, long retryAfter)
            throws IOException {
        response.setStatus(status.value());
        response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ErrorResponse.jsonOf(message, status));
    }
}
