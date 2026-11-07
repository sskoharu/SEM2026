package org.app.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test corto contra la URL desplegada (3 llamadas, 1 con LLM):
 *   .\mvnw.cmd test -Dtest=SmokeTest -De2e=true [-De2e.url=https://otra-url]
 */
@EnabledIfSystemProperty(named = "e2e", matches = "true")
class SmokeTest {

    private static final String BASE_URL = System.getProperty("e2e.url", "https://asistente-inteligente-4kiq.onrender.com");
    private static final String VALID_CLAIM = "Compré un celular nuevo en un local y a la semana dejó de encender. "
            + "Tengo la factura y el local se niega a cambiarlo o repararlo.";
    private static final String OUT_OF_SCOPE = "Quiero una receta de encebollado y consejos para viajar a Cuenca.";

    private final RestTemplate rest = buildRestTemplate();

    @Test
    void healthResponde() {
        ResponseEntity<String> r = rest.getForEntity(BASE_URL + "/health", String.class);
        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getBody() != null && r.getBody().contains("\"UP\""), "cuerpo: " + r.getBody());
    }

    @Test
    void relatoValidoLlegaARecuperacionConEvidencia() {
        ResponseEntity<String> r = start(VALID_CLAIM);
        String body = r.getBody() == null ? "" : r.getBody();
        assertEquals(200, r.getStatusCode().value(), body);
        assertTrue(body.contains("\"layer1State\":\"DONE\""), body);
        assertTrue(body.contains("\"ragEvidence\":[{"), "sin evidencia RAG: " + body);
        assertTrue(body.contains("\"estadoRevision\":\"APROBADO\""), "evidencia no aprobada: " + body);
    }

    @Test
    void relatoFueraDeAlcanceSeRechaza() throws InterruptedException {
        Thread.sleep(7_000);
        ResponseEntity<String> r = start(OUT_OF_SCOPE);
        assertEquals(400, r.getStatusCode().value(), r.getBody());
    }

    private ResponseEntity<String> start(String relato) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        String json = "{\"incidentDescription\":\"" + relato.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        return rest.postForEntity(BASE_URL + "/api/v1/claims/start", new HttpEntity<>(json, headers), String.class);
    }

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(180));
        RestTemplate template = new RestTemplate(factory);
        template.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(java.net.URI url, org.springframework.http.HttpMethod method,
                                    org.springframework.http.client.ClientHttpResponse response) {
            }
        });
        return template;
    }
}
