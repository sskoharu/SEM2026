# Redis: configuración, diagnóstico seguro y pruebas

## Configuración

Spring Boot 4.1.1 configura Lettuce mediante `spring.data.redis.*`. No se añade un cliente paralelo ni se transforma la contraseña. `RedisConfig` conserva el template y los serializadores existentes.

| Propiedad | Variable estándar preferida | Alias local | Base no secreta |
|---|---|---|---|
| host | SPRING_DATA_REDIS_HOST | REDIS_HOST | moving-goat-89109.upstash.io |
| port | SPRING_DATA_REDIS_PORT | REDIS_PORT | 6379 |
| username | SPRING_DATA_REDIS_USERNAME | REDIS_USERNAME | default |
| password | SPRING_DATA_REDIS_PASSWORD | REDIS_PASSWORD | Sin contraseña incluida |
| ssl.enabled | SPRING_DATA_REDIS_SSL_ENABLED | — | true |

La variable estándar tiene precedencia sobre el alias. Una `SPRING_DATA_REDIS_URL` explícita sustituye host, puerto y credenciales; comprobar su presencia sin mostrar su contenido. Mantener TLS y validación del certificado. No convertir una URL REST del proveedor en un endpoint RESP, ni utilizar un token REST sin verificar su compatibilidad.

La base anterior apuntaba a un proveedor diferente del configurado en Cloud Run. Se alineó únicamente el destino no secreto y se hizo explícito el usuario; no se cambiaron credenciales. En Cloud Run las variables estándar existentes ya sobrescribían esa base.

## Evidencia operativa del 6 de noviembre de 2026

La plantilla de servicio y la revisión con tráfico `sem2026-rag-api-00007-sn7` referenciaban el endpoint indicado, puerto 6379, usuario `default`, TLS habilitado y `sem2026-redis-password`, versión 8. `SPRING_DATA_REDIS_PASSWORD` y `REDIS_PASSWORD` apuntaban a la misma versión. No había URL Redis explícita que sustituyera los parámetros.

Una comprobación externa de solo lectura verificó certificado TLS, AUTH correcto y PONG con esa combinación. No se escribieron claves en Redis operativo. Por ello no se reproduce WRONGPASS con la referencia vigente y no se demuestra un defecto Java. Esta prueba no equivale a una comprobación desde la red de Cloud Run ni acredita qué credencial se utilizó en un incidente histórico.

El secreto no estaba vacío, no tenía saltos de línea ni espacios exteriores y no era una URL. La evidencia entregada registra únicamente esos indicadores y el resultado; no incluye valores, fragmentos, longitudes ni hashes del secreto. No cambiar ni recortar automáticamente contraseñas. No se modificó Secret Manager ni se hizo un despliegue.

Si reaparece WRONGPASS, registrar fecha y revisión sin datos de autenticación, verificar las referencias efectivas de esa revisión y comprobar AUTH con el usuario correcto. AUTH rechazado también puede indicar usuario deshabilitado o pareja usuario/contraseña incorrecta. Comparar versión y destino con la evidencia, sin imprimir secretos. No rotar credenciales ni crear versiones sin autorización. El incidente histórico queda pendiente de correlación con su revisión/configuración y evidencia temporal.

## Pruebas locales reproducibles

Después de `mvn clean verify` y de construir la imagen, desde `backend/App`, PowerShell 7 y Docker:

```powershell
docker build -t sem2026-rag:cierre-verificado .
pwsh -File scripts/Test-Container.ps1
pwsh -File scripts/Test-RedisReadiness.ps1 -OpenSsl 'C:/Program Files/Git/usr/bin/openssl.exe'
```

La segunda prueba requiere `redis:7.4-alpine` local y OpenSSL. Crea una red aislada, Redis con TLS obligatorio y usuario ACL de pruebas; deshabilita el usuario por defecto de ese Redis. Genera material y credenciales efímeros de prueba y los elimina al terminar. No lee Secret Manager ni contacta Redis/Qdrant de producción. No almacena valores sensibles en los resultados.

Comprueba PING y SET/GET autenticados con TLS, readiness 503 con contraseña de prueba incorrecta, readiness 200 con contraseña correcta, readiness 503 tras detener Redis y liveness 200 durante esa caída. La aplicación usa la CA de pruebas mediante un SSL bundle; no se deshabilita la validación del certificado. Las credenciales incorrectas se prueban únicamente en Redis local.

`RedisConfigurationTest` añade cuatro regresiones: bases empaquetadas, alias locales con ACL y contraseña intacta, precedencia de variables estándar y precedencia de URL TLS explícita. No abre conexiones de producción.

## Salud y puesta en servicio

`/actuator/health/readiness` incluye `redis`, además de `readinessState`. `/actuator/health/liveness` comprueba el estado del proceso. Los detalles de health no se exponen. No quitar Redis de readiness para ocultar un fallo de autenticación.

La imagen corregida se prepara localmente. Publicación, despliegue, cambios de tráfico y modificaciones de secretos requieren autorización. Después de un despliegue autorizado, comprobar readiness en la revisión candidata desde el entorno operativo antes de promover tráfico.

Referencia: [Spring Boot, conexión a Redis y precedencia de URL](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/data/nosql.html).
