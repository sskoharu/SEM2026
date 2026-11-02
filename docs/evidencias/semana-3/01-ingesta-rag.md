# Evidencia 01 — Ingesta y fragmentación del corpus LODC

## Estado

**Parcialmente implementado; ejecución externa pendiente.**

La ingesta existente está implementada en Java mediante
`backend/App/src/main/java/org/app/bootstrap/LodcDataIngestionRunner.java`.
No existe actualmente un `scripts/ingest.py`; por tanto, esta evidencia describe
el mecanismo real del backend y no afirma que exista un script Python que todavía
no está en el repositorio.

## Fuente y estrategia de fragmentación

El corpus se encuentra en:

```text
backend/App/src/main/resources/data/lodc/
```

La estrategia implementada es **un documento vectorial por objeto JSON**, donde
cada objeto representa un artículo, numeral o disposición de la LODC. El texto
indexado se toma de `article.document`; no se aplica una fragmentación arbitraria
por número fijo de caracteres ni un solapamiento adicional.

Esta estrategia conserva la unidad jurídica que debe citarse y permite asociar
cada resultado con su artículo mediante los metadatos. Si un artículo extenso
requiere subdivisión en el futuro, deberá conservarse el identificador del
artículo original y documentarse el criterio utilizado.

## Inventario local verificado

El inventario se calculó leyendo los archivos JSON del directorio de recursos,
sin conectarse al vector store:

| Archivo | Artículos/documentos |
|---|---:|
| `01_principios_generales.json` | 3 |
| `03_regulacion_publicidad.json` | 3 |
| `04_informacion_basica_comercial.json` | 6 |
| `05_responsabilidades_proveedor.json` | 12 |
| `06_servicios_publicos.json` | 9 |
| `07_proteccion_contractual.json` | 6 |
| `09_practicas_prohibidas.json` | 8 |
| `13_infracciones_sanciones.json` | 10 |
| `15_disposiciones_generales.json` | 2 |
| `16_disposiciones_finales.json` | 1 |
| **Total** | **60** |

Se verificaron **60 identificadores únicos**. Este conteo local no demuestra que
los 60 documentos hayan sido aceptados por Qdrant; esa comprobación requiere
ejecutar la aplicación con sus servicios y credenciales configurados.

## Metadatos

Los objetos del corpus contienen actualmente estos campos de metadatos:

```text
articulos_procedimentales_asociados
capitulo
documento_tipo
ley
numeral
numero_articulo
parametros_requeridos
tema_principal
tipo_reclamo_asociado
```

Durante la ingesta, el campo `id` del JSON se conserva como `original_id`.
También se conserva el mapa completo de metadatos del artículo. La implementación
genera un UUID determinista a partir de `id`, porque Qdrant requiere un
identificador compatible:

```java
UUID.nameUUIDFromBytes(semanticId.getBytes(StandardCharsets.UTF_8))
```

### Limitación de metadatos

Los 60 objetos revisados no tienen campos `url` ni `vigencia`. Por ello, esos
dos requisitos de la semana 3 están **pendientes** y no deben declararse como
cumplidos. La ingesta actual sí conserva artículo, capítulo, ley, categoría de
reclamo y tema cuando están presentes en el JSON.

## Vector store e índices

La aplicación utiliza Qdrant mediante Spring AI. La colección configurada es
`lodc_leyes_vector`; los valores sensibles de conexión se obtienen mediante
variables de entorno y no se incluyen en esta evidencia.

Antes de insertar los documentos, el runner intenta crear índices de payload
tipo `Keyword` para:

- `numero_articulo`;
- `tipo_reclamo_asociado`;
- `original_id`.

Si el índice ya existe o Qdrant no permite crearlo, el código registra un warning
y continúa. Este comportamiento debe observarse en los logs de una ejecución
real antes de marcar la ingesta como verificada.

## Comando reproducible

Desde la raíz del repositorio:

```powershell
.\backend\App\mvnw.cmd spring-boot:run
```

La ejecución requiere:

- un JDK compatible con `java.version` en `backend/App/pom.xml`;
- `LLM_API_KEY`;
- `QDRANT_API_KEY`;
- acceso a la colección Qdrant configurada;
- el resto de variables necesarias para el perfil de ejecución.

Para una ejecución sin exponer secretos, guardar únicamente la salida filtrada
de los logs que muestre:

```text
INICIANDO INGESTA DE LA LODC A QDRANT
Leyendo archivo: ...
Generando Embeddings e insertando ... documentos
INGESTA COMPLETADA CON ÉXITO
```

No se deben adjuntar claves, tokens, URLs privadas ni valores completos de
variables de entorno.

## Resultado observado

### Verificación local de archivos

- Estado: **Verificado localmente**.
- Resultado: 10 archivos JSON, 60 documentos, 60 identificadores únicos.
- Alcance: lectura y conteo del corpus local.
- No prueba conexión, generación de embeddings ni persistencia en Qdrant.

### Ejecución de la aplicación

- Estado: **Pendiente**.
- Resultado esperado: los logs deben reportar la lectura de los 10 archivos y
  la inserción de 60 documentos.
- Resultado observado: aún no registrado en esta evidencia.

## Limitaciones y próximos datos

Para cerrar esta evidencia se debe ejecutar el comando anterior y registrar:

1. commit probado;
2. fecha y entorno;
3. cantidad de documentos enviados;
4. confirmación del estado de la colección;
5. salida de creación o reutilización de índices;
6. cualquier error de archivos, embeddings o Qdrant.

Hasta completar esa ejecución, la matriz de `README.md` debe mantener como
pendientes la ingesta reproducible, la recuperación con filtros y los metadatos
de vigencia y URL.