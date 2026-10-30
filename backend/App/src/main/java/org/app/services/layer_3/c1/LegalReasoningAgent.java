package org.app.services.layer_3.c1;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.structured_outputs.FeasibilityReport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class LegalReasoningAgent {

    private final ChatClient chatClient;

    public LegalReasoningAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Evalúa la viabilidad jurídica del caso basándose en los hechos,
     * los parámetros recolectados y las leyes aplicables.
     */
    public FeasibilityReport analyze(SessionObject session) {
        log.info("Iniciando razonamiento jurídico para sesión: {}", session.getSessionId());

        // Extraemos el contexto necesario
        String claimType = session.getClaimType() != null ? session.getClaimType().name() : "NO DEFINIDO";
        String secondaryClaimType = session.getSecondaryClaimType() != null ? session.getSecondaryClaimType().name() : "NINGUNO";
        String narrative = session.getConflictDetails() != null ? session.getConflictDetails().getDescription() : "Sin relato";

        // Los parámetros vienen de la Capa 2 (ej. {"purchaseDate": "02/07/2026", "hasInvoice": "true"})
        String parameters = session.getCollectedParameters() != null
                ? session.getCollectedParameters().toString()
                : "{}";

        // Los artículos fueron recuperados por la Capa 1
        String articles = session.getApplicableArticles() != null
                ? String.join(", ", session.getApplicableArticles())
                : "Ninguno";

        String prompt = """
            <ROL>
            Eres un Abogado Experto en la Ley Orgánica de Defensa del Consumidor (LODC) de Ecuador.
            Tu tarea es evaluar la viabilidad jurídica de un reclamo y generar un informe estructurado.
            </ROL>
            
            <ESCENARIO>
            El usuario presenta un reclamo clasificado como: %s (Secundario: %s).
            
            HECHOS DEL CASO:
            "%s"
            
            PARÁMETROS RECOLECTADOS (Declarados por el usuario):
            %s
            
            ARTÍCULOS DE LA LODC APLICABLES A ESTE TIPO:
            %s
            </ESCENARIO>
            
            <CONSIGNA>
            Evalúa la coherencia jurídica de los hechos y parámetros frente a los artículos citados.
            
            1. EVALÚA CADA PARÁMETRO: Asígnale un peso lógico-jurídico:
               - "FUERTE": Apoya directamente el reclamo (ej. tiene factura, fecha dentro de plazo).
               - "DÉBIL": Aporta poco o es ambiguo.
               - "CONTRADICTORIO": Va en contra del reclamo (ej. la fecha indica prescripción, no tiene el bien).
               - "AUSENTE": No se proporcionó.
               
            2. DETERMINA LA VIABILIDAD GENERAL ("ALTO", "MEDIO", "BAJO"):
               - ALTO: Los parámetros son fuertes, no hay contradicciones y los artículos respaldan claramente.
               - MEDIO: Faltan pruebas clave (ej. no tiene factura), pero la base legal existe.
               - BAJO: Hay contradicciones graves (ej. prescripción de 12 meses, el usuario manipuló el bien).
               
            3. REDACTA EL RAZONAMIENTO: Explica brevemente (máx 300 caracteres) por qué le asignaste ese nivel de viabilidad. 
            
            4. GENERA ADVERTENCIAS: Si hay riesgos legales (prescripción, falta de pruebas), lístalos.
            </CONSIGNA>
            """.formatted(claimType, secondaryClaimType, narrative, parameters, articles);

        // Usamos Structured Output para que devuelva directamente el Record de Java
        FeasibilityReport report = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(FeasibilityReport.class);

        log.info("Razonamiento completado. Viabilidad: {}", report.overallViability());

        return report;
    }
}
