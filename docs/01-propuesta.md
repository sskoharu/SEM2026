# 01 · Propuesta del Proyecto

> **Proyecto:** Generador Inteligente de Reclamos de Consumo  
> **Familia de producto:** Procesador de documentos con RAG + agente de alcance controlado  
> **Seminario:** Ingeniería de Software con IA Generativa · UNIANDES  
> **Integrantes:** Mateo Sebastian Obregon Meza, Moises Omar Loor Vasquez, Fausto Fernando Gavilanes Procel  

---

## 1. Problema Real

Los consumidores ecuatorianos enfrentan con frecuencia problemas de garantías, cobros indebidos, publicidad engañosa, servicios básicos y compras por internet. La Ley Orgánica de Defensa del Consumidor (LODC) reconoce derechos claros, pero la mayoría de personas no sabe qué derecho le asiste, qué prueba necesita, a quién acudir primero ni cómo redactar un reclamo formal. El resultado es que muchos reclamos legítimos nunca se presentan o se presentan sin sustento y se archivan.

> **Situación actual (sin el sistema):**  
> La persona busca en redes sociales, recibe consejos contradictorios, escribe un reclamo sin citar la norma ni indicar lo que pide, o simplemente desiste.

---

## 2. Usuarios

| Tipo | Perfil del Usuario | Necesidad Principal |
| :---: | :--- | :--- |
| **Principal** | **Consumidor sin formación jurídica** | Entender si su caso tiene respaldo en la LODC y obtener un reclamo listo para presentar. |
| **Secundario** | **Consumidor con poca experiencia digital** | Una guía paso a paso intuitiva que le solicite únicamente lo necesario. |
| **Secundario** | **Estudiante o asesor de asociación de consumidores** | Un borrador estructurado con citas legales precisas que pueda revisar y validar rápidamente. |

---

## 3. Alcance

### 3.1 Lo que hace (In-Scope)
- **Análisis normativo:** Analiza el relato contra la LODC y su Reglamento, citando el artículo de origen con precisión.
- **Evaluación de evidencia:** Lista la evidencia presentada y la faltante según la categoría específica del reclamo.
-  **Cálculo de plazos determinista:** Calcula plazos legales aplicables mediante funciones deterministas (ej.: 3 días para devolución, 90 días en reparaciones, 12 meses de prescripción en acciones civiles).
-  **Ruta de escalamiento:** Indica la instancia idónea a la que acudir primero y el flujo de escalamiento institucional.
-  **Generación de documentos:** Genera un borrador formal descargable (DOCX/PDF) a partir de una plantilla estandarizada.
-  **Registro controlado:** Registra el reclamo (guardado interno) únicamente tras la confirmación explícita del usuario.

### 3.2 Lo que NUNCA hará (Alcance Negativo / Out-of-Scope)
-  **No es asesoría legal:** Orienta e informa; no reemplaza a un abogado ni garantiza el resultado del reclamo.
-  **No predice resultados ni indemnizaciones:** No predice si el reclamo "ganará" ni calcula indemnizaciones por daños y perjuicios.
-  **No gestiona trámites ante instituciones:** No presenta ni envía el reclamo en nombre del usuario a ninguna entidad pública o privada. Solo lo prepara y guarda internamente.
-  **No atiende materias fuera de la LODC:** Excluye materias laboral, penal, arrendamientos, tributaria o familiar (las reconoce y deriva oportunamente).
-  **No alucina artículos ni sanciones:** Si no encuentra respaldo expreso en las fuentes indexadas, lo declara con total transparencia y orienta la abstención.
-  **No vulnerable a inyecciones:** No obedece instrucciones contenidas en adjuntos o relatos que pretendan alterar el comportamiento o directivas del agente.
-  **Protección estricta de privacidad (PII):** No envía datos personales sensibles (cédula, teléfono, dirección) al modelo generativo.

---

## 4. Riesgos Identificados

