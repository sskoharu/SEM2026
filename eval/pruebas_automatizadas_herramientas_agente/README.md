# Pruebas automatizadas de las herramientas del agente

Las herramientas (`@Tool`) son las funciones de cálculo que el agente de la Capa 4 puede invocar. Las pruebas son JUnit 5 y viven en `src/test/java` (Maven solo compila y ejecuta desde ahí); esta carpeta las indexa como evidencia.

## Ejecución (un comando)

```powershell
.\mvnw.cmd test
```

Solo las de herramientas:

```powershell
.\mvnw.cmd test -Dtest="AgentToolsTest,CalculationEvalTest,CatalogToolsTest"
```

Informes: `target\surefire-reports`.

## Qué prueba cada archivo

| Archivo (`src/test/java/org/app/...`) | Qué verifica |
|---|---|
| `tools/AgentToolsTest.java` | Contrato de las definiciones: toda `@Tool` tiene descripción y nombre único. Casos de borde: cobro en exceso nunca negativo, piso de multa del Art. 71 (y que sale de configuración), segundos redondeados (Art. 91), interés con tasa en porcentaje (Art. 93), suma y diferencia de fechas (ISO y `dd/MM/yyyy`), fechas inválidas rechazadas, plazo del Art. 22 |
| `eval/CalculationEvalTest.java` | **Exactitud de herramientas** sobre `eval/calculos/casos_calculo.csv` (objetivo 100 %) |
| `services/layer_4/tools/CatalogToolsTest.java` | Herramientas del catálogo de cálculo (Capa 4) |
| `services/layer_4/catalog/CalculationCatalogTest.java` | Carga y validación del catálogo de cálculos |
| `services/layer_4/resolver/CatalogResolverServiceTest.java` | Resolución del plan de cálculo a partir del catálogo |
| `services/layer_4/resolver/PlanRendererTest.java` | Presentación del plan de ejecución |
| `services/layer_4/Layer4OrchestratorFlowTest.java` | Flujo de la Capa 4 de extremo a extremo con herramientas |
| `services/layer_3/dpe/DpeRequirementsCheckerTest.java` | Verificación determinista de requisitos de la Defensoría (sin LLM) |

Las herramientas son funciones deterministas, por eso se prueban sin llamar al LLM: no consumen tokens y los resultados son reproducibles.
