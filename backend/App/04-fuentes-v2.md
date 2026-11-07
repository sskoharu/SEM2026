# Catálogo de fuentes

Consulta y descarga original: **15 de octubre de 2026**. Revisión: 2 de noviembre de 2026. El corpus normativo del sistema se limita a una única fuente, la **Ley Orgánica de Defensa del Consumidor (LODC)**, indexada en la base vectorial **Qdrant**. Además, existe una colección auxiliar de orientación institucional de la Defensoría del Pueblo (DPE, F02), que no es normativa. Las reglas de cálculo de la Capa 4 se derivan de artículos de la LODC y se guardan fuera de Qdrant (sección 6). Se verificaron origen y formato; la **vigencia consolidada permanece pendiente de confirmación oficial**.

## 1. Inventario del corpus

| ID | Título y emisor normativo | Alojamiento y URL | Publicación documentada | Versión / vigencia | Formato y copia local                                                                                                           | Uso en el sistema |
|---|---|---|---|---|---------------------------------------------------------------------------------------------------------------------------------|---|
| F01 | Ley Orgánica de Defensa del Consumidor; Congreso Nacional | [EMAPASR](https://emapasr.gob.ec/htm/archivos/LOTAIP_2021/link/FEBRERO/LEY%20ORGANICA%20DE%20DEFENSA%20DEL%20CONSUMIDOR.pdf) | Ley 2000-21; R.O. Suplemento 116, 10-07-2000, indicado en copia | Vigente con reformas (Ley No. 2000-21, R.O. Suplemento 116, 10-07-2000; última reforma consolidada aplicable). | PDF, 34 páginas; `data/raw/F01-lodc.pdf`. Texto estructurado en 10 archivos JSON en `backend/App/src/main/resources/data/lodc/` | Único corpus normativo del sistema (RAG) |
| F02 | Orientación sobre atención de peticiones y trámite ante la Defensoría del Pueblo del Ecuador; GOB.EC | [gob.ec/dpe](https://www.gob.ec/dpe/tramites/atencion-peticiones-posibles-vulneraciones-derechos-humanos-naturaleza/) | No documentada | Vigente (Guía institucional activa), consultada el 02-11-2026 en el portal oficial GOB.EC. | Texto estructurado en `backend/App/src/main/resources/data/dpe/dpe_orientacion.json` (14 fragmentos)                                        | Orientación institucional para las Capas 3 y 5; no es fuente normativa |
## 2. Corpus indexado

- **Contenido:** 64 fragmentos correspondientes a 57 artículos distintos de la LODC, organizados en 10 archivos JSON por tema (principios generales, regulación de publicidad, información básica comercial, responsabilidades del proveedor, servicios públicos, protección contractual, prácticas prohibidas, infracciones y sanciones, defensoría y procedimiento, disposiciones generales y disposiciones finales). Algunos artículos se dividen en más de un fragmento (por ejemplo, por numerales).
- **Estructura de cada fragmento:** `id` semántico (por ejemplo, `lodc_ley_art_17`), texto del artículo (`document`) y metadatos.
- **Metadatos por fragmento:** `documento_tipo`, `ley`, `numero_articulo`, `capitulo`, `tema_principal`, `tipo_reclamo_asociado` (uno o más de los ocho tipos de reclamo), `parametros_requeridos` (datos que la Capa 2 debe recolectar) y `articulos_procedimentales_asociados` (artículos que la Capa 3 usa para proponer el proceder).
- **Uso de los metadatos:** la Capa 1 los emplea para clasificar el reclamo y fijar los artículos aplicables; la Capa 2, para determinar qué datos preguntar; la Capa 3, para recuperar los artículos procedimentales mediante filtro por `numero_articulo`; la Capa 4, para emparejar cada variable por calcular con su regla mediante el tipo de reclamo y el artículo base (sección 6).
- **Colección DPE:** los 14 fragmentos de F02 se indexan en una colección separada (`dpe_orientacion`), con metadatos `documento_tipo` (`ORIENTACION_DPE`), `fuente`, `url`, `seccion`, `capa_destino` y `articulos_lodc_relacionados`.

## 3. Fragmentación e indexación

- **Unidad de fragmentación:** el artículo, conservando su capítulo y tema; los artículos extensos se dividen en fragmentos con su número de artículo.
- **Embeddings:** `text-embedding-3-large` de OpenAI (3072 dimensiones), mediante Spring AI. Si se cambia el modelo, se deben borrar las colecciones de Qdrant y reingestar.
- **Almacenamiento:** Qdrant (colecciones `lodc_leyes_vector` y `dpe_orientacion`). Cada fragmento recibe un identificador UUID determinista derivado de su `id` semántico, de modo que reingestar el corpus actualiza los fragmentos existentes en lugar de duplicarlos; el `id` original se conserva en los metadatos (`original_id`).
- **Índices:** se crea un índice de payload sobre `numero_articulo` para permitir el filtrado exacto por artículo.
- **Búsqueda:** por similitud vectorial con umbral de 0,30 y filtrado por metadatos.
