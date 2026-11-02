# Evidencia 03 — Abstención y consultas sin respaldo

## Estado

**Hay pausas y rechazos implementados; la abstención RAG verificable todavía no
está demostrada con una ejecución externa.**

La implementación actual no muestra una respuesta final con una política
explícita de abstención basada en citas. Sí contiene dos mecanismos relacionados:

1. el `Gatekeeper` rechaza solicitudes que no parecen reclamos de consumo;
2. la Capa 1 pausa el flujo y solicita aclaración cuando el relato es
   insuficiente, incoherente o no alcanza el criterio configurado de recuperación.

Estos comportamientos no deben presentarse automáticamente como una abstención
correcta ante una pregunta cuya respuesta no está en las fuentes.

## Comportamientos localizados

### 1. Solicitud fuera del dominio

`InitialAnalysisService` invoca a `NarrativeAnalysisAgent` antes de iniciar el
RAG. Si el resultado indica que no es un reclamo de consumo, lanza
`IllegalArgumentException` con el mensaje de rechazo del agente o con un mensaje
por defecto.

`ClaimController.startClaim` transforma esa excepción en una respuesta HTTP
`400 Bad Request` con un objeto que contiene `error`.

Esto cubre una consulta fuera del dominio en la entrada inicial, pero no prueba
que el sistema se abstenga de contestar una pregunta jurídica sin respaldo en
el corpus.

### 2. Relato insuficiente

Si el `NarrativeAnalysisAgent` marca el relato como insuficiente,
`InitialAnalysisService`:

- crea la sesión;
- la deja en `Layer1InternalState.SOLICITANDO_ACLARACION`;
- guarda `clarificationMessage`;
- guarda hasta las preguntas aclaratorias devueltas por el análisis;
- evita iniciar todavía el orquestador RAG.

El mensaje configurado es:

```text
Para poder entender tu caso y buscar la ley exacta, necesitamos que aclares lo siguiente:
```

La sesión se devuelve al cliente para que pueda aportar información adicional.

### 3. Relato jurídicamente incoherente

`TraduciendoQueryStep` evalúa `LegalTranslationResult.isLegallyCoherent()`.
Cuando el resultado es falso, guarda el motivo en logs, establece el mensaje:

```text
El caso que describes parece no estar relacionado con la Defensa del Consumidor...
```

y cambia el estado a `SOLICITANDO_ACLARACION`.

Este camino solicita aclaración; no genera una respuesta jurídica inventada.

### 4. Recuperación insuficiente

`VectorSearchTool` aplica un umbral de similitud de `0.75`. Si la búsqueda no
devuelve documentos, `EvaluandoPrecisionStep` establece:

```text
No pudimos encontrar una base legal exacta...
```

y pasa a `SOLICITANDO_ACLARACION`. La prueba de contrato del conjunto de
evaluación valida los casos de silencio, pero todavía no ejecuta consultas
contra Qdrant.

## Prueba propuesta

La prueba para cerrar este punto debe usar una entrada que no pueda resolverse
con el corpus LODC indexado, por ejemplo una pregunta sobre un tema externo al
dominio. La entrada exacta debe registrarse cuando se ejecute; no se declara
aquí un resultado observado.

### Entrada

```text
[Registrar aquí la consulta ejecutada exactamente]
```

### Resultado esperado

El sistema debería:

- no presentar una conclusión jurídica como si estuviera respaldada;
- indicar que no encontró una fuente suficiente;
- orientar al usuario a reformular la consulta o aportar información pertinente;
- conservar el identificador de la sesión y el estado de pausa, si corresponde.

### Resultado observado

```text
Pendiente de ejecución con el backend y Qdrant configurados.
```

Debe conservarse la respuesta HTTP o la sesión devuelta, el estado interno
resultante y los logs relevantes, eliminando tokens, credenciales y datos
personales.

## Casos que deben distinguirse

| Caso | Mecanismo localizado | Estado de evidencia |
|---|---|---|
| Saludo, spam o asunto no relacionado con consumo | Rechazo del `Gatekeeper` | Implementado en código; ejecución pendiente |
| Reclamo de consumo con datos insuficientes | `SOLICITANDO_ACLARACION` | Implementado en código; ejecución pendiente |
| Relato jurídicamente incoherente | `SOLICITANDO_ACLARACION` desde `TraduciendoQueryStep` | Implementado en código; ejecución pendiente |
| Sin resultados o recuperación insuficiente | Umbral `0.75` y `SOLICITANDO_ACLARACION` | Implementado en código; ejecución pendiente |
| Pregunta cuya respuesta no existe en las fuentes | Lista vacía y mensaje de aclaración | Implementado parcialmente; ejecución pendiente |

## Pruebas automatizadas

No se localizó una prueba automatizada específica que demuestre estos flujos de
abstención o rechazo. La prueba deberá ejecutarse con el comando documentado en
el README:

```powershell
.\backend\App\mvnw.cmd test
```

Si se agregan pruebas Java posteriormente, deberán verificar estados y mensajes
sin depender de un proveedor LLM real. El comportamiento de un proveedor externo
debe probarse mediante un doble controlado y documentarse separadamente.

## Conclusión

El backend evita continuar en varios escenarios problemáticos mediante rechazo o
solicitud de aclaración. Esto es evidencia de control de flujo, no evidencia
suficiente de abstención RAG con fuentes. El requisito solo podrá marcarse como
verificado después de ejecutar una consulta sin respaldo y comprobar que la
respuesta final no inventa información y orienta al usuario de forma explícita.