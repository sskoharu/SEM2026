# Fuentes y revisión jurídica

Las copias originales disponibles en data/raw y los JSON de recursos se conservan como fuentes de trabajo. Los recursos registran como referencia la copia oficial publicada por el Ministerio de Producción:
`https://produccion.gob.ec/wp-content/uploads/2025/03/LEY-ORGANICA-DE-DEFENSA-DEL-CONSUMIDOR_2022_02_11.pdf`,
con versión indicada como última modificación del 11-feb-2022. Esto documenta procedencia de trabajo, pero no certifica por sí solo vigencia consolidada, autenticidad de transcripción ni incorporación de reformas. El reglamento no está normalizado en el corpus indexable.

docs/manifiesto-corpus.json identifica los diez JSON, sus huellas SHA-256 y sesenta IDs. No sustituye una revisión jurídica ni un manifiesto de los originales descargados. No se referencia un manifiesto ausente como si existiera.

El runner conserva la procedencia local y cada recurso registra `source_url`, `source_version`, `source_accessed_at` y `review_status=PENDIENTE_REVISION`. La ausencia de una revisión jurídica responsable mantiene `reviewed=false`, `status=PENDIENTE_REVISION`, URL operativa vacía y vigencia no verificada. CorpusPolicy excluye ese contenido como fundamento operativo. Se mantiene abstención hasta que un responsable documente y habilite una versión revisada.

No se consultaron nuevas fuentes externas para atribuir reformas o plazos. No se añaden normas por memoria, ni se transforma una copia histórica en norma vigente.
