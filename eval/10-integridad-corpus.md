# 10 · Integridad y revisión del corpus

## Qué se garantiza
- `src/main/resources/data/corpus_manifest.json` registra, por cada uno de los 78 fragmentos (64 LODC + 14 DPE): hash SHA-256 del texto, `estado_revision` (`PENDIENTE_REVISION` / `APROBADO`), `revisado_por` y `fecha_revision`, más una `corpus_version` global.
- Solo `tools/corpus_manifest.py` escribe el manifiesto. Editar el texto de un fragmento cambia su hash y lo devuelve a `PENDIENTE_REVISION`; los metadatos no se tocan a mano.
- **Compuerta de ingesta:** `LodcDataIngestionRunner` y `DpeDataIngestionRunner` abortan si el hash no coincide con el manifiesto o si el fragmento no está `APROBADO` (salvo `app.ingestion.allow-unreviewed=true`, que registra una advertencia).
- **Test de consistencia:** `CorpusManifestConsistencyTest` falla si JSON y manifiesto divergen (conteo, hashes, contador de aprobados).

## Flujo de revisión
1. Revisar cada artículo contra la fuente F01 (PDF oficial).
2. `python tools/corpus_manifest.py --approve 20,26,71 --reviewer "Nombre"` (o `--approve-all` solo tras revisar todo).
3. `python tools/corpus_manifest.py --check` y `.\mvnw.cmd test`.
4. Reingestar: `APP_INGESTION_ENABLED=true ./mvnw spring-boot:run`.

## Estado actual y verificación contra la fuente

- **LODC (64 fragmentos): `APROBADO`.** `tools/verify_corpus_pdf.py` compara cada fragmento con `src/main/resources/data/lodc/original_source/F01-lodc.pdf` (sin tildes, espacios ni signos, porque el PDF inserta espacios espurios). Resultado: **64/64 aparecen literalmente en el PDF** (los fragmentos por numeral se verifican como encabezado del artículo + numeral). Informe: `eval/verificacion_corpus_vs_pdf.md` y `.json`.
- Se aprobaron con `python tools/corpus_manifest.py --approve-verified --reviewer "Verificacion automatica tools/verify_corpus_pdf.py vs F01-lodc.pdf"`. El campo `revisado_por` deja constancia de que la revisión fue automática y contra qué fuente; no es una revisión jurídica humana de contenido.
- **DPE (14 fragmentos): `PENDIENTE_REVISION`.** No hay documento fuente en el repositorio contra el cual verificarlos.
- Si se edita el texto de un fragmento, su hash cambia y vuelve a `PENDIENTE_REVISION` hasta repetir la verificación.

## Consulta RAG sin endpoint nuevo
`POST /api/v1/claims/start` es la entrada del RAG (Portero + Capa 1). La respuesta incluye ahora `ragEvidence`: por fragmento recuperado, `fragmentId`, `numeroArticulo`, `score`, `snippet`, `estadoRevision` y `corpusVersion`. Los demás campos no cambian.

## Repetir los 20 casos
```
.\mvnw.cmd test -Dtest=DeployedFunctionalTest -De2e=true -De2e.url=<url con esta versión>
```
Los resultados (con el cuerpo completo, incluido `ragEvidence`) quedan en `target/resultados_pruebas_funcionales.csv`. Con ese archivo se recalculan recuperación y abstención y se actualiza `eval/20_relatos_referencia/conjunto_evaluacion.md`; no reutilizar cifras anteriores.
