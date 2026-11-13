# Levantamiento de información

## 1. Propósito

Este documento sustenta el origen de las necesidades (N01 a N11) de `02-requisitos-v2.md`, cuyos IDs de necesidad y de historia de usuario (HU) son los de referencia. Indica de qué fuente proviene cada necesidad y qué límites tiene esa evidencia.

## 2. Qué exige la guía oficial del caso

La guía del proceso de titulación (documento «CASOS Carrera de Software, agosto 2026», archivo `CASOS_Carrera_de_Software_agosto_2026_IA_Generativa.pdf` https://uniandeseduec-my.sharepoint.com/:b:/g/personal/moiseslv56_uniandes_edu_ec/IQACXcEB9PrvRqt3zsl5Aq69AdatAeCT7BC6lGQ09TW10_U?e=5tOW6Q) 
define el **Caso N.º 02, Grupo 2: «Generador Inteligente de Reclamos de Consumo»**. Para la Fase 1 (Semana 1) pide:

> «Recopilar al menos 10 casos tipo de reclamos. Redactar propuesta, historias de usuario, requisitos y arquitectura, incluyendo el modelo de datos de los reclamos y la plantilla del documento de salida. Catalogar las fuentes.»

Para este caso, la guía no exige encuestas ni entrevistas: el levantamiento consiste en reunir casos tipo de reclamo y catalogar las fuentes. Ambos entregables están cubiertos (secciones 4 y 6).

## 3. Fuentes de las necesidades

Las necesidades provienen de tres fuentes. Ninguna se presenta como encuesta o entrevista a usuarios finales.

| Fuente | Qué aporta | Qué no aporta |
|---|---|---|
| **A. Guía oficial del Caso 02** (objetivo, fases e indicadores mínimos de calidad) | El origen de la mayoría de las capacidades del sistema | No es evidencia de demanda de usuarios finales |
| **B. Guía de la Defensoría del Pueblo en GOB.EC** (F02) | Qué debe presentar una persona consumidora y por qué canales | No es normativa ni prueba que los usuarios soliciten el software |
| **C. LODC (F01) y confirmación del abogado revisor** | Corrección jurídica de artículos, tipos de reclamo, cálculos y vías de acción | El abogado actúa como experto de dominio, no como demanda de usuarios |

## 4. Casos tipo de reclamo recopilados (guía, Fase 1)

Se recopilaron **18 casos tipo de reclamo** (la guía exige al menos 10), distribuidos en los tres tipos que cubre el sistema. Cada caso tiene su relato, artículos de fondo y de proceder, y resultado esperado en los archivos de `eval\relatos_tipos\`.

| Tipo de reclamo | Casos | Fuente |
|---|---|---|
| Vicios ocultos y calidad | 5: televisor con píxeles muertos; taladro con garantía en servicio técnico más de 30 días; silla defectuosa de un juego de comedor; juego de comedor completo exigido por una silla; vicio oculto que afecta seguridad e idoneidad | `eval\relatos_tipos\vicios_ocultos_calidad\casos.md` |
| Servicio técnico y custodia | 4: reparación defectuosa en garantía; deterioro del bien en el taller; repuestos usados sin autorización; negativa de suministro de repuestos | `eval\relatos_tipos\servicio_tecnico_custodia\casos.md` |
| Información, precios y facturación | 9: paneles solares como tecnología alemana; vehículo con 0 % de interés y cuota que sube; purificador sin el filtro anunciado; coaching con premio falso; negativa a emitir factura; transacción diferida sin comprobante; servicio sin desglose; honorarios sin información previa; honorarios distintos a lo informado | `eval\relatos_tipos\informacion_precios_facturacion\casos.md` |

La corrección jurídica de los 18 casos fue confirmada por el abogado revisor (preguntas 12 a 17 de la solicitud de revisión jurídica).

## 5. Fuente B: guía de la Defensoría del Pueblo

* **Documento:** «Atención de peticiones por posibles vulneraciones a los derechos humanos y de la naturaleza». Información proporcionada por la Defensoría del Pueblo (DPE) en GOB.EC.
* **URL:** https://www.gob.ec/dpe/tramites/atencion-peticiones-posibles-vulneraciones-derechos-humanos-naturaleza
* **Fecha de consulta:** 02 de noviembre de 2026 (registrada en `04-fuentes-v2.md`, fuente F02).
* **Uso en el sistema:** orientación institucional, no normativa. Sus 14 fragmentos se indexan en la colección `dpe_orientacion` y alimentan las Capas 3 y 5.

| ID | Apartado publicado en la guía | Qué indica (paráfrasis del equipo)                                                                                                                                 | Necesidad que apoya (insumo) | HU |
|---|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|---|
| **G01** | Requisito obligatorio: petición de la persona | La petición puede ser verbal o escrita y consiste en poner en conocimiento hechos o reclamar una intervención                                                      | N02: hechos que debe contener. N06: el documento generado es esa petición escrita | HU-02, HU-07 |
| **G02** | Cómo hago el trámite: canales | Existen varios canales: presencial, GOB.EC, Quipux y Contacto Ciudadano Digital                                                                                    | N04 | HU-06 |
| **G03** | Consentimiento previo, libre, inequívoco e informado | Para el trámite presencial se presenta la autorización para el tratamiento de datos personales                                                                     | N11: contexto; el sustento principal es la LOPDP y la guía del caso | HU-07 |
| **G04** | Análisis de competencia por la Defensoría | La institución analiza el contenido y las circunstancias de la petición para determinar su competencia y el tipo de trámite                                        | N04: la vía se presenta como orientación y no como competencia definitiva. N05 y N10: sin dictamen definitivo | HU-04, HU-06 |
| **G05** | Descripción general del trámite | Recibe reclamos por mala calidad o indebida prestación de servicios públicos o privados                                                                            | N01: tipo de situaciones que se consideran de consumo | HU-01 |
| **G06** | Trámite presencial: sin documentación de sustento | Si la persona no tiene la información requerida en ese momento, se le asesora y puede ser atendida después con la información completa                             | N02: señalar lo que falta y orientar cómo completarlo | HU-02, HU-03 |

## 6. Trazabilidad: necesidad, origen y respaldo

| Necesidad (resumen) | Origen en la guía del Caso 02 | Guía DPE (B) | Confirmación del abogado (C), pregunta de la solicitud | HU |
|---|---|---|---|---|
| N01 Delimitar si es relación de consumo | Fase 4: «relatos que no son un problema de consumo» | G05 | Preg. 3 y 4 | HU-01 |
| N02 Estructurar hechos e identificar faltantes | Objetivo: «evidencia faltante»; Fase 2: hechos extraídos; Fase 4: «relatos incompletos (el sistema pide lo que falta)» | G01, G06 | Preg. 5 | HU-02, HU-03 |
| N03 Vincular hechos con artículos verificables | Fase 3: «RAG para asociar cada hecho con los artículos aplicables, con cita» | | Preg. 1, 3, 4, 6 y 15 | HU-05, HU-06 |
| N04 Orientar la ruta de acción procedimental | Objetivo: «instancia competente» | G02, G04 | Preg. 6, 8 y 16 | HU-06 |
| N05 Clasificar el reclamo preliminarmente | Fase 3: `clasificar_reclamo(hechos)` y 80 % de precisión | G04 | Preg. 4, 12, 13 y 14 (validado para 3 de los 8 tipos) | HU-04 |
| N06 Generar documento DOCX | Objetivo y Fase 3: «borrador de reclamo formal descargable» desde plantilla | G01 | Preg. 9 | HU-07 |
| N07 Acción explícita del usuario | Indicadores mínimos: «ningún reclamo se registra ni se genera sin confirmación explícita»; Fase 5 |  | Pregunta 18 propuesta; pendiente de agregar a la solicitud | HU-08 |
| N08 Cálculos con funciones de código | Alcance de `01-propuesta-v2.md`, apartado 3.1 | | Sección C, pregunta 7 | HU-09 |
| N09 Recolección guiada de datos | Objetivo: «el agente guía la recolección de datos» | | Preg. 5 | HU-03 |
| N10 Informe de viabilidad y correcciones | Objetivo: «informe estructurado» con derechos posiblemente vulnerados | G04 | Preg. 8, 16 y 17 | HU-06 |
| N11 Protección de datos personales | Fase 3: «no almacena datos innecesarios»; Fase 5: cumplimiento de la LOPDP | G03 | Preg. 10 y 11 | HU-07 |


## 7. Límites y pendientes

* No se realizaron entrevistas ni encuestas a usuarios finales.
* Las necesidades provienen principalmente de la guía del caso. La corrección jurídica se respalda con la confirmación escrita del abogado revisor, no con evidencia de demanda de usuarios.
* N05 contempla ocho tipos de reclamo, pero la revisión jurídica cubre tres (vicios ocultos y calidad; servicio técnico y custodia; información, precios y facturación). Los cinco restantes no están validados por el abogado.
* La guía del caso menciona un archivo adjunto con facturas o capturas. El sistema actual no recibe archivos: las evidencias se registran de forma declarativa y el usuario las adjunta al documento generado. Esta diferencia debe declararse como limitación.
