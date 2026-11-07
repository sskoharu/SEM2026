# Generador Inteligente de Reclamos de Consumo

Caso 02 · Grupo 2 · UNIANDES. La aplicación guía a consumidores en Ecuador para convertir el relato de una compra o un servicio problemático en un reclamo formal. Organiza hechos y comprobantes declarados, solicita datos faltantes y presenta referencias normativas cuando hay evidencia suficiente.

## Arquitectura

React y TypeScript presentan el flujo. Java 21 y Spring Boot coordinan cinco capas mediante Spring AI. Qdrant recupera fragmentos de la Ley Orgánica de Defensa del Consumidor (LODC), Redis conserva el estado temporal y PostgreSQL guarda casos mediante el flujo de archivo. Apache POI construye el Word. [Arquitectura y alcance](docs/03-arquitectura.md)

## Ejecución local

Use Java 21, Node.js 22, el wrapper de Maven y acceso a los servicios configurados. El [catálogo de variables](.env.example) se complementa con `DB_URL`, `DB_USER` y `DB_PASSWORD` para PostgreSQL. Configure `LLM_API_KEY`, `QDRANT_API_KEY` y `REDIS_PASSWORD` en el entorno del proceso. Para usar el backend local, configure `VITE_API_BASE_URL=http://localhost:8080` antes de iniciar Vite. Por defecto el cliente apunta a Render. Use dependencias aisladas para ensayos con escrituras.

En Windows, desde la raíz, inicie el backend en una terminal y el frontend en otra.

~~~powershell
.\backend\App\mvnw.cmd -f backend/App/pom.xml spring-boot:run
~~~

~~~powershell
cd frontend
npm ci
npm run dev
~~~

Para validar la copia local, ejecute `mvnw.cmd clean verify` desde `backend/App` y `npm test` y `npm run build` desde `frontend`. Las pruebas HTTP de Render tienen comandos y resultados separados. [Pruebas funcionales](docs/08-pruebas.md)

## Despliegue y resultados

El backend público responde en Render. La comprobación de esta entrega obtuvo HTTP 200 con `{"status":"UP"}` en `/health`. Los registros del 7 de noviembre contienen 19 de 20 casos de Capa 1 aprobados y una prueba de humo independiente de 3 de 3. El fallo C09 corresponde a un artículo esperado ausente. La ejecución CI #30 de la base documental figura como fallida. [Salud del backend](https://asistente-inteligente-4kiq.onrender.com/health) · [Evidencias de Semana 4](docs/evidencias/semana-4/README.md)

## Límites y documentación

La prueba pública cubre la entrada y la clasificación. La URL pública del frontend y la demo completa con aprobación y descarga requieren evidencia adicional. La etiqueta `APROBADO` observada en la respuesta de Render describe metadatos del índice, cuya vigencia jurídica exige revisión humana. La discrepancia entre emisión y consumo del token de sesión está descrita en el contrato.

- [Arquitectura](docs/03-arquitectura.md) y [contratos HTTP](docs/contratos-api.md)
- [Prompts](docs/05-prompts.md), [RAG](docs/06-rag.md) y [evaluación](docs/07-evaluacion.md)
- [Pruebas](docs/08-pruebas.md), [seguridad](docs/seguridad.md) y [decisiones](docs/09-decisiones.md)
- [Presentación de 12 minutos](docs/sustentacion-uniandes.pdf) y [registro de sustentación](docs/evidencias/semana-4/README.md)
