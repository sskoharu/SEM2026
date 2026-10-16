# Catálogo de fuentes

Consulta y descarga original: **15 de octubre de 2026**. Se conservan los recursos y el manifiesto sin alterar bytes o procedencia. Se verificaron origen y formato; la **vigencia consolidada permanece pendiente**. Las cuatro fuentes siguen deshabilitadas para el futuro RAG hasta revisión.

## 1. Inventario

| ID | Título y emisor normativo | Alojamiento y URL | Publicación documentada | Versión / vigencia | Formato y copia local | Uso previsto |
|---|---|---|---|---|---|---|
| F01 | Ley Orgánica de Defensa del Consumidor; Congreso Nacional | [EMAPASR](https://emapasr.gob.ec/htm/archivos/LOTAIP_2021/link/FEBRERO/LEY%20ORGANICA%20DE%20DEFENSA%20DEL%20CONSUMIDOR.pdf) | Ley 2000-21; R.O. Suplemento 116, 10-07-2000, indicado en copia | Copia histórica; última reforma y consolidación no confirmadas | PDF, 34 páginas; `data/raw/F01-lodc.pdf` | Corpus sustantivo solo después de revisión |
| F02 | Reglamento General a la Ley Orgánica de Defensa del Consumidor; Presidencia de la República | [EMAPASR](https://emapasr.gob.ec/htm/archivos/LOTAIP_2021/link/FEBRERO/REGLAMENTO%20LEY%20ORGANICA%20DE%20DEFENSA%20DEL%20CONSUMIDOR.pdf) | Decreto Ejecutivo 1314, R.O. 287, 19-03-2001, mencionado en texto | Incluye reformas anexas; consolidación y fechas anexas requieren contraste | PDF, 17 páginas; `data/raw/F02-reglamento-lodc.pdf` | Corpus sustantivo por artículo, con reformas separadas |
| F03 | Usuarios y consumidores; Defensoría del Pueblo del Ecuador | [DPE](https://www.dpe.gob.ec/usuarios-y-consumidores/) | Sin fecha de publicación confirmada; no confundir metadatos web con vigencia | Información institucional con nombres de entidades históricos; actualizar procedimientos antes de uso | HTML; `data/raw/F03-dpe.html` | Orientación e investigación documental; no norma con rango de ley |
| F04 | Ley Orgánica de Protección de Datos Personales; Asamblea Nacional | [Ministerio de Telecomunicaciones](https://www.telecomunicaciones.gob.ec/wp-content/uploads/2021/06/Ley-Organica-de-Datos-Personales.pdf) | R.O. Quinto Suplemento 459, 26-05-2021, verificado en portada | Edición de publicación; reformas posteriores y normativa complementaria por verificar | PDF, 70 páginas; `data/raw/F04-lopdp.pdf` | Referencia de privacidad; separada del fundamento sustantivo de consumo |

## 2. Disponibilidad y límites

- F01: PDF de 34 páginas, texto extraíble; copia histórica con errata de título y encabezados. No se certifican reformas o competencia actual.
- F02: PDF de 17 páginas, texto extraíble; reformas anexas con fechas discordantes que requieren contraste. La publicación del reglamento no se confunde con la de la ley.
- F03: HTML institucional con preguntas verificadas; puede conservar nombres de instituciones desactualizados. Sirve como evidencia documental, no directorio vigente aprobado.
- F04: PDF de 70 páginas legible visualmente; el cuerpo necesita OCR y revisión antes de usarlo como texto.

El manifiesto `evidencias/semana-1/05-manifiesto-fuentes.json` conserva URL, fecha, tamaño y SHA-256. Una huella identifica bytes, no prueba vigencia. Se descargó F01 desde EMAPASR tras fallar la alternativa de Superintendencia de Bancos por certificado TLS; no se desactivó TLS. No se declara corpus listo para ingesta.

## 3. Fragmentación prevista

LODC y reglamento: artículo como unidad, conservando capítulo y disposición. Si es extenso, dividir por apartados con referencia al artículo completo. DPE: sección o pregunta/respuesta. LOPDP: revisar OCR antes de fragmentar y mantener su función de privacidad separada del fundamento de consumo.

Metadatos: ID de fuente y fragmento, título, emisor, URL oficial, artículo/sección, capítulo si existe, versión, publicación, vigencia, consulta y hash. Desconocidos se marcan pendientes. Tamaños se ajustarán al modelo de embeddings sin truncar contexto. Solo documentos revisados se habilitarán.

## 4. Investigación y actualización

Las quince preguntas se registran en `evidencias/semana-1/01-preguntas-reales.md`. Incluyen DPE, [FAQ Sí Postal](https://sipostal.serviciopostal.gob.ec/Navegacion/PreguntasFrecuentes), conservada como E01-faq-postal.html, y una [consulta pública ARCOTEL](https://www.arcotel.gob.ec/wp-content/uploads/2025/05/Incorporacion-de-los-aportes-ciudadanos-del-Proceso-de-RC-2024-.pdf), página 2, consulta 2. No se conserva ese PDF completo con nombres ciudadanos. No son entrevistas del equipo ni normas adicionales del corpus.

Antes de habilitar se cotejarán versiones y reformas en fuentes oficiales y se revisará orientación institucional actual. No se atribuirán artículos o competencias por suposición. No se añadirán reglas de cálculo, plazos o promedios al alcance.
