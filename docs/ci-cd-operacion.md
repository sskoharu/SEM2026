# CI/CD: controles, habilitación externa y reversión

## Integración continua

`ci-cd.yml` atiende push, pull_request y solicitudes manuales. Reutiliza `ci.yml`
para Java 21, Maven, frontend y proxy. Construye ambos contenedores, prueba salud,
seguridad y Redis TLS/ACL, valida workflows con actionlint y escanea todo el historial
con Gitleaks. Los binarios de esas herramientas se verifican con checksums oficiales.

Los Dockerfiles usan bases públicas por digest, sin necesitar credenciales del
Artifact Registry para CI de PR. El backend conserva Java 21, usuario app sin root,
PORT y el JAR existente. No cambia la arquitectura ni incluye corpus aprobado.

Dependencias npm: se bloquean vulnerabilidades moderadas o superiores del conjunto
y de runtime. Un override exacto de minimatch 11.0.1 corrige los diez avisos previos
de la cadena ESLint; instalación limpia, lint, compilación y pruebas verifican la
compatibilidad. npm audit informa cero vulnerabilidades conocidas, sin afirmar
ausencia de fallos desconocidos. Maven verifica
resolución/compilación y pruebas; no hay escaneo CVE Maven exhaustivo ni se afirma tal.

## Despliegue candidato deshabilitado

El job requiere simultáneamente ejecución manual con deploy_candidate=true,
ref main, variable ENABLE_CLOUD_RUN_CANDIDATE=true, éxito de todos los jobs y
aprobación del environment cloud-run-candidate. La variable de habilitación está
ausente y no se creó el entorno en esta sesión. No se ejecutó ningún workflow remoto.

Un administrador debe:

1. Crear el environment cloud-run-candidate con revisores obligatorios, prevención
   de autoaprobación y restricción a main; verificar disponibilidad de estas
   protecciones en el plan del repositorio privado. Si el plan no las soporta,
   no habilitar el job: hace falta una alternativa de aprobación real.
2. Configurar/probar WIF limitado al repositorio oficial y su contexto de despliegue
   en main/environment. Evitar permisos de identidad para PR de forks o ramas
   arbitrarias. La cuenta desplegadora necesita Artifact Registry Writer en el
   repositorio de imágenes, permisos de actualización de Cloud Run y actAs sobre
   la cuenta runtime existente; no asignar Owner ni Editor por comodidad.
3. Verificar las variables existentes: GCP_PROJECT_ID, GCP_REGION,
   GCP_ARTIFACT_REPOSITORY, GCP_WORKLOAD_IDENTITY_PROVIDER, GCP_SERVICE_ACCOUNT y
   CLOUD_RUN_SERVICE. Confirmar que la última identifica el backend existente.
4. Solo tras verificar realmente la protección, configurar
   CANDIDATE_ENVIRONMENT_PROTECTION_CONFIRMED=true. Esa atestación no crea ni
   sustituye la protección de GitHub.
5. Obtener autorización del usuario para habilitar ENABLE_CLOUD_RUN_CANDIDATE=true.
   No se creó ni modificó esa variable aquí. La habilitación no autoriza promoción.

El job usa OIDC/WIF, nunca claves JSON de servicio escritas en YAML. Publica una
imagen por SHA del commit, obtiene su digest y despliega ese digest con --no-traffic.
Antes registra solo tráfico, revisión lista e imagen anterior, sin entornos ni
secretos. Conserva IAM, red y referencias de secretos existentes. Un fallo de
validación impide el despliegue; no se crea un servicio nuevo si describe falla.

## Validación de una candidata autorizada

Registrar URL y revisión; verificar arranque sin ingesta ni esquema automático,
liveness 200 y readiness 200 con Redis operativo. Usar identidad IAM legítima
cuando corresponda; comprobar HTTP Basic, RAG_USER y rechazo anónimo. No incluir
contraseñas en comandos/logs. Probar relato, aclaraciones, vista previa y descarga
DOCX con datos sintéticos. Sin evidencia jurídica aprobada, el sistema debe excluir
el corpus y no inventar citas. La prueba positiva queda pendiente de aprobación e
indexación autorizadas. No desactivar Redis health para promover.

La promoción de tráfico requiere una autorización independiente y un procedimiento
registrado; el workflow no la ejecuta. El frontend se construye/prueba, pero no se
despliega implícitamente junto al backend.

## Reversión

Una candidata no recibe tráfico de producción. Si falla, conservar o retirar su
tag únicamente con autorización; no borrar datos ni secretos. Si posteriormente
se autorizó promoción y hay que revertir, usar la distribución exacta conservada
en candidate-before.json. Ejemplo preparado, no ejecutado:

```powershell
gcloud run services update-traffic sem2026-rag-api --project sem2026-rag-taller --region us-central1 --to-revisions REVISION_PREVIA=100
```

Sustituir REVISION_PREVIA por la revisión realmente registrada; si había reparto,
restituir ese reparto. Comprobar health, autenticación y descarga después. No usar
una revisión histórica fija sin comprobar el estado previo.

## Publicación GitHub

Tras revisión y autorización, desde la rama de integración:

```powershell
git fetch origin
git status --short
git log --oneline origin/main..HEAD
git push --set-upstream origin codex/cierre-integral-sem2026-20261106
```

Los comandos de push están documentados, no ejecutados. Si main avanzó, integrar
y repetir pruebas antes. Abrir un PR hacia main, revisar CI, corpus y evidencia de
seguridad, y obtener aprobación. No hacer merge ni habilitar CD automáticamente.

Referencias: https://github.com/google-github-actions/auth y
https://docs.cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines
