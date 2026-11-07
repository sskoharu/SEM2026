# 01 · Propuesta del Proyecto

> **Proyecto:** Generador Inteligente de Reclamos de Consumo  
> **Familia de producto:** Procesador de documentos con RAG + agente de alcance controlado  
> **Seminario:** Ingeniería de Software con IA Generativa · UNIANDES  
> **Integrantes:** Mateo Sebastian Obregon Meza, Moises Omar Loor Vasquez, Fausto Fernando Gavilanes Procel

---

## 1. Problema Real

Los consumidores ecuatorianos enfrentan con frecuencia problemas de garantías, cobros indebidos, publicidad engañosa, servicios básicos y compras por internet. La Ley Orgánica de Defensa del Consumidor (LODC) reconoce derechos claros, pero la mayoría de personas no sabe qué derecho le asiste, qué información necesita reunir, qué camino puede seguir ni cómo redactar un reclamo formal. El resultado es que muchos reclamos legítimos nunca se presentan o se presentan sin sustento y se archivan.

> **Situación actual (sin el sistema):**  
> La persona busca en redes sociales, recibe consejos contradictorios, escribe un reclamo sin citar la norma ni indicar lo que pide, o simplemente desiste.

---

## 2. Usuarios

| Tipo | Perfil del Usuario | Necesidad Principal |
| :---: | :--- | :--- |
| **Principal** | **Consumidor sin formación jurídica** | Entender si su caso tiene respaldo en la LODC y obtener un reclamo listo para presentar. |
| **Secundario** | **Consumidor con poca experiencia digital** | Una guía paso a paso intuitiva que le solicite únicamente lo necesario, mediante preguntas fáciles de entender. |
| **Secundario** | **Estudiante o asesor de asociación de consumidores** | Un borrador estructurado con citas de artículos de la LODC que pueda revisar y validar rápidamente. |

---

## 3. Alcance

### 3.1 Lo que hace (In-Scope)
- **Filtrado inicial de reclamos:** Realiza un análisis inicial para no dar paso a relatos que estén fuera del contexto de la LODC; si el relato es de consumo pero insuficiente, solicita aclaraciones antes de continuar.
- **Clasificación inicial de reclamos:** Realiza un análisis relacional mediante busqueda vectorial entre el relato y los artículos  de la LODC recuperados del corpus y, con base en ello, determina a qué tipo de reclamo se asocia (ocho tipos).
- **Recolección guiada de la información:** El agente guía la recolección de datos mediante preguntas fáciles de entender, una a la vez, para alimentar el tipo de reclamo con la máxima información necesaria para el futuro reclamo, validando cada respuesta.
- **Análisis normativo:** Analiza el relato contra la LODC, citando los artículos de origen obtenidos del corpus mediante sus metadatos.
- **Evaluación de viabilidad:** Presenta un informe estructurado con los fundamentos normativos del caso, y permite al usuario aceptarlo o enviar correcciones en lote antes de continuar.
- **Propuestas de proceder:** Genera propuestas de proceder para satisfacer los reclamos avalados por los artículos de la LODC, con sus artículos procedimentales; el usuario elige la opción y completa el formulario correspondiente.
- **Cálculo determinista:** Calcula fechas límite,montos, y porcentajes mediante funciones Java; el modelo decide qué función invocar, pero los calculos deterministas son realizados por funciones del codigo.
- **Generación de documentos:** Genera un documento formal descargable (DOCX) inyectando los datos de la sesión en una plantilla estática mediante código Java, previa solicitud expresa del usuario. El modelo solo redacta las secciones de hechos y pretensiones.
- **Sesión persistente:** Conserva el estado de cada capa en Redis (vencimiento de 24 horas), de modo que la conversación puede pausarse y reanudarse.
- **Control de uso:** Limita las solicitudes por dirección IP para evitar el consumo excesivo de tokens del modelo.

### 3.2 Lo que NUNCA hará (Alcance Negativo / Out-of-Scope)
- **No es asesoría legal:** Orienta e informa; no reemplaza a un abogado ni garantiza el resultado del reclamo.
- **No predice resultados ni indemnizaciones:** No predice si el reclamo "ganará" ni calcula indemnizaciones por daños y perjuicios.
- **No gestiona trámites ante instituciones:** No presenta ni envía el reclamo en nombre del usuario a ninguna entidad pública o privada. Solo lo prepara como documento descargable.
- **No atiende materias fuera de la LODC:** Excluye materias laboral, penal, arrendamientos, tributaria o familiar; su filtro inicial descarta los relatos ajenos al consumo.
- **No inventa artículos:** Los artículos aplicables y procedimentales se obtienen del corpus de la LODC mediante sus metadatos, y el documento se construye sobre una plantilla estática. Si no se recuperan fragmentos normativos tras varios reintentos, el sistema solicita aclaración en lugar de continuar.
- **Protección de datos de identificación:** No envía al modelo generativo los datos de identificación del consumidor ni del proveedor (nombre, cédula, contacto); se solicitan solo al generar el documento y se inyectan en la plantilla mediante código.

