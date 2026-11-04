# RAG, fragmentación y revisión del corpus

El corpus actual contiene diez JSON y sesenta IDs únicos. Cada objeto es una unidad jurídica: artículo, numeral o disposición. No hay corte ciego por caracteres ni solapamiento adicional. Los artículos extensos conservan su unidad y referencia; cualquier subdivisión futura debe conservar el artículo original. No se implementa extracción automática de los PDF originales: esa transformación queda pendiente de trazabilidad.

LodcDataIngestionRunner valida archivos e IDs, conserva texto original, document_id, original_id y source_file. Los JSON mantienen además la URL y versión de la fuente de trabajo, pero no las convierten en aprobación jurídica. No altera fechas ni añade vigencia inventada. Si falta un campo, lo declara pendiente. La ingesta no se ejecuta automáticamente y solo inserta documentos aprobados. No elimina puntos, índices ni colecciones. Un fallo de archivo cancela la escritura completa.

CorpusPolicy exige reviewed=true, status=VIGENTE, URL HTTPS y vigencia no vacía ni marcada como no verificada. Es una decisión del revisor humano registrada en metadatos; el código no certifica que esa declaración sea jurídicamente correcta. Los sesenta fragmentos entregados no cumplen esa revisión: se excluyen como fundamento operativo. No se añadió ni aprobó el reglamento por inferencia.

CorpusCatalog complementa Qdrant con coincidencia léxica y búsqueda exacta de artículos. VectorSearchTool combina resultados por ID, aplica la política de aprobación y valida números antes de crear filtros. El umbral y top-k se cargan desde config/parametros.yaml. Un score no es un porcentaje de precisión.

Los fragmentos pasan como datos delimitados a un mensaje separado de las instrucciones de sistema. CitationPolicy comprueba quote exacto + ID. Las opciones también exigen sourceId, supportingQuote y artículo coincidente. La revisión semántica de pertinencia y la aplicación al hecho concreto siguen siendo humanas.

## Cierre jurídico pendiente

Un responsable debe cotejar la fuente oficial, su versión/reformas, URL, fecha y fragmento, registrar quién revisó y con qué evidencia y solo entonces habilitar sus metadatos. Una huella de archivo identifica bytes, no prueba vigencia. No se habilita una fuente para mejorar artificialmente las métricas.
