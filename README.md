# Generador Inteligente de Reclamos de Consumo

UNIANDES, Carrera de Software. **Caso 02, Grupo 2. Fase 1: definición del problema y diseño.** Revisión: 16 de octubre de 2026. Sustentación: 7 de noviembre de 2026.

Integrantes: Mateo Sebastian Obregon Meza, Moises Omar Loor Vasquez y Fausto Fernando Gavilanes Procel.

## Estado y propósito

Se analizaron el Caso 02 y la guía originales y se simplificó la documentación. **No hay aplicación implementada, pruebas de producto ejecutadas ni despliegue.** El sistema permitirá organizar un relato de consumo, consultar normativa y preparar un reclamo revisable. No constituye asesoría jurídica definitiva.

## Alcance mínimo

Relato o archivo legible, hechos estructurados, datos faltantes, cálculos deterministas, clasificación orientativa, informe con fuentes e instancia respaldada, borrador DOCX y registro interno. Generar y registrar requerirán confirmaciones separadas. No habrá envío a autoridades, mapa reversible de PII, ni segundo formato documental.

Se conservan React/TypeScript/Vite, Java/Spring Boot/Spring AI, DeepSeek-R1 propuesto, embeddings multilingües, ChromaDB y SQLite. La guía recomienda un stack base, admite equivalentes y exige justificar alternativas. Se cambió el lenguaje; compatibilidad, acceso a R1 y aceptación docente siguen pendientes.

## Documentación

| Evidencia | Archivo |
|---|---|
| Problema, objetivos, alcance y decisiones | [Propuesta](docs/01-propuesta.md) |
| 11 historias, 31 criterios y RNF medibles | [Requisitos](docs/02-requisitos.md) |
| Diagrama con frontend, backend, modelo, base vectorial, herramienta del agente y fuentes; descripción del flujo de una consulta de principio a fin | [Arquitectura](docs/03-arquitectura.md) |
| Catálogo con copias oficiales y límites | [Fuentes](docs/04-fuentes.md) |
| 15 preguntas documentales verificadas | [Preguntas](docs/evidencias/semana-1/01-preguntas-reales.md) |

## Estructura y ejecución

Se conservan `app/`, `frontend/`, `scripts/`, `config/`, `data/raw/`, `data/processed/`, `eval/`, `tests/`, `docs/` y `docs/evidencias/semana-1/`. Las carpetas de código contienen marcadores, no módulos funcionales. No se inventan comandos de instalación o ejecución.

`.env.example` no contiene secretos, pero conserva valores de ejemplo anteriores que deberán alinearse al implementar. No representa configuración vigente de R1. Se mantiene `.gitignore`; antes de generar datos privados se revisarán sus exclusiones.

## Fuentes, privacidad y evaluación

Se conservaron cuatro recursos oficiales descargados. Vigencia consolidada y OCR de LOPDP siguen pendientes; no están habilitados para RAG. Se usarán datos ficticios en desarrollo y pruebas, claves solo backend, minimización de datos y confirmación del consumidor.

Metas futuras: >=12/15 con categoría y artículo correctos, cero generación/registro sin permiso y cero hechos inventados en 15 borradores. No se midieron. La guía exige también evaluación general y pruebas desplegadas en fases posteriores.

Se utilizó IA para apoyar documentación y verificaciones documentales; no se atribuyen a ella fuentes jurídicas ni entrevistas. Los nombres fueron confirmados por el usuario; contribuciones reales y acceso docente siguen por acreditar. No se hicieron commits ni push durante esta simplificación.
