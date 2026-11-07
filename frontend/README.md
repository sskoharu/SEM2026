# Generador Inteligente de Reclamos de Consumo

Frontend académico de UNIANDES para registrar reclamos de consumo en Ecuador y solicitar su análisis al backend Spring Boot.

## Tecnologías

React 19, TypeScript 5.9 y Vite 8. ESLint revisa el código. El diseño utiliza CSS adaptable en azul y blanco, sin bibliotecas visuales adicionales.

## Instalación y ejecución

Requisitos: Node.js 22.12 o posterior compatible con las herramientas de desarrollo y npm. Esta entrega se validó con Node.js 24.18.0.

Desde la carpeta `frontend`:

```powershell
npm ci
npm run dev
```

Comprobaciones y versión de producción:

```powershell
npm run build
npm run lint
npm run test
npm run preview
```

`build` comprueba TypeScript y genera `dist/`. `preview` permite revisar esa compilación localmente.

## Estructura modular

~~~text
frontend/
├── public/
│   ├── favicon.svg
│   └── icons.svg
├── src/
│   ├── assets/
│   │   ├── hero.png
│   │   ├── react.svg
│   │   └── vite.svg
│   ├── modules/
│   │   └── reclamos/
│   │       ├── components/
│   │       │   ├── AnalisisInicial.tsx
│   │       │   ├── EstadoCarga.tsx
│   │       │   ├── FormularioReclamo.tsx
│   │       │   ├── MensajeError.tsx
│   │       │   ├── PreguntasComplementarias.tsx
│   │       │   └── ResultadoFinal.tsx
│   │       ├── constants/
│   │       │   └── reclamo.constants.ts
│   │       ├── services/
│   │       │   ├── reclamosService.ts
│   │       ├── types/
│   │       │   └── reclamo.types.ts
│   │       └── utils/
│   │           └── validarReclamo.ts
│   ├── shared/
│   │   ├── http/
│   │   │   ├── esperar.ts
│   │   │   ├── httpClient.ts
│   │   │   ├── httpErrors.ts
│   │   │   └── retryPolicy.ts
│   │   └── metrics/
│   │       ├── metricas.types.ts
│   │       └── metricasService.ts
│   ├── App.css
│   ├── App.tsx
│   ├── index.css
│   └── main.tsx
├── tests/
│   ├── http/
│   │   ├── httpClient.test.ts
│   │   └── retryPolicy.test.ts
│   ├── manual/
│   │   ├── escenarios.html
│   │   └── escenarios.tsx
│   ├── metrics/
│   │   └── metricasService.test.ts
│   └── reclamos/
│       ├── datos.ts
│       ├── flujo.test.tsx
│       ├── servicios.test.ts
│       └── validarReclamo.test.ts
├── .gitignore
├── eslint.config.js
├── index.html
├── package-lock.json
├── package.json
├── README.md
├── tsconfig.app.json
├── tsconfig.json
├── tsconfig.node.json
├── tsconfig.tests.json
├── vite.config.ts
└── vitest.config.ts
~~~

- `App.tsx` coordina el estado del formulario, etapas, solicitudes cancelables al backend, mensajes, respuestas y reinicio. Conserva la estructura general y el indicador de cuatro etapas.
- `FormularioReclamo` presenta los campos y comunica los cambios a `App`. Selecciona las soluciones de producto o servicio a partir de las constantes.
- `AnalisisInicial` presenta los estados de espera, carga y error, los hechos, orientaciones y mensajes. Compone los componentes de preguntas y resultado final.
- `PreguntasComplementarias` muestra las preguntas y comunica las respuestas opcionales y la acción de completar a `App`.
- `ResultadoFinal` presenta la confirmación del backend y la cantidad de preguntas pendientes.
- `reclamo.types.ts` define los tipos compartidos de reclamo, estado, pregunta y análisis.
- `reclamo.constants.ts` contiene los valores iniciales y las opciones de solución existentes.
- `validarReclamo.ts` contiene las reglas y mensajes de validación del formulario original.
- `reclamosApi.ts` valida y adapta las sesiones devueltas por el backend para presentar hechos, preguntas y orientaciones.

Los componentes dependen de los tipos compartidos; el formulario también depende de las constantes. Las utilidades dependen de los tipos y no de React. `App` llama a las utilidades y entrega datos y funciones a los componentes. Los estilos se mantienen en `App.css` e `index.css`; `main.tsx` monta la aplicación con StrictMode.

## Funcionalidades disponibles

