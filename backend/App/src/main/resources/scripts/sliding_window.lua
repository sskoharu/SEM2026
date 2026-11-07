-- KEYS[1] = ventana actual, KEYS[2] = ventana anterior
-- ARGV[1] = limite, ARGV[2] = ventana (s), ARGV[3] = now (ms)
-- Devuelve {permitido(1/0), uso estimado, retryAfter (ms)}
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2]) * 1000
local now = tonumber(ARGV[3])

local curr = tonumber(redis.call('GET', KEYS[1]) or '0')
local prev = tonumber(redis.call('GET', KEYS[2]) or '0')

local elapsed = now % window
local estimate = prev * ((window - elapsed) / window) + curr

if estimate + 1 > limit then
    return {0, math.floor(estimate), window - elapsed}
end

if redis.call('INCR', KEYS[1]) == 1 then
    redis.call('PEXPIRE', KEYS[1], window * 2)
end
return {1, math.floor(estimate) + 1, 0}
