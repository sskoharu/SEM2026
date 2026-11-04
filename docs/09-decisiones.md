# Decisiones de la corrección

- Se conserva la arquitectura y las versiones de dependencias del directorio fuente. No se modifica ningún valor de credencial.
- Se corrige el import faltante y se comprueba compilación limpia, sin reutilizar target anterior.
- Las sesiones anónimas se protegen con un token de capacidad independiente del UUID, hash SHA-256 en servidor y almacenamiento en memoria del cliente. No equivalen a una cuenta autenticada; quien robe el token puede actuar como su poseedor. HTTPS y protección frente a XSS siguen siendo necesarios.
- Los controles de concurrencia son locales a la instancia. Un despliegue multirréplica necesita una estrategia distribuida revisada antes de habilitarse.
- Los datos personales documentales se recogen después del análisis y se insertan localmente en la plantilla. Relatos y parámetros enviados al modelo se minimizan; los patrones no garantizan reconocer cualquier nombre libre, por lo que se exige usar datos ficticios en el seminario.
- Inventario: relato minimizado, parámetros, fuentes, decisiones y borrador temporal se conservan en Redis por 60 minutos. Nombre, identificación, correo, teléfono, proveedor, RUC/dirección aportados forman parte del snapshot/documento autorizado. No se utilizan como corpus ni como datos de evaluación reales. El token bruto no se guarda en Redis ni en logs.
- Las fuentes sin vigencia acreditada no se habilitan automáticamente. La tasa 0.12 heredada permanece como dato de configuración, con verificada=false. No constituye una tasa legal aprobada. Los tests usan tasa sintética explícita.
- Las constantes jurídicas propuestas por el modelo se bloquean. LegalRuleRegistry lee config/reglas_juridicas.json, entregado vacío. Una regla exige fuente recuperada, cita, versión, fecha y responsable con aprobación explícita; no se aprueba ningún plazo por memoria.
- Las citas se restringen a extractos textuales comprobables; su aplicabilidad requiere revisión humana.
- La aprobación vincula el contenido integral visible y los bytes exactos del DOCX al snapshot de sesión. La descarga no invoca otra redacción.
- Se retira únicamente el servicio TypeScript duplicado fuera de src y el recurso vacío sin consumidores. Los DTO históricos no se conectan artificialmente ni se atribuyen como funciones operativas.
- No se atribuyen aportaciones o revisiones a los integrantes sin historial. Esta corrección fue asistida por Codex y verificada mediante los comandos de entrega.
