# Cierre local de integración con GitHub — 6 de noviembre de 2026

## Base y alcance

Repositorio oficial: https://github.com/sskoharu/SEM2026. Rama principal `main`.
La base local y remota verificada fue `fe6989c605d512d12153c625911a04a12e9dd05c`.
Se preparó `codex/cierre-integral-sem2026-20261106` sin modificar `main`.
No había commits remotos nuevos ni conflictos de ramas. Se conservaron los
workflows remotos, incluido el de verificación de Google Cloud; no se ejecutaron.

Se incorporaron las correcciones RAG y Redis locales, pruebas, imágenes definidas
por Dockerfile, proxy de frontend y timeout de análisis ya presentes localmente.
El CI incorpora ahora los tests Node del proxy además de los tests frontend.
Se reforzó `.gitignore` para archivos privados y artefactos generados.
No se cambiaron arquitectura, textos jurídicos, credenciales ni infraestructura.

## Pruebas repetidas

- Backend: `mvn -B -ntp clean verify`, 95 pruebas sin fallos ni omitidas.
- Frontend: `npm ci`, `npm run build`, `npm test`, 52 pruebas correctas.
- Proxy: `node --test tests/server/proxy.test.mjs`, 7 pruebas correctas.
- Imagen backend: construida como `sem2026-rag:github-validado`; 5 verificaciones
  de `scripts/Test-Container.ps1` y prueba TLS/ACL de `scripts/Test-RedisReadiness.ps1`.
- Redis local: PING y SET/GET correctos; readiness 200 disponible, 503 ante
  WRONGPASS o caída; liveness 200 durante caída.
- Gemini real y Qdrant local aislado: cuatro consultas de 3072 dimensiones,
  cero puntos y cero upserts porque no existen aprobaciones verificables.

Las pruebas de flujo DOCX y fundamentación del backend usan dobles para servicios
externos. No equivalen a una demostración positiva con fuentes jurídicas aprobadas
en producción. Los 60 fragmentos y sus hashes originales permanecen intactos.
Los artículos 49 y 70 y la disposición final segunda se documentan en
`revision-juridica.md`; todos requieren revisión humana, sin aprobaciones inventadas.

## Bloqueo de publicación por historial sensible

Gitleaks 8.30.1 detectó una contraseña Redis literal en el commit histórico
`b6fd4ad688693dad4b20dfab564127249c5d4e97`, archivo
`backend/App/src/main/resources/application.properties`, línea 62.
No se reproduce el valor. El archivo actual utiliza variables y no contiene esa
contraseña. La comparación protegida confirma que es distinta del secreto vigente
versión 8. Esto no acredita su revocación en el proveedor anterior.

El escaneo de los archivos candidatos actuales no detectó secretos. No se añadieron
excepciones para ocultar el hallazgo histórico ni se desactivó el workflow existente
de Gitleaks. Este workflow puede bloquear CI mientras el antecedente siga sin resolver.

Antes de publicar:

1. El titular debe confirmar revocación de la credencial histórica o autorizar su
   invalidación en el proveedor afectado. No cambiar el secreto operativo vigente
   únicamente por este hallazgo de una contraseña diferente.
2. Revisar accesos históricos del proveedor y alcance de la exposición. No probar
   la contraseña antigua contra servicios externos ni copiarla en tickets o logs.
3. Acordar con los colaboradores si corresponde limpiar el historial mediante un
   procedimiento coordinado y autorizado. Eliminar el literal solo del archivo actual
   no elimina los commits anteriores. No realizar reescrituras ni force-push aquí.
4. Repetir el escaneo, documentar la remediación y solicitar aprobación del push.

La rama y el commit local son revisables; no implican autorización ni aptitud de
publicación mientras persista este bloqueo. No se ejecutó push, merge a main,
despliegue, modificación de Secret Manager ni operación en Qdrant de producción.

## Pendientes operativos

Revisión jurídica, indexación positiva autorizada, prueba integral en el entorno
operativo y autorización de publicación/despliegue. La configuración Redis vigente
aceptó AUTH/PING con TLS en la comprobación previa; la causa del WRONGPASS histórico
no se ha demostrado. Ver `redis-operacion.md` y `rag-recuperacion.md`.