---

## 4. Riesgos Identificados

| ID | Riesgo Identificado | Nivel de Impacto | Estrategia de Mitigación |
| :---: | :--- | :---: | :--- |
| **R1** | **Normativa desactualizada:** La LODC fue reformada varias veces; el texto indexado podría no ser el vigente (ej.: régimen sancionatorio). | 🔴 **Alto**<br>*(Citar norma derogada)* | Cargar el corpus desde la versión consolidada oficial y actualizarlo cuando cambie la norma; el equipo es responsable de su mantenimiento. |
| **R2** | **Alucinación de artículos o sanciones:** El modelo inventa normas inexistentes. | 🔴 **Alto** | Los artículos aplicables y procedimentales se obtienen de los metadatos del corpus vectorial y no de la generación libre; si no se recuperan fragmentos tras 3 reintentos, se pide aclaración al usuario. |
| **R3** | **Confusión con asesoría jurídica:** El usuario interpreta el informe como patrocinio legal formal. | 🔴 **Alto** | Aviso preventivo visible en la interfaz, tono estrictamente orientativo y derivación a la Defensoría del Pueblo o consultorios jurídicos. |
| **R4** | **Fuga de datos personales (PII):** Exposición de cédula, dirección o teléfono hacia el LLM o logs del sistema. | 🔴 **Alto**<br>*(LOPDP)* | Los datos de identificación se solicitan solo al generar el documento, se inyectan en la plantilla por código y no se almacenan en la sesión ni se envían al modelo. Riesgo residual: el relato libre podría contener datos personales. |
| **R5** | **Inyección de prompt y relatos ajenos al dominio:** El usuario incluye instrucciones maliciosas o contenido fuera del dominio en el texto libre. | 🟡 **Medio** | El filtro inicial descarta relatos ajenos a la LODC y las instrucciones del sistema delimitan el rol del agente; se recomienda ampliar con pruebas de inyección. |
| **R6** | **Regulaciones sectoriales especializadas:** Telecomunicaciones, energía o banca poseen normativas propias no cubiertas en el corpus base. | 🟡 **Medio** | El corpus se limita a la LODC; el informe se presenta como orientación preliminar y se advierte que normativas sectoriales requieren verificación. |
| **R7** | **Agotamiento de cuotas / rate-limiting:** La capa gratuita del modelo se agota o aplica límites estrictos de peticiones. | 🟡 **Medio** | Reintentos con backoff exponencial, límite de solicitudes por IP (ventana deslizante en Redis) y proveedor compatible con OpenAI configurable mediante variables de entorno. |

---

## 5. Modelo Generativo y Stack Tecnológico

| Componente | Tecnología Seleccionada | Justificación Técnica y de Proyecto |
| :--- | :--- | :--- |
| **Backend** | `Spring Boot` + `Spring AI` (Java 21) | Alternativa justificada al stack base del seminario; soporta herramientas Java deterministas (`@Tool`), salidas estructuradas tipadas y una arquitectura por capas con máquinas de estado. |
| **Modelo Generativo** | `GPT` (`gpt sol 5.6`, mediante un cliente compatible con OpenAI) | Modelo de baja latencia y costo reducido, con soporte de salidas estructuradas y de llamadas a herramientas; configurado con temperatura baja para respuestas consistentes. |
| **Embeddings** | `text-embedding-3-large` | Captura semánticamente el lenguaje jurídico y el coloquial; se integra con la misma cuenta del modelo generativo. |
| **Base Vectorial** | `Qdrant` (persistente) | ligero, rápido y permite filtrado eficiente por metadatos (capítulo, artículo, tipo de reclamo). |
| **Estado de sesión** | `Redis` | Almacena el estado de cada capa y de la sesión con vencimiento de 24 horas |
| **Frontend** | `React` + `TypeScript` (`Vite`) | Formulario interactivo guiado paso a paso con informe estructurado por secciones; arquitectura desacoplada comunicada vía API REST con el backend. |
| **Despliegue** | `Docker` + `Render` | Imagen contenedorizada multietapa con configuración por variables de entorno; adecuado para prototipos. |
| **Control de Versiones** | `GitHub` | Acceso de lectura para el docente desde la semana 1; cumplimiento estricto de la rúbrica de commits, ramas por funcionalidad y revisiones cruzadas vía PR. |
