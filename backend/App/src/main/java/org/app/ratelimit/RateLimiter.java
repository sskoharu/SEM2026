package org.app.ratelimit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateLimiter {

    public record Result(boolean allowed, long used, long retryAfterSeconds) {}

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("scripts/sliding_window.lua"));
        this.script.setResultType(List.class);
    }

    public Result tryAcquire(String bucket, String clientId, int limit, int windowSeconds) {
        return tryAcquire(bucket, clientId, limit, windowSeconds, System.currentTimeMillis());
    }

    Result tryAcquire(String bucket, String clientId, int limit, int windowSeconds, long nowMs) {
        long windowMs = windowSeconds * 1000L;
        long index = nowMs / windowMs;
        String base = "rl:" + bucket + ":" + clientId + ":";

        List<?> res = redis.execute(script,
                List.of(base + index, base + (index - 1)),
                String.valueOf(limit), String.valueOf(windowSeconds), String.valueOf(nowMs));

        if (res == null || res.size() < 3) {
            throw new IllegalStateException("Respuesta inesperada del script de rate limit");
        }
        long retryMs = ((Number) res.get(2)).longValue();
        return new Result(((Number) res.get(0)).longValue() == 1,
                ((Number) res.get(1)).longValue(),
                (retryMs + 999) / 1000);
    }
}
