package org.app.services.layer_1.components.c3;

import org.app.services.context.layer_1.LegalTranslationResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LegalTranslatorAgent {
    private final ChatClient chatClient;

    public LegalTranslatorAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public LegalTranslationResult translateAndEvaluate(String originalNarrative, List<Document> failedDocs, int attempt) {
        String context = (failedDocs != null && !failedDocs.isEmpty())
                ? "Artículos recuperados previamente (poco relevantes): " + failedDocs.stream().map(Document::getText).limit(2).collect(Collectors.joining(" | "))
                : "Primer intento de búsqueda.";

        String prompt = """
            <ROL>
            Eres un experto jurídico en la Ley Orgánica de Defensa del Consumidor (LODC) de Ecuador.
            Tu misión es ANALIZAR LA CRONOLOGÍA del relato y determinar el tipo de reclamo más probable.
            </ROL>

            <ESCENARIO>
            Relato del usuario:
            <RELATO>
            %s
            </RELATO>

            Contexto de búsqueda anterior: %s (Intento #%d)
            </ESCENARIO>

            <CONSIGNA>

            PASO 1: ANÁLISIS CRONOLÓGICO Y ESTRUCTURAL

            Identifica la SECUENCIA DE EVENTOS en el relato:

            🔍 INDICADORES CLAVE:

            A) ¿Hay una COMPRA inicial?
               - Palabras: "compré", "adquirí", "me vendieron", "pagué por"
               → has_purchase = true/false

            B) ¿El PRODUCTO/BIEN falló o tiene defectos?
               - Palabras: "falló", "se rompió", "defectuoso", "no funciona", "se quemó", "se apagó",
                 "se hinchó", "se destiñó", "mala calidad", "dejó de funcionar"
               → has_product_failure = true/false

            C) ¿El usuario LLEVÓ el bien a un TALLER/TÉCNICO para reparación?
               - Palabras EXPLÍCITAS: "llevé al taller", "dejé en reparación", "envié al técnico",
                 "lo llevé a arreglar", "servicio técnico", "centro de reparación"
               - ⚠️ NO CONFUNDIR "no me dan garantía" con "llevé a reparar"
               → has_repair_service = true/false

            D) ¿El daño/problema ocurrió DURANTE LA CUSTODIA del taller/técnico?
               - Palabras: "me lo devolvieron rayado", "lo dañaron en el taller",
                 "cuando lo recogí estaba peor", "me lo entregaron roto"
               → damage_during_custody = true/false

            E) ¿El problema es de INFORMACIÓN ENGAÑOSA (no de defecto del producto)?
               - Palabras: "publicidad falsa", "precio diferente", "no me dieron factura",
                 "me cobraron más", "no tiene etiqueta"
               - ⚠️ El producto/servicio funciona, solo hay engaño de información
               → has_misleading_info = true/false

            ---

            PASO 2: DETERMINACIÓN DEL TIPO DE RECLAMO

            Basándote en los indicadores, determina el tipo más probable:

            🟢 TIPO: VICIOS_OCULTOS_CALIDAD
            Cronología: COMPRA → FALLA DEL PRODUCTO → NEGATIVA DE GARANTÍA (SIN TALLER INTERMEDIO)
            Condiciones:
            - has_purchase = true
            - has_product_failure = true
            - has_repair_service = false (CRÍTICO: el usuario NO llevó a reparar)
            - damage_during_custody = false

            Ejemplo: "Compré un TV, a las 3 semanas falló, la tienda no me da garantía"

            🔵 TIPO: SERVICIO_TECNICO_CUSTODIA
            Cronología: COMPRA/TENÍA UN BIEN → LLEVÓ A REPARAR → DAÑO EN TALLER o REPARACIÓN DEFECTUOSA
            Condiciones:
            - has_repair_service = true (CRÍTICO: menciona explícitamente taller/técnico)
            - damage_during_custody = true (el daño ocurrió en custodia)

            Ejemplo: "Llevé mi carro al taller, me lo devolvieron rayado"

            🟠 TIPO: INFORMACION_PRECIOS_FACTURACION
            Cronología: COMPRA/SERVICIO → INFORMACIÓN ENGAÑOSA (producto funciona bien)
            Condiciones:
            - has_misleading_info = true
            - has_product_failure = false (el producto/servicio funciona)

            Ejemplo: "Compré un celular, el precio en góndola era $500 pero me cobraron $600. El celular funciona bien"

            ---

            PASO 3: GENERACIÓN DEL QUERY DE BÚSQUEDA

            Basándote en el tipo determinado, genera el query conceptual:

            - Si es VICIOS_OCULTOS_CALIDAD:
              → "producto defectuoso garantía vicios ocultos calidad responsabilidad proveedor bien inadecuado reposición negativa garantía"

            - Si es SERVICIO_TECNICO_CUSTODIA:
              → "servicio técnico deterioro custodia reparación defectuosa responsabilidad taller acondicionamiento menoscabo"

            - Si es INFORMACION_PRECIOS_FACTURACION:
              → "información deficiente publicidad engañosa precio facturación rotulado obligaciones proveedor información veraz"

            ---

            PASO 4: CONFIANZA

            Asigna un nivel de confianza (1-5):
            - 5: Cronología clara y explícita (ej. dice "llevé al taller" o "compré y falló")
            - 4: Cronología clara pero con lenguaje ambiguo
            - 3: Indicadores mixtos, caso frontera
            - 2: Faltan datos clave
            - 1: Muy ambiguo

            ---

            ⚠️ REGLAS CRÍTICAS:

            1. La frase "no me dan garantía" o "se niegan a arreglar" NO significa que el usuario llevó al taller.
               Solo significa que el PROVEEDOR ORIGINAL (tienda donde compró) rechaza cumplir la garantía.
               → Esto es VICIOS_OCULTOS, no SERVICIO_TECNICO

            2. Solo marca has_repair_service=true si el relato dice EXPLÍCITAMENTE:
               - "llevé al taller"
               - "dejé en reparación"
               - "el técnico vino"
               - "servicio técnico"

            3. Si el relato dice "la batería se hinchó" o "la pantalla se levantó":
               → Eso es has_product_failure=true (defecto del producto)
               → NO es damage_during_custody (no hubo custodia)

            4. NO uses palabras literales del relato en el query (ej. "Samsung", "De Prati", "celular").
               Usa conceptos legales generales.

            </CONSIGNA>
            """.formatted(originalNarrative, context, attempt);

        return chatClient.prompt().system(org.app.services.ModelInputPolicy.SYSTEM)
                .user(prompt)
                .call()
                .entity(LegalTranslationResult.class);
    }
}