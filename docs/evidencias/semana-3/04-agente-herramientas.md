# Evidencia 04 — Agente y herramientas deterministas

## Estado

**La cobertura mínima de herramientas deterministas está verificada; las
garantías de agente, configuración variable y servicios externos siguen
parcialmente pendientes.**

Esta evidencia describe las clases localizadas en el backend. No se afirma que
una herramienta haya sido invocada por un modelo ni que una prueba haya pasado
si no existe una ejecución registrada.

## Herramientas localizadas

### Validación de entradas

`org.app.services.layer_2.components.tools.ValidationTools` es un componente
Spring que expone dos métodos con `@Tool`:

| Herramienta | Parámetros | Comportamiento observado en código |
|---|---|---|
| `validateDate` | `parameterName`, `userInput` | Acepta fechas `dd/MM/yyyy` que no sean futuras; devuelve `OK` o un mensaje `ERROR` |
| `validateText` | `parameterName`, `userInput` | Rechaza vacío, acepta ciertas respuestas cortas y exige al menos tres caracteres en los demás casos |

`ValidationClassifierAgent` recibe estas herramientas mediante
`.tools(validationTools)` y solicita al modelo normalizar y validar una entrada.

### Cálculos

`org.app.services.layer_4.tools.CalculationTools` expone tres herramientas
deterministas:

| Herramienta | Parámetros | Resultado |
|---|---|---|
| `calculate_future_date` | `daysToAdd` | Fecha actual más días, en `dd/MM/yyyy` |
| `calculate_legal_interest` | `capital`, `daysLate` | `(capital * tasa configurada * días) / 365`, redondeado a dos decimales |
| `calculate_percentage` | `amount`, `percentage` | Porcentaje del monto, redondeado a dos decimales |

Las operaciones usan `BigDecimal` para montos y `RoundingMode.HALF_UP` en los
resultados monetarios. La fecha depende de `LocalDate.now()`, por lo que una
prueba reproducible debe controlar la fecha o verificar el formato y la
relación temporal, no comparar contra una fecha fija.

## Integración del agente

`CalculationPlannerAgent` recibe el plan de ejecución de la sesión y expone
`CalculationTools` al modelo mediante `.tools(calculationTools)`. El prompt
indica que debe usar únicamente las herramientas proporcionadas y producir un
JSON de tareas.

`CalculationExecutorService` no ejecuta código generado libremente: enruta el
campo `toolName` mediante un `switch` cerrado hacia tres métodos Java conocidos.
Un nombre no reconocido produce un warning y no ejecuta una operación.

Esta lista cerrada es una barrera importante frente a la ejecución arbitraria
de comandos. No se localizó en el código revisado una herramienta que ejecute
SQL, shell o comandos generados por el modelo.

## Extracción y validación de parámetros

Los agentes de extracción de Capa 2 implementan `ILayer2ExtractionAgent`.
`BaseExtractionAgent` solicita tres grupos:

- `extractedParameters`;
- `irrelevantParameters`;
- `proceduralParameters`.

La respuesta se parsea como `PreExtractionResult` con Jackson. Si ocurre una
excepción durante la llamada o el parseo, el agente devuelve un resultado vacío
con listas nulas. Este fallback evita romper el flujo, pero debe ser cubierto
por una prueba para determinar si es el comportamiento deseado.

## Contratos y validación de parámetros

Las herramientas tienen firmas Java concretas y anotaciones `@Tool`, pero no se
localizaron clases Pydantic porque el backend es Java. Tampoco se localizaron
anotaciones Bean Validation específicas sobre los argumentos de las herramientas
de cálculo.

Existe una inconsistencia que debe registrarse antes de declarar esta parte
verificada: el prompt de `ValidationClassifierAgent` menciona una herramienta
`validateAmount`, pero `ValidationTools` solo implementa `validateDate` y
`validateText`. No se debe documentar `validateAmount` como disponible.

## Límite de tareas, confirmación y fuera de dominio

`CalculationExecutorService` define `MAX_ITER = 3` y ejecuta como máximo tres
tareas del plan. Si recibe una cuarta tarea, registra
`MAX_ITER_EXCEEDED`. Este límite controla el ejecutor de cálculos, no todos los
ciclos internos de tool calling de Spring AI.

