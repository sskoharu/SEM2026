package org.app.services.layer_1.components.c4;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimType;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Log4j2
public class MetadataClassificationService {

    /**
     * Determina el tipo de reclamo usando un enfoque híbrido:
     * 1. Votación de Qdrant (basado en metadatos)
     * 2. Sugerencia del LLM (análisis cronológico) - con PESO DOBLE
     * 3. Reglas de desempate semántico
     */
    public void determineClaimTypes(SessionObject session, List<Document> documents, String llmSuggestedType) {
        Map<ClaimType, Integer> votes = new HashMap<>();
        Map<ClaimType, List<String>> articlesByType = new HashMap<>();

        // 1. Contar votos basados en los metadatos de Qdrant
        for (Document doc : documents) {
            Object typesObj = doc.getMetadata().get("tipo_reclamo_asociado");
            String articleNumber = (String) doc.getMetadata().get("numero_articulo");

            if (typesObj instanceof List<?> typesList) {
                for (Object type : typesList) {
                    try {
                        ClaimType ct = ClaimType.valueOf(type.toString());
                        votes.put(ct, votes.getOrDefault(ct, 0) + 1);

                        articlesByType.computeIfAbsent(ct, k -> new ArrayList<>());
                        if (articleNumber != null && !articlesByType.get(ct).contains(articleNumber)) {
                            articlesByType.get(ct).add(articleNumber);
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Tipo de reclamo desconocido en metadatos: {}", type);
                    }
                }
            }
        }

        // 2. NUEVO: Agregar el voto del LLM con PESO DOBLE (más confiable por análisis cronológico)
        if (llmSuggestedType != null && !llmSuggestedType.isEmpty()) {
            try {
                ClaimType llmType = ClaimType.valueOf(llmSuggestedType);
                votes.put(llmType, votes.getOrDefault(llmType, 0) + 2); // Peso doble para el LLM
                log.info("🧠 LLM sugiere tipo: {} (con peso +2)", llmType);
            } catch (IllegalArgumentException e) {
                log.warn("Tipo sugerido por LLM no válido: {}", llmSuggestedType);
            }
        }

        // 3. Ordenar por cantidad de votos (Votación híbrida Qdrant + LLM)
        List<Map.Entry<ClaimType, Integer>> sortedVotes = votes.entrySet().stream()
                .sorted(Map.Entry.<ClaimType, Integer>comparingByValue().reversed())
                .toList();

        if (sortedVotes.isEmpty()) {
            log.warn("No se pudo determinar un ClaimType por votos.");
            session.setClaimType(null);
            return;
        }

        ClaimType primaryType = sortedVotes.get(0).getKey();
        log.info("📊 Votación híbrida: {}", sortedVotes.stream()
                .limit(3)
                .map(e -> e.getKey() + "=" + e.getValue())
                .toList());

        // 4. MOTOR DE REGLAS DE DESEMPATE (última línea de defensa)
        primaryType = applySemanticRules(session, primaryType);

        // 5. Asignar Principal y Secundario
        session.setClaimType(primaryType);
        List<String> primaryArticles = articlesByType.getOrDefault(primaryType, new ArrayList<>());

        // Si la regla de desempate cambió el tipo, aseguramos que los artículos correspondan
        if (primaryArticles.isEmpty()) {
            primaryArticles = extractArticlesByType(documents, primaryType);
        }

        session.setApplicableArticles(primaryArticles);
        session.setRequiredParameters(extractRequiredParameters(documents, primaryType));

        log.info("✅ ClaimType Principal FINAL: {} | Artículos base: {}", primaryType, primaryArticles);
        log.info("📝 Parámetros requeridos para la Capa 2: {}", session.getRequiredParameters());

        // 6. Asignar Secundario (si existe y es diferente al principal)
        for (int i = 1; i < sortedVotes.size(); i++) {
            ClaimType secondaryCandidate = sortedVotes.get(i).getKey();
            if (secondaryCandidate != primaryType) {
                session.setSecondaryClaimType(secondaryCandidate);
                log.info("🔁 ClaimType Secundario: {}", secondaryCandidate);
                break;
            }
        }
    }

    /**
     * MOTOR DE REGLAS DE DESEMPATE SEMÁNTICO
     * Evalúa la ESTRUCTURA del relato para corregir confusiones de Qdrant entre tipos similares.
     */
    private ClaimType applySemanticRules(SessionObject session, ClaimType votedType) {
        String narrative = session.getConflictDetails().getDescription().toLowerCase();

        // =========================================================
        // FASE 1: EXTRACCIÓN DE PATRONES SEMÁNTICOS
        // =========================================================

        // --- Patrón de COMPRA de producto ---
        boolean mencionaCompra = narrative.contains("compré") || narrative.contains("compre")
                || narrative.contains("adquirí") || narrative.contains("adquiri")
                || narrative.contains("compra") || narrative.contains("adquirir")
                || narrative.contains("compramos") || narrative.contains("adquirimos");

        // --- Patrón de FALLA del producto (después de la compra) ---
        boolean mencionaFallaProducto = narrative.contains("falló") || narrative.contains("fallo")
                || narrative.contains("defecto") || narrative.contains("defectuoso")
                || narrative.contains("no funciona") || narrative.contains("se rompió")
                || narrative.contains("se rompio") || narrative.contains("se quemó")
                || narrative.contains("se quemo") || narrative.contains("se apagó")
                || narrative.contains("se apago") || narrative.contains("dañado")
                || narrative.contains("empezó a fallar") || narrative.contains("empezo a fallar")
                || narrative.contains("dejó de funcionar") || narrative.contains("dejo de funcionar")
                || narrative.contains("no sirve") || narrative.contains("mala calidad");

        // --- Patrón de GARANTÍA o FACTURA ---
        boolean mencionaGarantiaFactura = narrative.contains("garantía") || narrative.contains("garantia")
                || narrative.contains("factura") || narrative.contains("ticket")
                || narrative.contains("comprobante") || narrative.contains("recibo");

        // --- Patrón de NEGATIVA DE GARANTÍA (proveedor no cumple) ---
        boolean mencionaNegativaGarantia = narrative.contains("no me dan garantía")
                || narrative.contains("no me dan garantia")
                || narrative.contains("rechazan") || narrative.contains("no quieren")
                || narrative.contains("no me quieren") || narrative.contains("se niegan")
                || narrative.contains("no aceptan") || narrative.contains("no reconocen")
                || (narrative.contains("dicen que") && (narrative.contains("abrí") || narrative.contains("abri")
                    || narrative.contains("lo dañé") || narrative.contains("lo dañe")
                    || narrative.contains("mal uso")));

        // --- Patrón de TALLER / SERVICIO TÉCNICO (custodia) ---
        boolean mencionaTaller = narrative.contains("taller") || narrative.contains("servicio técnico")
                || narrative.contains("servicio tecnico") || narrative.contains("técnico")
                || narrative.contains("tecnico") || narrative.contains("dejé a reparar")
                || narrative.contains("deje a reparar") || narrative.contains("llevé a arreglar")
                || narrative.contains("lleve a arreglar") || narrative.contains("lo llevé")
                || narrative.contains("lo lleve") || narrative.contains("dejé mi")
                || narrative.contains("deje mi");

        // --- Patrón de DAÑO EN TALLER (durante custodia) ---
        boolean mencionaDanoEnTaller = (narrative.contains("rayó") || narrative.contains("rayo")
                || narrative.contains("dañó") || narrative.contains("daño")
                || narrative.contains("me lo devolvió") || narrative.contains("me lo devolvio")
                || narrative.contains("me lo entregó") || narrative.contains("me lo entrego"))
                && (narrative.contains("roto") || narrative.contains("rayado")
                    || narrative.contains("peor") || narrative.contains("más dañado")
                    || narrative.contains("mas dañado"));

        // --- Patrón de PUBLICIDAD ENGAÑOSA ---
        boolean mencionaPublicidadEnganosa = narrative.contains("publicidad")
                || narrative.contains("anuncio") || narrative.contains("propaganda")
                || narrative.contains("catálogo") || narrative.contains("catalogo")
                || narrative.contains("oferta") || narrative.contains("promoción")
                || narrative.contains("promocion");

        // --- Patrón de PRECIO INCORRECTO (pero el producto funciona) ---
        boolean mencionaPrecioIncorrecto = narrative.contains("precio")
                && (narrative.contains("diferente") || narrative.contains("más caro")
                    || narrative.contains("mas caro") || narrative.contains("engaño")
                    || narrative.contains("engano") || narrative.contains("falso")
                    || narrative.contains("no coincide") || narrative.contains("cobró más")
                    || narrative.contains("cobro mas"));

        // --- Patrón de FALTA DE FACTURA / INFORMACIÓN ---
        boolean mencionaFaltaFacturaInfo = narrative.contains("no me dieron factura")
                || narrative.contains("no me dio factura") || narrative.contains("sin factura")
                || narrative.contains("no me informaron") || narrative.contains("no me dijeron")
                || narrative.contains("no me explicaron");

        // =========================================================
        // FASE 2: APLICACIÓN DE REGLAS DE DESEMPATE
        // =========================================================

        // --- REGLA 1: VICIOS OCULTOS gana a SERVICIO TÉCNICO ---
        // Condición: Compra + Falla del producto + (Garantía o Negativa) + SIN mención de taller
        if (votedType == ClaimType.SERVICIO_TECNICO_CUSTODIA) {
            if (mencionaCompra && mencionaFallaProducto && !mencionaTaller) {
                log.info("⚖️ REGLA 1 ACTIVADA: Sobrescribiendo SERVICIO_TECNICO -> VICIOS_OCULTOS");
                log.info("   Razón: Producto comprado que falló SIN intervención de taller.");
                log.info("   Indicadores: compra={}, falla={}, taller={}", mencionaCompra, mencionaFallaProducto, mencionaTaller);
                return ClaimType.VICIOS_OCULTOS_CALIDAD;
            }
        }

        // --- REGLA 2: SERVICIO TÉCNICO gana a VICIOS OCULTOS ---
        // Condición: Mención explícita de taller/reparación + Daño durante custodia
        if (votedType == ClaimType.VICIOS_OCULTOS_CALIDAD) {
            if (mencionaTaller && mencionaDanoEnTaller) {
                log.info("⚖️ REGLA 2 ACTIVADA: Sobrescribiendo VICIOS_OCULTOS -> SERVICIO_TECNICO");
                log.info("   Razón: El daño ocurrió en el TALLER (custodia del prestador).");
                log.info("   Indicadores: taller={}, dañoEnTaller={}", mencionaTaller, mencionaDanoEnTaller);
                return ClaimType.SERVICIO_TECNICO_CUSTODIA;
            }
        }

        // --- REGLA 3: INFORMACIÓN gana a VICIOS OCULTOS ---
        // Condición: Problema de precio/publicidad/factura SIN falla del producto
        if (votedType == ClaimType.VICIOS_OCULTOS_CALIDAD) {
            if ((mencionaPublicidadEnganosa || mencionaPrecioIncorrecto || mencionaFaltaFacturaInfo)
                    && !mencionaFallaProducto) {
                log.info("⚖️ REGLA 3 ACTIVADA: Sobrescribiendo VICIOS_OCULTOS -> INFORMACION_PRECIOS_FACTURACION");
                log.info("   Razón: El problema es de INFORMACIÓN, no de calidad del bien.");
                log.info("   Indicadores: publicidad={}, precio={}, faltaInfo={}, falla={}",
                        mencionaPublicidadEnganosa, mencionaPrecioIncorrecto, mencionaFaltaFacturaInfo, mencionaFallaProducto);
                return ClaimType.INFORMACION_PRECIOS_FACTURACION;
            }
        }

        // --- REGLA 4: INFORMACIÓN gana a SERVICIO TÉCNICO ---
        // Condición: Problema de documentación/precio SIN servicio de reparación
        if (votedType == ClaimType.SERVICIO_TECNICO_CUSTODIA) {
            if ((mencionaPrecioIncorrecto || mencionaFaltaFacturaInfo)
                    && !mencionaTaller && !mencionaFallaProducto) {
                log.info("⚖️ REGLA 4 ACTIVADA: Sobrescribiendo SERVICIO_TECNICO -> INFORMACION_PRECIOS_FACTURACION");
                log.info("   Razón: El problema es de INFORMACIÓN, no de servicio técnico.");
                return ClaimType.INFORMACION_PRECIOS_FACTURACION;
            }
        }

        // --- REGLA 5: Confirmación de SERVICIO TÉCNICO si hay custodia real ---
        if (mencionaTaller && (mencionaDanoEnTaller || narrative.contains("repararon mal")
                || narrative.contains("reparo mal") || narrative.contains("empeoró")
                || narrative.contains("empeoro"))) {
            log.info("⚖️ REGLA 5 ACTIVADA: Confirmado SERVICIO_TECNICO_CUSTODIA");
            log.info("   Razón: Custodia real del bien + daño/servicio defectuoso.");
            return ClaimType.SERVICIO_TECNICO_CUSTODIA;
        }

        // =========================================================
        // FASE 3: VALIDACIÓN FINAL POR CRONOLOGÍA
        // =========================================================
        // Si la votación fue por Servicio Técnico pero NO hay evidencia de custodia,
        // es probablemente un caso de Vicios Ocultos mal clasificado.
        if (votedType == ClaimType.SERVICIO_TECNICO_CUSTODIA) {
            if (mencionaCompra && mencionaFallaProducto && mencionaNegativaGarantia && !mencionaTaller) {
                log.info("⚖️ REGLA DE VALIDACIÓN CRONOLÓGICA: Forzando VICIOS_OCULTOS");
                log.info("   Razón: La cronología indica: COMPRA → FALLA → NEGATIVA DE GARANTÍA (sin taller intermedio).");
                return ClaimType.VICIOS_OCULTOS_CALIDAD;
            }
        }

        log.info("   No se aplicó ninguna regla de desempate. Tipo votado: {}", votedType);
        return votedType; // Si ninguna regla se activó, respetamos la votación de Qdrant.
    }

    /**
     * Extrae parámetros de los documentos, filtrando por el tipo final ganador.
     */
    private List<String> extractRequiredParameters(List<Document> documents, ClaimType primaryType) {
        Set<String> parameters = new LinkedHashSet<>();

        for (Document document : documents) {
            Object associatedTypes = document.getMetadata().get("tipo_reclamo_asociado");
            if (!(associatedTypes instanceof List<?> types)
                    || types.stream().noneMatch(type -> primaryType.name().equals(String.valueOf(type)))) {
                continue;
            }

            Object requiredParameters = document.getMetadata().get("parametros_requeridos");
            if (requiredParameters instanceof List<?> parameterList) {
                parameterList.stream()
                        .map(String::valueOf)
                        .filter(parameter -> !parameter.isBlank())
                        .forEach(parameters::add);
            }
        }

        return new ArrayList<>(parameters);
    }

    // Método auxiliar para obtener artículos si la regla de desempate cambia el tipo
    private List<String> extractArticlesByType(List<Document> documents, ClaimType type) {
        List<String> articles = new ArrayList<>();
        for (Document doc : documents) {
            Object associatedTypes = doc.getMetadata().get("tipo_reclamo_asociado");
            if (associatedTypes instanceof List<?> types && types.stream().anyMatch(t -> type.name().equals(String.valueOf(t)))) {
                String num = (String) doc.getMetadata().get("numero_articulo");
                if (num != null && !articles.contains(num)) articles.add(num);
            }
        }
        return articles;
    }
}