# Recuperación RAG e indexación controlada

## Compilar y validar

```powershell
Set-Location C:/Users/User/SEM2026/backend/App
mvn clean verify
```

El JAR se genera en target/Backend_Artifact-1.0-SNAPSHOT.jar. Las credenciales se
suministran por el entorno existente, nunca mediante archivos versionados. Los
nombres SPRING_AI_VECTORSTORE_QDRANT_API_KEY y APP_SECURITY_RAG_USERNAME/PASSWORD
que usa Cloud Run conservan su precedencia. No se cambiaron Secret Manager o IAM.

POST /api/v1/rag/query y GET /api/v1/rag/search requieren HTTP Basic y RAG_USER.
La identidad se configura mediante app.security.rag.username/password; también
se admiten los aliases RAG_API_USERNAME y RAG_API_PASSWORD. Sin configuración,
no se concede acceso anónimo ni se publica una contraseña conocida.

## Revisión humana sin alterar el corpus

Los 60 JSON locales declaran reviewed=true y VIGENTE, pero carecen de responsable,
fecha y estado_revision=APROBADO. Esas marcas no acreditan aprobación humana.
La política compartida exige evidencia completa, URL HTTPS válida, vigencia
informada y controles de contenido. NO_VERIFICADA y No verificada se rechazan.

Puede usarse un registro externo, sin modificar los fragmentos originales:

```json
{
  "schema": "sem2026-corpus-review-1",
  "reviews": [
    {
      "id": "identificador-existente-del-fragmento",
      "text_sha256": "sha256-del-texto-exacto-en-utf8",
      "decision": "PENDIENTE_REVISION",
      "reviewed_by": "",
      "reviewed_at": "",
      "url": "",
      "vigencia": "",
      "notes": ""
    }
  ]
}
```

La plantilla entregada Revisiones-pendientes.json ya contiene los IDs y hashes
actuales, con cero aprobaciones. Una persona autorizada debe cotejar cada texto
con publicación oficial, reformas y vigencia, y solo después completar su decisión
APROBADO o RECHAZADO, identidad, fecha ISO y evidencia. No modificar hashes para
hacer pasar textos diferentes. El hash certifica coincidencia, no validez jurídica.

Con un registro configurado, los IDs ausentes, rechazados o con hash diferente
quedan excluidos. Las decisiones se cargan al arrancar; no se actualizan en caliente.
Quien pueda modificar el archivo puede modificar las decisiones: debe almacenarse
bajo permisos y revisión de cambios, no como una aprobación automática del sistema.

## Indexación: requiere autorización de escritura separada

La API normal NO ejecuta ingesta ni inicializa esquema. No usar el antiguo flag
rag.ingest-on-start para eludir el procedimiento; una activación incompatible se
rechaza. No activar APP_INDEXING_ENABLED ni initialize-schema en el servicio Cloud Run.

La entrada de mantenimiento carga únicamente los componentes necesarios, sin API,
Spring Security ni Redis. Con credenciales existentes provisionadas de forma privada,
desde backend/App y solo tras autorizar la operación y el corpus:

```powershell
java '-Dloader.main=org.app.bootstrap.LodcIndexingCommand' -cp target/Backend_Artifact-1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher --app.indexing.enabled=true --spring.main.web-application-type=none --app.corpus.review-file=C:/ruta-controlada/revisiones.json
```

La entrada activa el perfil indexing y exige habilitación expresa. K_SERVICE impide
su uso dentro del servicio Cloud Run. El proceso cierra el contexto al terminar.
No quitar ese indicador ni desactivar TLS para sortear controles.

La colección existente lodc_leyes_vector tiene 3072 dimensiones y distancia Cosine.
NO añadir initialize-schema=true para esa colección. Crear una colección/esquema
ausente requiere autorización distinta. El procedimiento conserva UUID deterministas,
original_id, document_id y source_file; normaliza numero_articulo como cadena y
crea/verifica los tres índices de payload existentes solo con documentos admitidos.
Un error de lectura o de índices interrumpe la operación; no hay borrados ni resets.
El upsert no es una transacción: verificar conteos y errores antes de reintentarlo.

La prueba ejecutada del comando usó exclusivamente destinos loopback, TLS activo,
credenciales sintéticas y 0 documentos admitidos. No fue una ingesta real.

## Lectura y errores

La búsqueda mantiene el umbral 0.75. Combina evidencia léxica aprobada con búsqueda
vectorial; si falla el proveedor vectorial, conserva la evidencia local aprobada.
Sin evidencia local, el fallo se propaga y no se disfraza de ausencia de fuentes.

La consulta por número de artículo usa scroll paginado y filtro exacto de Qdrant,
con el payload doc_content que escribe Spring AI. No usa similitud, embeddings ni
un topK arbitrario para recuperar fragmentos de un artículo específico. Los documentos
recuperados vuelven a pasar por la política de aprobación.

## Preparación de actualización; no ejecutada

1. Revisar e integrar los archivos completos del paquete, preservando los cambios
   de frontend que ya existían. Repetir mvn clean verify desde backend/App.
2. Con Docker disponible, construir desde backend/App:

```powershell
docker build -t sem2026-rag:correccion-rag .
```

El Dockerfile usa Eclipse Temurin Java 21 JRE público fijado por digest,
usuario app sin root, directorio /app y ENTRYPOINT compatible con PORT.
No necesita acceso al registro privado para validar PR y no conserva capas
anteriores de la aplicación. El contexto excluye fuentes, entornos y secretos.
La imagen se construye y valida localmente; publicar requiere autorización.
3. Publicar una imagen requiere autorización; no se ha realizado push. Cuando se autorice:

```powershell
$Imagen = 'us-central1-docker.pkg.dev/sem2026-rag-taller/sem2026-docker/sem2026-rag:correccion-rag'
docker tag sem2026-rag:correccion-rag $Imagen
docker push $Imagen
```

4. Crear una revisión candidata requiere autorización posterior; conservar referencias
de secretos, IAM, cuenta de servicio y red. Comando preparado, no ejecutado:

```powershell
gcloud run deploy sem2026-rag-api --project sem2026-rag-taller --region us-central1 --image $Imagen --no-traffic --tag rag-validacion
```

Comprobar antes que el tag no tenga otro uso. La consulta de tráfico confirmó 100%
en 00007-sn7 y un tag fase2 preexistente en 00009-6dm; no se modificaron ni se deben
sobrescribir por este procedimiento. No añadir acceso público, reemplazos de secretos
ni comandos de cambio de tráfico. La promoción requiere autorización independiente.
5. Validar la revisión privada, autenticación, health y endpoints. Para demostrar
recuperación productiva positiva hacen falta aprobación jurídica y una indexación
real autorizada. Desplegar código por sí solo no llena una colección vacía.
