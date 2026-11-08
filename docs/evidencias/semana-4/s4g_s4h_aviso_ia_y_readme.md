# s4g / s4h — Aviso de IA, datos y README final

## s4g — Aviso de uso de IA y tratamiento de datos
Está en el `README.md`, sección **"Aviso de uso de IA y tratamiento de datos"**, y se sustenta en `docs/09-decisiones.md` (E1, E3, E5).

Puntos para exponer:
- El sistema usa un LLM (OpenAI); puede equivocarse; no es abogado ni da asesoría vinculante.
- Texto sugerido para mostrar en la interfaz antes de enviar el relato (incluido en el README).
- Minimización: nombre, cédula y contacto se piden solo al generar el Word y se insertan por código; no pasan por el modelo ni quedan en la sesión de Redis. La cédula se enmascara en la compuerta DPE.
- Retención: sesión en Redis 24 h; reclamo registrado en PostgreSQL.
- Límite declarado: el relato libre no se anonimiza; por eso el aviso pide no escribir datos personales en él.

**Pendiente (fuera del backend):** el aviso debe mostrarse realmente en el cliente/interfaz. Este repositorio solo contiene el backend; si hay frontend, hay que incluir el texto allí y capturarlo como evidencia.

## s4h — README final y decisiones
- Enlace roto corregido: ahora apunta a `plan_pruebas_funcionales_url_desplegada/` y se enlazan `docs/08-pruebas.md`, `eval/comparacion_s3_s4.md`, `eval/07-evaluacion.md` y el resto de documentos de `eval/` (todos los enlaces de README y `09-decisiones.md` fueron verificados con script: ninguno roto).
- Referencia obsoleta corregida en `docs/06-rag.md` (`docs/07-evaluacion.md` → `eval/07-evaluacion.md`).
- Añadido "Inicio rápido (≈10 minutos)", `/register` y `/health` en la tabla de API, estructura actualizada y limitaciones ampliadas (Reglamento no indexado, DPE sin revisión humana, sin bucle ReAct, variabilidad de C09).
- Decisiones técnicas y éticas: `docs/09-decisiones.md` (D1–D7, E1–E7, limitaciones en la sección 4).
