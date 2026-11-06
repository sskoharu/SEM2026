# Seguridad de publicación y revocación verificada

## Evidencia y alcance

El 6 de noviembre de 2026 se comprobó por TLS validado que la contraseña Redis
histórica del commit `b6fd4ad688693dad4b20dfab564127249c5d4e97`, propiedad
`spring.data.redis.password`, seguía siendo aceptada en el Redis anterior de
Layerbase. No se imprimió ni guardó su valor en informes.

El usuario autorizó invalidar únicamente esa credencial si existía acceso
administrativo. Se verificó el usuario ACL `default`, la pertenencia de la
contraseña al ACL y la ausencia de `nopass`. Se retiró únicamente esa contraseña
mediante ACL; no se deshabilitó el usuario ni se reemplazaron otras contraseñas.
Una conexión TLS nueva devolvió WRONGPASS: **revocación verificada para nuevas
autenticaciones**. No se modificaron Upstash ni Secret Manager.

No se cerraron conexiones que ya estuvieran autenticadas. El administrador del
Redis anterior debe revisar y terminar conexiones/sesiones residuales sospechosas
desde el plano de control. No intentar entrar con la contraseña retirada para
administrar el servicio. Este alcance residual no se presenta como resuelto.

## Exposición Git

La contraseña permanece en el historial compartido. El commit es alcanzable desde
main y las ramas remotas feature/backend-capas, feature/backend-semana3,
feature/correcciones-semana3, feature/frontend-ultimas-capas,
feature/semana3-evidencias y seguridad/gitleaks-ignore. No se encontraron etiquetas
remotas anunciadas. El proveedor GitHub puede conservar referencias de PR, cachés
y clones que no aparecen en la lista de ramas; el alcance externo no es exhaustivo.

La rama remota seguridad/gitleaks-ignore ya contenía la huella exacta del hallazgo.
Su existencia no demostraba revocación. Solo después de retirarlo y verificar
WRONGPASS se incorporó la excepción precisa de `.gitleaksignore` en esta rama,
con evidencia documentada. No hay exclusiones de archivos, reglas ni proveedores.

El escaneo con política debe excluir exclusivamente la huella de ese commit,
archivo, regla y línea. Un escaneo adicional sin excepciones sigue mostrando el
antecedente y debe conservarse como evidencia. La excepción no sanea el historial
ni permite otros secretos; un secreto en cualquier commit nuevo sigue bloqueando
CI. No debe usarse esta estrategia si se pierde la evidencia de revocación.

## Estrategia para sanear el historial compartido — no ejecutada

1. El administrador acuerda una ventana con todos los colaboradores, congela
   escrituras y registra ramas, etiquetas, PR y backups controlados.
2. En un clon espejo aislado, autorizado para esta operación, usar git-filter-repo
   para sustituir únicamente asignaciones literales de la propiedad Redis del
   archivo histórico por `${REDIS_PASSWORD}`. No escribir la contraseña en un
   archivo de reemplazos ni en la línea de comandos. Conservar asignaciones que
   ya sean referencias de entorno; no eliminar el archivo ni componentes del sistema.
3. Escanear todas las referencias del espejo sin excepciones, verificar corpus,
   compilar y comparar árboles. Registrar correspondencia entre commits originales
   y saneados. Retirar la excepción histórica únicamente después de ese escaneo.
4. Coordinar con el administrador la actualización de todas las referencias
   afectadas y el tratamiento de PR/cachés por GitHub. Esta fase implica reescritura
   compartida y requiere autorización específica; no se prepara un push destructivo
   para ejecución automática.
5. Los colaboradores recrean sus clones desde la referencia saneada y reaplican
   solo trabajo revisado. No fusionar ramas viejas que reintroduzcan el historial.
6. Repetir Gitleaks y las pruebas antes de reabrir escrituras. La revocación es
   obligatoria incluso si se limpia el historial: clones previos no desaparecen.

No se ejecutó ninguna reescritura ni publicación. El cierre de sesiones residuales
y la limpieza coordinada del historial requieren intervención administrativa.

Referencia primaria: https://github.com/newren/git-filter-repo