- Formulario inicial reducido al relato del incidente, que es el único dato solicitado por `POST /api/v1/claims/start`.
- Validación de una descripción mínima de 20 caracteres y un máximo de 2000 caracteres.
- La capa 1 solicita dinámicamente la información faltante mediante las preguntas devueltas por el backend.
- Análisis inicial conectado al backend, con preguntas y respuestas gestionadas por las sesiones de las capas 1 y 2.
- Respuestas complementarias opcionales incorporadas al resultado final; las preguntas sin respuesta no impiden continuar.
- Indicador de cuatro etapas, mensajes de estado y botón **Limpiar** para reiniciar e iniciar otro reclamo.
- Diseño adaptable para pantallas grandes y pequeñas.

## Limitaciones y pendientes

El frontend no genera análisis localmente ni contiene un modo de demostración. La sesión se conserva en el backend; recargar la página pierde únicamente el estado visual local. Guardar borrador y Mis reclamos permanecen pendientes. El comprobante solo se declara; no hay carga de archivos ni OCR.

El adaptador `services/reclamosApi.ts` usa los endpoints reales del backend: `/api/v1/claims/start`, `/{sessionId}/layer1/clarify`, `/{sessionId}/layer2/start` y `/{sessionId}/layer2/answer`. La URL se configura con `VITE_API_BASE_URL` y usa `https://asistente-inteligente-4kiq.onrender.com` por defecto. Para desarrollo local, defina `VITE_API_BASE_URL=http://localhost:8080`.

## Semana 2

El frontend usa el cliente fetch con AbortController, política de reintentos, errores tipados y métricas técnicas para comunicarse con el backend. Las respuestas se validan antes de presentarse y no existe una ruta local alternativa.

La carga bloquea campos y doble envío. Limpiar cancela; los errores conservan los datos y permiten volver a analizar. Se muestra la latencia local medida y tokens no disponibles.

El cliente HTTP admite timeout configurable (60000 ms por intento), 429 y Retry-After, hasta tres intentos con espera exponencial, conexión, servidor, respuesta vacía o inválida y cancelación. POST y otras operaciones modificadoras no se reintentan sin garantía explícita de idempotencia del servidor y clave estable. Los reintentos incluyen pausas que también pueden cancelarse.

Las métricas guardan identificador, fecha, operación técnica, duración total, estado, código, intentos y tokens. Se conservan hasta 100 registros en memoria y no contienen datos del formulario. Los tokens ausentes se guardan como null, sin estimaciones. obtenerMetricas y limpiarMetricas están disponibles en shared/metrics/metricasService.ts para consumidores internos.

Las pruebas se organizan en tests/reclamos, tests/http y tests/metrics. npm run test comprueba tipos y ejecuta Vitest; la auditoría del 21 de octubre de 2026 tiene 79 pruebas aprobadas. HTTP se prueba con mocks identificados. El laboratorio /tests/manual/escenarios.html funciona solo en el servidor de desarrollo, muestra errores y métricas de prueba y no forma parte de dist.

La documentación completa está en ../docs/semana-2 y las evidencias verificables en ../docs/evidencias/semana-2. Incluyen arquitectura, módulos, manejo de errores, métricas, plan y resultados de pruebas, decisiones e integración pendiente.

## Auditoría final y preparación para GitHub

El 21 de octubre de 2026 se revisó el frontend sin modificar su arquitectura ni actualizar dependencias. TypeScript utiliza `strict`. Editar los datos del formulario descarta el análisis anterior para evitar mostrar hechos desactualizados; los datos introducidos se conservan. Las validaciones del frontend deben repetirse en el backend.

Ejecute también `npm audit`. No coloque credenciales en variables `VITE_`: todo valor expuesto al cliente puede leerse desde el navegador. `.env` y sus variantes están excluidos de Git; `.env.example`, si se necesita, solo debe contener valores públicos de ejemplo. Configure `VITE_API_BASE_URL` cuando el backend no esté en `http://localhost:8080`.

El informe vigente está en `../docs/semana-2/10-auditoria-final.md`. Los documentos y el Word del 20 de octubre conservan las evidencias históricas de 63 pruebas; el informe final registra las 79 actuales. Los recursos de plantilla en `src/assets` y `public/icons.svg` no tienen referencias en la aplicación y se conservan por trazabilidad; no afectan al JavaScript generado.

El backend debe permitir CORS para el origen del frontend y estar disponible antes de enviar el formulario. La aplicación usa por defecto el backend desplegado en Render; para desarrollo local, configure `VITE_API_BASE_URL`. Los POST no se reintentan sin garantía explícita de idempotencia; el flujo actual conserva los datos y muestra el error cuando la conexión o la respuesta no son válidas.

Para publicar posteriormente, revise primero `git status --short` y el contenido preparado con `git diff --cached`. Versione fuente, pruebas, configuraciones, README, package.json, package-lock.json, documentos y evidencias académicas seleccionadas. Excluya node_modules, dist, entornos privados, logs, cachés, archivos temporales y frontend.zip. La publicación debe realizarla el responsable del repositorio; esta auditoría no ejecuta push ni crea remotos.
