package org.app.services.layer_1.components.c4;


import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimType;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Log4j2
public class MetadataClassificationService {

    public void determineClaimTypes(SessionObject session, List<Document> documents) {
        Map<ClaimType, Integer> votes = new HashMap<>();
        Map<ClaimType, List<String>> articlesByType = new HashMap<>(); // Guarda qué artículos votaron por qué tipo

        // 1. Contar votos basados en los metadatos
        for (Document doc : documents) {
            Object typesObj = doc.getMetadata().get("tipo_reclamo_asociado");
            String articleNumber = (String) doc.getMetadata().get("numero_articulo");

            if (typesObj instanceof List<?> typesList) {
                for (Object type : typesList) {
                    try {
                        ClaimType ct = ClaimType.valueOf(type.toString());
                        votes.put(ct, votes.getOrDefault(ct, 0) + 1);

                        // Guardar el artículo que apoya a este tipo
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

        // 2. Ordenar por cantidad de votos
        List<Map.Entry<ClaimType, Integer>> sortedVotes = votes.entrySet().stream()
                .sorted(Map.Entry.<ClaimType, Integer>comparingByValue().reversed())
                .toList();

        // 3. Asignar Principal y Secundario, y llenar applicableArticles
        if (!sortedVotes.isEmpty()) {
            ClaimType primaryType = sortedVotes.get(0).getKey();
            session.setClaimType(primaryType);

            // Guardamos los artículos que fundamentaron el tipo principal
            List<String> primaryArticles = articlesByType.getOrDefault(primaryType, new ArrayList<>());
            session.setApplicableArticles(primaryArticles);
            session.setRequiredParameters(extractRequiredParameters(documents, primaryType));

            log.info("ClaimType Principal: {} | Artículos base: {}", primaryType, primaryArticles);
            log.info("Parámetros requeridos para la Capa 2: {}", session.getRequiredParameters());

            if (sortedVotes.size() > 1) {
                ClaimType secondaryType = sortedVotes.get(1).getKey();
                session.setSecondaryClaimType(secondaryType);

                // (Opcional) Podrías agregar los artículos del secundario a la misma lista o guardarlos aparte
                log.info("ClaimType Secundario: {} | Artículos base: {}", secondaryType, articlesByType.get(secondaryType));
            }
        } else {
            log.warn("No se pudo determinar un ClaimType.");
            session.setClaimType(null);
        }
    }

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
}