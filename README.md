# Generador Inteligente de Reclamos de Consumo

Proyecto del grupo 2, caso de reclamos de consumo, para el Seminario de
Ingeniería de Software con IA Generativa (UNIANDES). El sistema organiza el
relato del consumidor, recupera normativa y prepara información revisable; no
constituye asesoría jurídica definitiva.

## Estado actual

El repositorio contiene una aplicación funcional en desarrollo:

- **Backend:** Java 21, Spring Boot 4.1.1 y Spring AI 2.0.1.
- **Frontend:** React, TypeScript y Vite.
- **Modelo configurado:** Gemini mediante el endpoint compatible con OpenAI de
  Google; la clave se lee desde `LLM_API_KEY`.
- **Embeddings y búsqueda:** `gemini-embedding-001` y Qdrant; la clave se lee
  desde `QDRANT_API_KEY`.
- **Corpus:** artículos LODC normalizados en
  `backend/App/src/main/resources/data/lodc/*.json`.
- **Persistencia de sesión:** Redis, configurado mediante propiedades de
  entorno o despliegue.
- **Parámetros deterministas:** `config/parametros.yaml`, cargado como
  configuración externa para los cálculos.

La ingesta actual lee los JSON del classpath y los inserta en la colección
Qdrant configurada por `spring.ai.vectorstore.qdrant.collection-name`.
Los PDF/HTML de `data/raw/` son fuentes de referencia, pero no son leídos
directamente por el runner actual.

## Flujo implementado y límites

El backend recibe el reclamo, anonimiza los datos antes de llamar al modelo,
extrae información estructurada, busca documentos en Qdrant y clasifica el
reclamo mediante metadatos y reglas. Las herramientas de cálculo permitidas
son `calculate_future_date`, `calculate_legal_interest` y
`calculate_percentage`; las herramientas de validación son `validateDate` y
`validateText`. El ejecutor limita a tres tareas de cálculo por plan.

El flujo existente incluye una pausa `WAITING_FOR_DECISION` antes de continuar
con la ruta de acción. Esto no debe confundirse con un agente separado con
herramientas `evidencia_faltante`, `buscar_normativa` o `generar_documento`:
esas herramientas no existen con esos nombres. Tampoco se debe afirmar que
existe un flujo completo de envío o confirmación de un documento final.

## Ejecución y pruebas

Desde la raíz del repositorio:

```powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml test
```

La evaluación de contrato se ejecuta explícitamente con:

```powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml -Dgroups=eval test
```

La suite de CI excluye `eval`, porque la evaluación de recuperación necesita
Qdrant, embeddings y sus variables de entorno. El conjunto de referencia está
en `eval/casos_rag.jsonl`; contiene 20 casos, pero todavía no representa una
métrica ejecutada contra Qdrant.

El frontend se prueba desde `frontend`:

```powershell
npm ci
npm test -- --run
```

## Documentación

| Tema | Archivo |
|---|---|
| Propuesta y alcance | `docs/01-propuesta.md` |
| Requisitos | `docs/02-requisitos.md` |
| Arquitectura real y límites | `docs/03-arquitectura.md` |
| Fuentes normativas | `docs/04-fuentes.md` |
| Evidencia de Semana 3 | `docs/evidencias/semana-3/` |
| Casos de evaluación | `eval/casos_rag.jsonl` |