| ID | Riesgo Identificado | Nivel de Impacto | Estrategia de Mitigación |
| :---: | :--- | :---: | :--- |
| **R1** | **Normativa desactualizada:** La LODC fue reformada varias veces; el texto indexado podría no ser el vigente (ej.: régimen sancionatorio). |  **Alto**<br>*(Citar norma derogada)* | Descargar la versión consolidada oficial, registrar fecha de vigencia por artículo, marcar `estado=derogado` y excluirlo de la recuperación. |
| **R2** | **Alucinación de artículos o sanciones:** El modelo inventa normas inexistentes. | 🔴 **Alto** | Citas obligatorias con `id` de fragmento, validación de que el artículo citado exista en el índice vectorial y política de abstención probada. |
| **R3** | **Confusión con asesoría jurídica:** El usuario interpreta el informe como patrocinio legal formal. | 🔴 **Alto** | Aviso preventivo visible en la interfaz, tono estrictamente orientativo y derivación a la Defensoría del Pueblo o consultorios jurídicos. |
| **R4** | **Fuga de datos personales (PII):** Exposición de cédula, dirección o teléfono hacia el LLM o logs del sistema. | 🔴 **Alto**<br>*(LOPDP)* | Anonimización previa: sustituir datos personales por marcadores antes de invocar al modelo; completarlos en la plantilla local por código; logs sin PII. |
| **R5** | **Inyección de prompt desde adjuntos:** Facturas o documentos adjuntos con texto malicioso para evadir instrucciones. |  **Medio** | Contenido de adjuntos delimitado como dato no confiable (*untrusted data*) y pruebas automatizadas de inyección en la fase de evaluación. |
| **R6** | **Regulaciones sectoriales especializadas:** Telecomunicaciones, energía o banca poseen normativas propias no cubiertas en el corpus base. |  **Medio** | Fichas de instancia en configuración marcadas como "por verificar"; derivar explícitamente cuando el caso corresponda a un sector especial. |
| **R7** | **Agotamiento de cuotas / rate-limiting:** La capa gratuita del modelo se agota o aplica límites estrictos de peticiones. |  **Medio** | Reintentos con backoff exponencial, caché semántica de recuperación y soporte de proveedores alternativos configurables en `.env`. |
| **R8** | **Fallas en OCR / Visión:** Ilegibilidad o errores en capturas fotográficas borrosas de comprobantes. | 🟡 **Medio** | Paso obligatorio de previsualización para que el usuario confirme o corrija los datos extraídos antes de procesarlos. |

---

## 5. Modelo Generativo y Stack Tecnológico

| Componente | Tecnología Seleccionada | Justificación Técnica y de Proyecto |
| :--- | :--- | :--- |
| **Backend** | `Spring Boot` + `Spring AI` | Stack base del seminario; `BeanOutputConverter` de Spring AI combinado con Java Records/POJOs garantiza validación y tipado estricto de entradas y salidas estructuradas. |
| **Modelo Generativo** | `DeepSeek-R1` | Especializado en razonamiento profundo (*Chain-of-Thought*); ideal para análisis legal riguroso, desglosando lógicamente el relato frente a los artículos de la LODC y evitando saltos de asociación en el RAG. |
| **Embeddings** | `sentence-transformers` (Multilingüe local) | Optimizado para capturar semánticamente tanto el lenguaje jurídico como el coloquial ecuatoriano; opera localmente sin costo ni dependencia de cuotas de API externa. |
| **Base Vectorial** | `ChromaDB` (Persistente en disco) | Stack oficial del seminario; ligero, rápido y permite filtrado eficiente por metadatos (vigencia, capítulo, artículo). |
| **Frontend** | `React` + `TypeScript` (`Vite`)<br>*(Alt.: Streamlit)* | Formulario interactivo guiado paso a paso con informe estructurado por secciones; arquitectura desacoplada comunicada vía API REST con el backend. |
| **Despliegue** | `Render` / `Railway` / `Hugging Face Spaces` | Planes gratuitos estables y adecuados para prototipos académicos y evaluación continua. |
| **Control de Versiones** | `GitHub` | Acceso de lectura para el docente desde la semana 1; cumplimiento estricto de la rúbrica de commits, ramas por integrante y revisiones cruzadas vía PR. |
