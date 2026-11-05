# Pruebas automatizadas de la construcción del prompt

Los prompts son plantillas `.st` en `src/main/resources/prompts/`, cargadas y renderizadas por `PromptLoader` con variables `{{nombre}}`. Las pruebas son JUnit 5 y viven en `src/test/java` (Maven solo compila y ejecuta desde ahí); esta carpeta las indexa como evidencia.

## Ejecución (un comando)

```powershell
.\mvnw.cmd test
```

Solo las de prompts:

```powershell
.\mvnw.cmd test -Dtest="PromptTemplatesTest,PromptConstructionTest,InitialAnalysisServiceScopeTest"
```

Informes: `target\surefire-reports`.

## Qué prueba cada archivo

| Archivo (`src/test/java/org/app/...`) | Qué verifica |
|---|---|
| `prompts/PromptTemplatesTest.java` | Cada plantilla se renderiza con sus propias variables sin dejar placeholders; falla si falta una variable; valores con `$`, `\` o `{{x}}` se conservan literales; las reglas de extracción por tipo de reclamo cargan |
| `prompts/PromptConstructionTest.java` | Integridad de todas las `.st` (no vacías, UTF-8 sano, placeholders bien formados). El relato inyectado queda como dato literal y no se reinterpreta. Salvaguardas presentes: tres categorías de alcance del portero y tipos soportados, prohibición de preguntar fuera de alcance, relato delimitado como DATOS y no revelar instrucciones (`system_v2/v3`), salida JSON exigida, `{{relato}}` presente en los prompts de capa |
| `services/layer_1/components/c1/InitialAnalysisServiceScopeTest.java` | Cómo se usa el resultado del prompt del portero: fuera de corpus y fuera de dominio devuelven mensajes distintos (HTTP 400) y no piden aclaración |

## Qué no cubren

Comprueban que el prompt se construye bien y contiene las reglas, no que el LLM las obedezca. Ese comportamiento se mide con el conjunto de evaluación (`eval/20_relatos_referencia`) y las pruebas funcionales contra la URL desplegada (`DeployedFunctionalTest`, solo con `-De2e=true`).
