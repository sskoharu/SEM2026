# 06 · RAG: estrategia de fragmentación

Este documento justifica cómo se divide (fragmenta) el corpus antes de indexarlo y por qué. Se basa en lo que implementa el proyecto, no en una configuración genérica.

## 1. Resumen de la decisión

| Aspecto | Decisión |
|---|---|
| Unidad de fragmento | **Un artículo completo = un fragmento** (chunking estructural, no por tamaño fijo) |
| Tamaño | Variable, determinado por el artículo: mediana ≈ 470 caracteres, media ≈ 625, máximo 4.682 (Art. 2) |
| Solapamiento | **Ninguno (0)**; cada fragmento es autocontenido |
| Corpus LODC | 64 fragmentos de 57 artículos distintos (algunos artículos se dividen por numerales) en 10 archivos `src/main/resources/data/lodc/*.json` |
| Corpus DPE | 14 fragmentos de orientación procedimental en `data/dpe/dpe_orientacion.json` (131 a 575 caracteres, media ≈ 226) |
| Embeddings | `text-embedding-3-large` (3072 dimensiones), almacenados en Qdrant (`lodc_leyes_vector`) |
| Metadatos por fragmento | `numero_articulo`, `capitulo`, `tema_principal`, `tipo_reclamo_asociado`, `parametros_requeridos`, `articulos_procedimentales_asociados` |

## 2. Por qué fragmentar por artículo

1. **El artículo es la unidad de cita jurídica.** El sistema debe responder con "Art. 20", "Art. 71", etc. Si un fragmento mezclara dos artículos o partiera uno por la mitad, la cita sería ambigua o incompleta. Con un fragmento por artículo, el `numero_articulo` del fragmento *es* la cita.
2. **El artículo es semánticamente completo.** En la LODC cada artículo desarrolla una sola obligación o derecho (por ejemplo, Art. 17 sobre información veraz, Art. 18 sobre entrega oportuna). Partirlo separaría la regla de sus excepciones o consecuencias y degradaría la recuperación.
3. **Son cortos.** El 95 % de los artículos mide menos de unos 1.500 caracteres (aprox. 400 tokens), muy por debajo del límite del modelo de embeddings. No hay necesidad técnica de trocearlos. Un fragmento tan pequeño mantiene el embedding enfocado en un solo tema, lo que mejora la precisión del *top-k*.
4. **Permite metadatos precisos.** Cada artículo se asocia a un tipo de reclamo (`tipo_reclamo_asociado`), a los parámetros que exige (`parametros_requeridos`) y a los artículos procedimentales relacionados. Esa asociación solo tiene sentido si el fragmento es exactamente un artículo. Estos metadatos alimentan la clasificación de la Capa 1 y la solicitud de datos de la Capa 2.
5. **Permite recuperación exacta por número.** La Capa 3 recupera artículos concretos por `numero_articulo` (filtro sobre un índice de payload), sin depender de similitud semántica. Esto exige que el número identifique un único fragmento.
6. **Es auditable y reproducible.** El identificador es determinista (`lodc_ley_art_N`, convertido en UUID por hash), por lo que reingestar hace *upsert* sin duplicar y se puede verificar qué texto exacto sustenta cada respuesta.

## 3. Por qué no hay solapamiento

El solapamiento sirve para que el contexto que cae en el borde entre dos trozos no se pierda cuando se corta un texto por tamaño. Aquí **no se corta por tamaño**, sino en las fronteras naturales del documento, de modo que no existen "bordes" arbitrarios que compensar. Añadir solapamiento tendría costos sin beneficio:

- Duplicaría texto entre fragmentos, y un mismo artículo podría aparecer dos veces en el *top-k*, desplazando otras fuentes.
- Haría ambiguo el vínculo entre fragmento y número de artículo, que es la base de la cita.
- Aumentaría el costo de embeddings y de almacenamiento sin mejorar la recuperación.

Las referencias cruzadas entre artículos se resuelven con **metadatos** (`articulos_procedimentales_asociados`) y no con texto duplicado.

## 4. Por qué no se usa un tamaño fijo

Un divisor por tokens o caracteres (por ejemplo, 500 tokens con 50 de solapamiento) se justifica para documentos largos y sin estructura. La LODC tiene estructura explícita y fragmentos naturalmente pequeños, así que un tamaño fijo habría cortado artículos en puntos arbitrarios y habría perdido la correspondencia fragmento = cita.

