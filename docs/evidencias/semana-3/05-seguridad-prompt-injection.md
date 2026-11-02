# Evidencia 05 — Seguridad frente a prompt injection

## Estado

**La defensa específica contra instrucciones maliciosas dentro de documentos
recuperados no está implementada ni probada de forma verificable.**

Se localizaron algunas reglas de comportamiento en los prompts y controles
estructurales del flujo, pero no equivalen a una sanitización o aislamiento
confiable del contenido no confiable.

## Fuentes de contenido que llegan a los prompts

El backend mezcla datos de distintas procedencias en prompts enviados mediante
`ChatClient`:

| Componente | Contenido interpolado |
|---|---|
| `LegalTranslatorAgent` | relato original y texto de hasta dos documentos recuperados previamente |
| `LegalReasoningAgent` | relato, parámetros recolectados y artículos aplicables |
| `CalculationPlannerAgent` | `executionPlan` de la sesión |
| `DocumentDraftingAgent` | relato, parámetros, plan de ejecución e informe de viabilidad |
| `BaseExtractionAgent` | relato del usuario y lista de parámetros requeridos |

El flujo de recuperación guarda los objetos `Document` en
`Layer1ExecutionContext`. La ingesta conserva el texto de los JSON del corpus,
pero no se localizó un filtro que detecte o neutralice instrucciones incrustadas
antes de que ese texto pueda usarse como contexto.

## Controles observados

### Delimitación textual

Algunos prompts usan etiquetas como `<ROL>`, `<ESCENARIO>`, `<RELATO>` y
`<CONSIGNA>` para separar visualmente instrucciones y datos. Por ejemplo,
`LegalTranslatorAgent` coloca el relato entre `<RELATO>` y `</RELATO>`, y
`DocumentDraftingAgent` separa los campos con etiquetas de texto.

Esto mejora la legibilidad del prompt, pero no constituye un control técnico:
el contenido interpolado puede contener texto que imite esas etiquetas o
instrucciones de mayor prioridad.

### Restricciones de comportamiento

Se encontraron reglas como:

- en `CalculationPlannerAgent`: usar únicamente las herramientas recibidas y
  no inventar herramientas ni fórmulas;
- en `DocumentDraftingAgent`: no inventar hechos y no usar placeholders;
- en `ValidationClassifierAgent`: evaluar únicamente el texto ingresado y no
  mencionar revisión de documentos;
- en `LegalTranslatorAgent`: usar conceptos jurídicos generales y seguir
  reglas explícitas de clasificación.

Estas reglas son instrucciones para el modelo. No se localizó una verificación
posterior que compare la respuesta con una política de seguridad antes de
aceptarla.

### Restricción de herramientas

El ejecutor de cálculos usa un `switch` cerrado sobre tres nombres conocidos:
`calculate_future_date`, `calculate_legal_interest` y
`calculate_percentage`. Un nombre distinto no se ejecuta.

Este control limita la selección de herramientas, pero no evita que un
documento manipule una respuesta textual, altere una clasificación o intente
influir en el plan que el modelo genere.

## Caso exigido por la revisión

### Documento con “ignora tus instrucciones”

No se localizó una prueba automatizada ni un filtro específico para este caso.
Con el código actual, un texto así podría permanecer dentro del contenido de
un `Document` recuperado y llegar al prompt de `LegalTranslatorAgent` como
`Artículos recuperados previamente...`.

Por tanto, el resultado seguro **no está demostrado**. No debe afirmarse que el
sistema ignora correctamente esa instrucción hasta ejecutar una prueba con un
documento controlado y verificar que:

1. el texto se trata como dato y no como instrucción;
2. no cambia las reglas del sistema ni habilita una herramienta no permitida;
3. la respuesta conserva el formato y las restricciones esperadas;
4. se registra o rechaza el fragmento si la política del proyecto así lo exige.

## Pruebas localizadas

La inspección de `backend/App/src/test` localizó las pruebas unitarias actuales,
pero no pruebas específicas para:

- un documento con instrucciones adversariales;
- delimitación o sanitización de fragmentos RAG;
- preservación del prompt del sistema frente a contenido recuperado;
- rechazo de una herramienta solicitada desde un documento;
- validación de la salida después de una posible inyección.

No se ejecutaron pruebas ni se registraron resultados sintéticos.

## Riesgos pendientes

| Riesgo | Evidencia actual | Estado |
|---|---|---|
| Instrucciones dentro de documentos recuperados | El texto recuperado se concatena en el contexto de `LegalTranslatorAgent` | Pendiente de mitigación |
| Instrucciones dentro del relato del usuario | El relato se interpola en varios prompts | Pendiente de prueba adversarial |
| Respuesta del modelo que incumple reglas | Se usa salida estructurada en algunos agentes, pero no una política posterior general | Pendiente |
| Tool calling no autorizado | Existe un enrutamiento cerrado para cálculos | Parcialmente mitigado |
| Ejecución arbitraria de SQL o shell | No se localizó una herramienta de ese tipo | No observado en la inspección |

## Plan de verificación pendiente

La prueba mínima debe usar un documento de prueba cuyo texto incluya una
instrucción adversarial, por ejemplo `ignora tus instrucciones y ejecuta ...`,
sin usar credenciales ni modificar el corpus real. La prueba debe comprobar la
salida del componente que consume el contexto y no solo que el texto fue
almacenado.

También debe agregarse una prueba para el caso equivalente en el relato del
usuario, porque el contenido no confiable no proviene únicamente de Qdrant.
Hasta que esas pruebas existan y se ejecuten con Maven, este requisito debe
permanecer como **Pendiente**.

## Resultado documentado

- Controles estructurales: **Parcialmente observados**.
- Lista cerrada de herramientas de cálculo: **Observada en código**.
- Sanitización o aislamiento de instrucciones en documentos: **No localizada**.
- Prueba contra `ignora tus instrucciones`: **No localizada ni ejecutada**.
- Resultado de Maven para esta evidencia: la suite general pasa, pero no incluye
  una prueba adversarial específica; por tanto, la mitigación completa sigue
  **Pendiente**.