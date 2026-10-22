package org.app.config;

import jakarta.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.*;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableResilientMethods
@Log4j2
public class ResilienceConfig implements RetryListener {

    // 1. Se ejecuta JUSTO ANTES de realizar un nuevo intento tras un fallo previo
    public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
        log.warn("⚠️ Preparando reintento. Intento actual: {}/{} para la operación.",
                retryState.getRetryCount());
    }

    // 2. Se ejecuta cuando la operación FALLA en uno de los intentos
    public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
        log.error("❌ Fallo en la ejecución del método. Causa: {}", throwable.getMessage());
    }

    // 3. Se ejecuta cuando la operación es EXITOSA en alguno de los reintentos
    public void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Object result) {
        log.info("✅ Operación completada con éxito tras el reintento.");
    }

    // aqui quiero agregar logica que retorne al usuario un mensaje de error que diga "Se alcanzaron los intentos máximos, por favor intente más tarde" y que detenga la ejecución del método
    // 4. Se ejecuta cuando se AGOTAN todos los intentos permitidos (Max Retries alcanzado)
    public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
        log.error("🚨 Política de reintentos AGOTADA. Se alcanzaron los {} intentos máximos. Deteniendo ejecuciones.");
    }
}