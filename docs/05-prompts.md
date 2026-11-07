# Estado de los prompts y su comparación

Las tablas originales son antecedentes documentales, no resultados de esta corrección. Los agentes Java construyen entradas según su etapa y los recursos system_v1/v2/v3 se conservan como historial. La configuración de esta base identifica el chat como `gpt-5.6-sol` y los embeddings como `text-embedding-3-large` mediante Spring AI. Esto identifica la configuración versionada. La revisión y el proveedor efectivos de Render no se deducen de los registros HTTP. El uso de Gemini en el informe académico pertenece a una versión anterior. No se les atribuyen métricas reales actuales.

# Comparación de versiones del prompt

Caso 02 · Generador Inteligente de Reclamos de Consumo


## Versiones

- `src/main/resources/prompts/system_v1.st`: versión base. Define el rol del asistente, el formato JSON, las restricciones generales y la instrucción de usar `null` o abstenerse cuando no existan datos suficientes.

- `src/main/resources/prompts/system_v2.st`: agrega reglas explícitas por campo, manejo de fechas y montos, abstención detallada, protección frente a instrucciones maliciosas y reglas para casos fuera de dominio.

- `src/main/resources/prompts/system_v3.st`: agrega un procedimiento de decisión ordenado, ejemplos completos, criterios más precisos para seleccionar el estado y un autochequeo antes de responder.

## Conjunto de prueba (10 entradas)

Incluyan dos entradas que el sistema no debería poder procesar y una fuera de dominio.

| # | Entrada | Tipo (normal / no procesable / fuera de dominio) |
|---|---|---|
| 1 |Producto defectuoso con información completa  | Normal |
| 2 |Cobro duplicado | Normal|
| 3 |Servicio intermitente con fechas ambiguas  |Normal |
| 4 |Entrega incompleta y evidencia mencionada |Normal |
| 5 |Varios montos contradictorios  |Normal |
| 6 |Relato coloquial  |Normal |
| 7 |Descripción demasiado genérica  |No procesable |
| 8 |Intento de manipulación sin caso real  |No procesable |
| 9 |Reclamo laboral |Fuera de dominio |
| 10 |Producto no entregado |Normal |

## Resultados

Marquen ✔ o ✘ en cada columna: ¿responde lo pedido?, ¿inventa?, ¿respeta formato y tono?

| # | system_v1: correcta | system_v1: inventa | system_v1: formato | system_v2: correcta | system_v2: inventa | system_v2: formato | system_v3: correcta | system_v3: inventa | system_v3: formato |
|---|---|---|---|---|---|---|---|---|---|
| 1 |✔ |✘|✔ |✔ |✘ |✔ |✔ |✘ |✔ |
| 2 |✔|✘ |✔ |✔ |✘ | ✔| ✔|✘ |✔ |
| 3 | ✔|✘ |✔ |✔ |✘ |✔ |✔ |✘ |✔ |
| 4 |✔ |✘ |✔ |✔ |✘ |✔ |✔ |✘ |✔ |
| 5 |✔ | ✘|✘ | ✔|✘ |✘ |✔ |✘ |✔ |
| 6 |✔ | ✘|✔ |✔ |✘ | ✔|✔ |✘ |✔ |
| 7 | ✔|✘ |✔ |✔ |✘ |✔ |✔ |✘ |✔ |
| 8 |✔ |✘ |✔ |✔ |✘ |✔ | ✔|✘ | ✔|
| 9 | ✔|✘ |✔ |✔ |✘ |✔ |✔ |✘ |✔ |
| 10 |✔ |✘ |✔ |✔ |✘ |✔ | ✔|✘ |✔ |

## Conclusión

- Versión elegida: `system_v3`. Incluye más reglas y ejemplos,
  pero se eligió principalmente porque maneja mejor la ambigüedad,
  los datos faltantes y las contradicciones, produciendo respuestas
  más completas y confiables.
- Temperatura elegida y por qué: Se seleccionó una temperatura de 0.1 porque el sistema realiza extracción factual estructurada a partir de un formulario. No se requiere creatividad ni diversidad en las respuestas; se necesita consistencia, tono neutral y fidelidad a los datos recibidos. La temperatura baja reduce la variabilidad y el riesgo de paráfrasis o inferencias no sustentadas. Esta configuración no garantiza por sí sola que el modelo no invente información, por lo que también se utilizan reglas explícitas de abstención y pruebas con casos ambiguos.
- Máximo de tokens y por qué: `2048`. La respuesta debe contener un objeto JSON
  estructurado, incluyendo datos faltantes y motivo de abstención en casos
  ambiguos o fuera de dominio. Este límite evita truncamientos sin permitir
  respuestas innecesariamente extensas.
- Qué aprendimos: Aprendimos que es necesario verificar y comparar diferentes system prompts, porque el comportamiento del sistema depende directamente de las reglas, restricciones y ejemplos que se establecen. Las pruebas permitieron reconocer qué instrucciones ayudan a extraer información correctamente, evitar invenciones y manejar datos ambiguos o insuficientes.