El fuera de dominio se filtra en la entrada mediante el `Gatekeeper`, como se
documentó en `03-abstencion.md`. No se localizó una prueba que demuestre que un
agente rechaza una solicitud de herramienta no disponible.

`ClaimController` mantiene la sesión en `WAITING_FOR_DECISION` antes de
continuar la ruta de acción. Además, `Layer5Controller` rechaza la generación
del DOCX con `409 Conflict` si la Capa 3 todavía no está en `DONE`. Por tanto,
la generación queda protegida por la aceptación previa; no existe un endpoint
de envío a una autoridad.

## Pruebas Java localizadas y ejecutadas

En `backend/App/src/test` se localizó la clase:

```text
backend/App/src/test/java/org/app/controllers/ClaimControllerTest.java
```

Antes de esta evidencia no se localizaron pruebas específicas para las
herramientas. Actualmente sí existen:

- `ValidationToolsTest`;
- `CalculationToolsTest`;
- `CalculationExecutorServiceTest`.

Siguen sin existir pruebas específicas para construcción de prompts, límite de
iteraciones, consulta fuera de dominio o prompt injection.

Se agregaron pruebas unitarias puras para las herramientas y se actualizó la
prueba existente del controlador para el API de pruebas de Spring Boot 4. El
comando ejecutado desde la raíz del repositorio fue:

```powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml test
```

### Resultado observado

Ejecución realizada el **2026-11-02**, con JDK 21:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Distribución observada:

| Clase | Pruebas | Fallos | Errores |
|---|---:|---:|---:|
| `ClaimControllerTest` | 2 | 0 | 0 |
| `ValidationToolsTest` | 7 | 0 | 0 |
| `CalculationExecutorServiceTest` | 4 | 0 | 0 |
| `CalculationToolsTest` | 4 | 0 | 0 |

El resultado demuestra la ejecución de las pruebas unitarias agregadas para
validación, cálculos y límite de tareas. No demuestra todavía pruebas del
planificador con un LLM real, prompt injection ni ejecución contra Qdrant.

## Cobertura mínima completada y trabajo pendiente

La cobertura mínima ejecutada, sin llamar a un LLM real, cubre:

1. fecha válida, fecha futura y formato inválido;
2. texto vacío, respuesta corta permitida y texto demasiado corto;
3. porcentaje con monto y tasa conocidos;
4. interés legal con valores conocidos y redondeo;
5. enrutamiento de porcentaje e interés legal;
6. nombre desconocido sin ejecución de código;
7. argumentos inválidos y manejo explícito del error.

La construcción de prompts, la extracción JSON, el límite interno de ciclos de
tool calling y el rechazo fuera de dominio permanecen fuera de esta cobertura
mínima.

## Resultado observado

### Verificación estática

- Estado: **Parcialmente verificado**.
- Se localizaron cinco herramientas Java anotadas con `@Tool`.
- Se localizaron dos agentes que reciben herramientas mediante Spring AI.
- El ejecutor de cálculos usa una lista cerrada de nombres conocidos.
- No se localizaron herramientas de SQL, shell o comandos libres.

### Ejecución automatizada

- Estado: **Parcialmente verificado**.
- Las pruebas Java de `ValidationTools` y `CalculationTools` pasaron.
- Las pruebas de `CalculationExecutorService` pasaron para dos herramientas
  permitidas, un nombre no reconocido y argumentos inválidos.
- La suite completa del backend pasó con 18 pruebas.
- Las pruebas de construcción de prompts y prompt injection todavía no existen.

## Limitaciones

- No hay evidencia de esquema Pydantic, porque este backend no es Python.
- El límite verificado es del ejecutor de tareas; no limita todos los ciclos
  internos de tool calling.
- `validateAmount` aparece en un prompt, pero no existe en la clase de herramientas.
- El fallback de extracción devuelve valores nulos ante errores y aún no está
  cubierto por una prueba.
-   La tasa anual se carga desde `config/parametros.yaml` mediante
  `CalculationProperties`. El valor actual es `0.12` para conservar el
  comportamiento previo; debe validarse jurídicamente antes de usarlo en
  producción.
- La evidencia mínima de herramientas queda cubierta, pero el requisito
  completo de Semana 3 no se marca como cumplido por las limitaciones
  anteriores.