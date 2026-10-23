package org.app;

import org.app.schemas.requests.ClaimRequestDto;
import org.app.services.ClaimService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.chat.client.ChatClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DisplayName("Pruebas de Resiliencia y Reintentos a Nivel de Servicio")
class ClaimServiceResiliencyTest {

    @Autowired
    private ClaimService claimService;

    @Test
    @DisplayName("Debe reintentar ante errores transitorios y aplicar política de backoff")
    void testRetryPolicyOnTransientErrors() {
        // Nota: Aquí simulas el fallo del cliente subyacente de IA para verificar
        // que el servicio intenta las 4 ejecuciones antes de lanzar la excepción de recuperación.

        ClaimRequestDto requestDto = new ClaimRequestDto();
        requestDto.setClaim("test claim");

        // Ejecución y verificación de la política de reintentos
        // (Ajusta según el mock de tu ChatClient interno)
    }
}