## 5. Caso especial: artículos largos

Tres artículos superan los 1.500 caracteres: Art. 2 (4.682, definiciones), Art. 40 (2.076) y Art. 71 (1.630). Se mantienen **íntegros**:

- Caben holgadamente en el límite de entrada del modelo de embeddings.
- Art. 71 (sanciones) se cita directamente en los reclamos de vicios ocultos y no debe perder partes de su texto.
- El Art. 2 es un glosario de definiciones. Su embedding es más genérico, pero el sistema no depende de recuperarlo por similitud para responder.

**Limitación asumida:** un artículo largo produce un embedding más "promediado" y puede puntuar peor frente a consultas muy específicas sobre una sola de sus partes. Si la evaluación mostrara fallos por este motivo, la mejora prevista es subdividir **solo esos artículos por incisos o numerales**, conservando `numero_articulo` y añadiendo un metadato de inciso, sin tocar el resto del corpus.

## 6. Corpus de orientación de la Defensoría del Pueblo (DPE)

La orientación procedimental (cómo presentar el reclamo, qué requisitos exige, canales y pasos) no tiene artículos, así que se fragmenta **por unidad funcional**: un fragmento por requisito, paso, canal, descripción del trámite y naturaleza del rol. Cada uno lleva `seccion` (`REQUISITO`, `PASO`, `CANAL`, `DESCRIPCION_TRAMITE`, `NATURALEZA_ROL`), `capa_destino` y `articulos_lodc_relacionados`. Son cortos (media ≈ 226 caracteres) porque cada uno expresa una sola instrucción.

Este corpus **no se recupera por embeddings**: la Capa 3 lo consulta de forma determinista por sección y orden de paso (`DpeGuidanceService`), y la Capa 5 lo usa para generar el documento. La fragmentación en unidades pequeñas con metadatos es lo que permite ese acceso sin LLM ni similitud, y así no se mezcla orientación práctica con derecho sustantivo.

## 7. Parámetros de recuperación asociados

| Parámetro | Valor | Dónde |
|---|---|---|
| Búsqueda semántica | `search(query, topK=5)` | `BuscandoFuentesStep` / `VectorSearchTool.search` |
| Umbral de similitud | 0,30 (bajo, para no descartar candidatos; la clasificación posterior filtra) | `VectorSearchTool` |
| Umbral de abstención | 0,40 inicial (`app.rag.abstention-threshold`); si el mejor score recuperado es menor, tras 3 reintentos responde `SOLICITANDO_ACLARACION` con el objeto `abstention` (motivo, mejorScore, umbral). Sin calibrar: ver `docs/07-evaluacion.md` | `EvaluandoPrecisionStep` / `AbstentionPolicy` |
| Recuperación exacta | por `numero_articulo` | `VectorSearchTool.getArticlesByNumbers` |
| Índices de payload | `numero_articulo`, `tipo_reclamo_asociado`, `original_id` | `LodcDataIngestionRunner` |

Con fragmentos de un artículo, un `topK` de 5 entrega como máximo cinco artículos distintos, un contexto reducido y sin duplicados para el LLM.

## 8. Cómo se valida

- La métrica **Recuperación correcta** del conjunto de evaluación (`eval/20_relatos_referencia/conjunto_evaluacion.md`) comprueba que, para cada relato, los artículos esperados aparecen en `applicableArticles`.
- Las pruebas funcionales contra la URL desplegada (`plan_pruebas_funcionales_url_desplegada/`, `DeployedFunctionalTest`) repiten esa verificación de extremo a extremo.

## 9. Limitaciones conocidas

- La recuperación es solo vectorial: no hay búsqueda híbrida (BM25) ni filtro por `tipo_reclamo_asociado` en la consulta, aunque el índice existe. Una consulta que cite "Art. 71" literal depende solo de la similitud.
- El corpus contiene 57 artículos de la LODC (64 fragmentos), no la totalidad de la ley ni su reglamento. El sistema se abstiene cuando el caso queda fuera de ese alcance.
- No se ha medido empíricamente el efecto del tamaño de fragmento (no hay comparación contra un divisor de tamaño fijo). La justificación es estructural y jurídica, respaldada por la métrica de recuperación, no por un experimento de ablación.
