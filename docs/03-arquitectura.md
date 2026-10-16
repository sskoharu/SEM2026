**Diagrama (Mermaid o imagen) con frontend, backend, modelo, base vectorial, herramientas del agente y fuentes**:  
[![image1.png](https://i.postimg.cc/5ynP6mwc/image1.png)](https://postimg.cc/7JJ1R0sX)
**2\. Descripción del flujo de una consulta de principio a fin**

El ciclo de vida de un caso dentro del sistema sigue este proceso secuencial, separando estrictamente la interpretación semántica (IA) de la lógica de negocio procesal (Java):

1. **Recepción del caso:** El usuario ingresa el relato de su problema y sube la evidencia (adjuntos) desde el Frontend.
2. **Anonimización:** El Backend (Spring Boot) recibe la petición y reemplaza los datos personales (PII) por marcadores para proteger la identidad antes de enviar datos a la IA.
3. **Extracción estructurada:** El Agente envía el texto al Modelo (DeepSeek-R1) para extraer los hechos, fechas, montos y el nombre del proveedor en formato estructurado (JSON mapeado con BeanOutputConverter).
4. **Verificación de evidencia:** El Agente utiliza la herramienta evidencia_faltante para comprobar si el relato y los adjuntos cumplen con los requisitos mínimos del tipo de reclamo. Si falta información, pregunta al usuario antes de avanzar.
5. **Búsqueda semántica (RAG):** Con los hechos consolidados, el Agente invoca buscar_normativa para consultar en ChromaDB (Base Vectorial) los fragmentos exactos de la Ley Orgánica de Defensa del Consumidor (Fuentes) que aplican al caso.
6. **Razonamiento legal:** El Modelo analiza los hechos frente a los fragmentos de la ley recuperados e identifica los derechos posiblemente vulnerados.

**7\. Ejecución de Reglas Deterministas (Código vs. Alucinación):** Dado que los LLMs son propensos a fallar en cálculos exactos, el Agente delega las decisiones procesales y matemáticas exclusivamente al backend en Java. Se ejecutan herramientas estáticas:

- calcular_plazos: Usa java.time.LocalDate para sumar días exactos desde el evento y define si el reclamo está vigente o prescrito.
- resolver_instancia: Define la ruta jerárquica estricta (ej. Proveedor → Arcotel → Defensoría).
- promedio_consumo: Realiza el cálculo matemático en BigDecimal de los últimos 6 meses para cobros excesivos. _El modelo no inventa fechas ni rutas de atención; el código las impone basándose en archivos de configuración YAML._

**8\. Validación del usuario (Human-in-the-loop y recolección de datos):** El Backend consolida todo (hechos extraídos por la IA + artículos del RAG + plazos exactos del código Java) y lo presenta en el Frontend como un "Informe Estructurado". Aquí ocurre la supervisión obligatoria: el usuario revisa, corrige si es necesario y confirma que la información es cierta. _Importante:_ Este informe visual no es el documento final, sino que actúa como la "lista de ingredientes validados" (los datos perfectos y confirmados) que habilitarán la redacción legal.

**9\. Confirmación y Generación (Plantillas estáticas, cero inventos):** Al presionar "Registrar y generar borrador", **el LLM no redacta el documento legal desde cero**. El Agente llama a la función Java generar_documento, la cual toma una plantilla legal prediseñada (un archivo .docx fijo y aprobado en el servidor) y simplemente inyecta los datos validados del Paso 8 en las variables correspondientes (ej. reemplaza \[HECHOS\] con el relato y \[PLAZO_CALCULADO\] con la fecha de Java). Esto devuelve un documento formal a prueba de alucinaciones, garantizando el indicador de calidad: "cero hechos inventados", listo para su descarga e impresión.