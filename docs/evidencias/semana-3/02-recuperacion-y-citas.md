# Evidencia 02 — Recuperación, filtros y citas

## Estado

**Recuperación y exposición de artículos implementadas; ejecución contra
Qdrant y cita generada por el modelo pendientes de verificación.**

Esta evidencia distingue entre lo que se observa en el código y lo que debe
demostrarse ejecutando el backend. No se registra una respuesta del modelo ni
un artículo recuperado como resultado real hasta contar con la salida de una
ejecución.

## Flujo implementado

La búsqueda de fuentes se realiza en:

```text
backend/App/src/main/java/org/app/services/layer_1/components/c2/
```

El flujo actual es:

1. `TraduciendoQueryStep` obtiene un término de búsqueda conceptual del agente
   legal y lo guarda en `SessionObject.currentSearchQuery`.
2. `BuscandoFuentesStep` llama a `VectorSearchTool.search(query, 5)`.
3. `VectorSearchTool` crea una búsqueda de similitud con:
   - `topK = 5`;
   - umbral de similitud `0.75`;
   - el `VectorStore` configurado sobre Qdrant.
4. Los documentos recuperados se guardan en memoria en
   `Layer1ExecutionContext.retrievedDocs`.
5. `EvaluandoPrecisionStep` entrega esos documentos a la clasificación posterior y
envía el flujo a aclaración cuando la lista está vacía.

La recuperación, por tanto, está conectada al flujo de Capa 1. Los artículos
clasificados se conservan en `SessionObject.applicableArticles` y el frontend
los expone como `fuentes`.

## Filtros por metadatos

Existe un segundo método en `VectorSearchTool`:

```java
getArticlesByNumbers(List<String> articleNumbers)
```

Construye un filtro de Qdrant con expresiones `numero_articulo == ...` y lo
utiliza para recuperar artículos procedimentales específicos. Esto demuestra
soporte de filtrado por número de artículo en el código.

El flujo principal de `BuscandoFuentesStep` utiliza búsqueda semántica sin un
filtro de vigencia, cantón o categoría adicional. Aunque la ingesta conserva
`tipo_reclamo_asociado` en los metadatos y crea su índice en Qdrant, todavía no
se ha verificado una consulta principal que lo utilice como filtro.

## Identificadores disponibles para una futura cita

Durante la ingesta, cada documento conserva:

- el texto legal en `Document.text`;
- el identificador original del JSON en `metadata.original_id`;
- `numero_articulo`;
- `ley`;
- `capitulo`;
- `tema_principal`;
- `tipo_reclamo_asociado`, cuando está presente;
- otros metadatos del artículo.

Por ello, una cita técnicamente trazable debería construirse a partir de
`original_id` y `numero_articulo`, acompañada por el texto exacto recuperado.
El modelo recibe actualmente el texto de los documentos fallidos como contexto
en `LegalTranslatorAgent`, pero no hay evidencia en este flujo de un contrato
que obligue a la respuesta final a devolver los identificadores utilizados.

## Cita obligatoria: estado real

La respuesta de sesión ya incluye `applicableArticles`, que el frontend mapea
a `fuentes` y presenta como `Art. <número>`. Esto expone los artículos
clasificados, pero no equivale todavía a una cita textual generada por el
modelo con fragmento y explicación.

Por tanto, el requisito de **cita visible y trazable** queda:

```text
Implementado parcialmente: artículo visible; fragmento y ejecución real
pendientes.
```

No debe marcarse como cumplido solamente porque Qdrant devuelve documentos.
Recuperar una fuente y mostrar una cita identificable son dos requisitos
distintos.

## Caso de prueba propuesto

Este caso se utilizará cuando estén disponibles Qdrant, el modelo de embeddings
y las variables de ejecución:

```json
{
  "incidentDescription": "El proveedor publicó un precio y me cobró uno diferente."
}
```

La prueba debe conservar:

1. la entrada exacta;
2. el query conceptual generado;
3. el número de resultados devueltos;
4. cada `original_id` y `numero_articulo` recuperado;
5. el texto exacto del fragmento que fundamenta la respuesta;
6. la respuesta del modelo;
7. la cita visible en la respuesta o la constatación explícita de que no existe.

El artículo recuperado no se predeclara como resultado observado. La relación
entre el caso y un artículo concreto se confirmará únicamente con los logs o la
respuesta real de Qdrant.

## Comando de ejecución

Desde la raíz del repositorio:

```powershell
.\backend\App\mvnw.cmd spring-boot:run
```

La ejecución requiere las mismas dependencias y variables documentadas en
`01-ingesta-rag.md`. Antes de probar este caso debe completarse la ingesta o
confirmarse que la colección ya contiene el corpus.

La salida que debe guardarse, sin secretos, debe incluir como mínimo:

```text
-> BUSCANDO_FUENTES | Query: '...'
Se recuperaron ... documentos de Qdrant.
```

Si se necesita una evidencia más detallada, se debe añadir temporalmente un
log controlado que muestre únicamente identificadores de artículo y metadatos
no sensibles; no se deben imprimir tokens ni credenciales.

## Resultado observado

### Verificación estática del código

- Estado: **Verificado parcialmente**.
- La búsqueda semántica solicita hasta cinco documentos.
- El umbral configurado es `0.75`.
- Los documentos recuperados se guardan en el contexto de Capa 1.
- Existe filtrado por `numero_articulo` para búsquedas específicas.
- `applicableArticles` se expone en la respuesta de sesión y en el frontend.

### Ejecución con Qdrant

- Estado: **Pendiente**.
- No se ha registrado todavía una consulta real con su salida.
- No se ha verificado todavía que la respuesta final muestre la fuente exacta.

## Limitaciones detectadas

- No se registra un score simulado como métrica de precisión. La lista vacía
  produce aclaración, pero todavía no se ha medido la calidad de recuperación.
- El límite `topK = 5` está implementado, pero no existe en esta evidencia una
  medición de precisión o recall.
- La respuesta expone artículos clasificados, pero todavía no devuelve el
  fragmento textual citado.
- Los metadatos locales no contienen `url` ni `vigencia`, como se documentó en
  `01-ingesta-rag.md`.
- No se deben completar las métricas de recuperación ni declarar citas correctas
  hasta ejecutar y registrar el caso.