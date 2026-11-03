package org.app.services.layer_1.components.c1;

import org.app.services.context.layer_1.Layer1Context;
import org.app.schemas.structured_outputs.NarrativeAnalysisResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class NarrativeAnalysisAgent {

    private final ChatClient chatClient;

    public NarrativeAnalysisAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public NarrativeAnalysisResult analyze(Layer1Context context) {
        String prompt = """
            <ROL>
            Eres el Portero y Analista de Relato de un sistema de Defensa del Consumidor en Ecuador (LODC).
            Tu función es evaluar si un texto es un reclamo de consumo, extraer sus entidades básicas y preparar los términos de búsqueda legal.
            </ROL>
            
            <ESCENARIO>
            El usuario ha escrito: 
            <RELATO>
            %s
            </RELATO>
            </ESCENARIO>
            
            <CONSIGNA>
            1. FILTRO: Determina si el texto es un reclamo de consumo (LODC) o si es un saludo, spam o consulta de otra área legal.
            2. SUFICIENCIA: Evalúa si tiene información básica. Si faltan datos vitales, genera hasta 3 preguntas.
            3. ENTIDADES: Extrae el bien/servicio y el proveedor.
            4. KEYWORDS: Genera de 3 a 5 palabras clave legales para la búsqueda vectorial.
            5. TIPO: Infiere el tipo de reclamo (TIPO_1 a TIPO_8) si es evidente.
            </CONSIGNA>

            <REGLA_DE_EXCEPCION_BIEN_SERVICIO>
            El campo "entidad_bien_servicio" es OBLIGATORIO en la mayoría de los reclamos.
            
            SIN EMBARGO, es OPCIONAL (puedes devolver null) ÚNICAMENTE si el relato trata exclusivamente sobre los siguientes temas:
            - Falta de libro de reclamos o negativa a registrar un reclamo (Art. 4 num. 12).
            - Falta de oficina o registro de reclamaciones en servicios públicos (Art. 35).
            - Aplicación de fórmulas de reajuste de precios ilegales (Art. 55 num. 6).
            
            Si el relato NO encaja en ninguna de estas 3 excepciones, DEBES extraer o inferir el bien o servicio.
            </REGLA_DE_EXCEPCION_BIEN_SERVICIO>
            """.formatted(context.incidentDescription());

        return chatClient.prompt().system(org.app.services.ModelInputPolicy.SYSTEM)
                .user(prompt)
                .call()
                .entity(NarrativeAnalysisResult.class);
    }
}