# Generador Inteligente de Reclamos de Consumo

Prototipo académico de UNIANDES para registrar reclamos de consumo en Ecuador y recorrer un análisis **simulado mediante reglas locales**. No utiliza IA real, Ollama ni servicios del backend.

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
│   │       │   └── simulacionService.ts
│   │       ├── types/
│   │       │   └── reclamo.types.ts
│   │       └── utils/
│   │           ├── simularAnalisis.ts
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

- `App.tsx` coordina el estado del formulario, etapas, solicitudes cancelables, mensajes, respuestas y reinicio. La espera de 850 ms ahora pertenece al servicio de simulación. Conserva la estructura general y el indicador de cuatro etapas.
- `FormularioReclamo` presenta los campos y comunica los cambios a `App`. Selecciona las soluciones de producto o servicio a partir de las constantes.
- `AnalisisInicial` presenta los estados de espera, carga y error, los hechos, orientaciones y mensajes. Compone los componentes de preguntas y resultado final.
- `PreguntasComplementarias` muestra las preguntas y comunica las respuestas opcionales y la acción de completar a `App`.
- `ResultadoFinal` presenta la confirmación de demostración y la cantidad de preguntas pendientes.
- `reclamo.types.ts` define los tipos compartidos de reclamo, estado, pregunta y análisis.
- `reclamo.constants.ts` contiene los valores iniciales y las opciones de solución existentes.
- `validarReclamo.ts` contiene las reglas y mensajes de validación del formulario original.
- `simularAnalisis.ts` genera los hechos y preguntas con las reglas originales e incorpora las respuestas proporcionadas sin reanálisis con IA.

Los componentes dependen de los tipos compartidos; el formulario también depende de las constantes. Las utilidades dependen de los tipos y no de React. `App` llama a las utilidades y entrega datos y funciones a los componentes. Los estilos se mantienen en `App.css` e `index.css`; `main.tsx` monta la aplicación con StrictMode.

## Funcionalidades disponibles

- Datos del consumidor: nombre, correo, teléfono y cédula.
- Reclamos de producto, servicio, garantía u otro; proveedor y producto o servicio.
- Fecha y valor opcionales; declaración de factura o nota de venta y número opcional.
- Descripción de hasta 5000 caracteres y selección de solución, incluida otra solución.
- Validación de nombre, correo, producto, proveedor, descripción mínima de 20 caracteres, solución y valor decimal no negativo con hasta dos decimales, fecha real no futura y solución compatible con el tipo.
- Análisis inicial simulado con hechos declarados, preguntas según la información disponible y orientaciones generales.
- Respuestas complementarias opcionales incorporadas al resultado final; las preguntas sin respuesta no impiden continuar.
- Indicador de cuatro etapas, mensajes de estado y botón **Limpiar** para reiniciar e iniciar otro reclamo.
- Diseño adaptable para pantallas grandes y pequeñas.

## Limitaciones y pendientes

El análisis es una demostración local, no una valoración legal ni una respuesta real de IA. No existe persistencia: recargar la página pierde los datos. Guardar borrador y Mis reclamos permanecen pendientes, como en la versión original. El comprobante solo se declara; no hay carga de archivos ni OCR.

El backend Java Spring Boot está pendiente de integración y no se modifica en esta tarea. No se crea `services/reclamosApi.ts` porque todavía no existe una necesidad ni un contrato confirmado; se añadirá cuando el backend esté disponible, sin inventar endpoints.

## Semana 2

Se añadieron servicios de reclamos y simulación, cliente fetch con AbortController, política de reintentos, errores tipados y métricas técnicas. El producto usa demostración; API sin adaptador informa integración pendiente. No hay endpoints definitivos inventados.

La carga bloquea campos y doble envío. Limpiar cancela; los errores conservan los datos y permiten volver a analizar. Se muestra la latencia local medida y tokens no disponibles.

El cliente HTTP admite timeout configurable (15000 ms por intento), 429 y Retry-After, hasta tres intentos con espera exponencial, conexión, servidor, respuesta vacía o inválida y cancelación. POST y otras operaciones modificadoras no se reintentan sin garantía explícita de idempotencia del servidor y clave estable. Los reintentos incluyen pausas que también pueden cancelarse.

Las métricas guardan identificador, fecha, operación técnica, duración total, estado, código, intentos y tokens. Se conservan hasta 100 registros en memoria y no contienen datos del formulario. Los tokens ausentes se guardan como null, sin estimaciones. obtenerMetricas y limpiarMetricas están disponibles en shared/metrics/metricasService.ts para consumidores internos.

Las pruebas se organizan en tests/reclamos, tests/http y tests/metrics. npm run test comprueba tipos y ejecuta Vitest; la auditoría del 21 de octubre de 2026 tiene 79 pruebas aprobadas. HTTP se prueba con mocks identificados. El laboratorio /tests/manual/escenarios.html funciona solo en el servidor de desarrollo, muestra errores y métricas de prueba y no forma parte de dist.

La documentación completa está en ../docs/semana-2 y las evidencias verificables en ../docs/evidencias/semana-2. Incluyen arquitectura, módulos, manejo de errores, métricas, plan y resultados de pruebas, decisiones e integración pendiente.

## Auditoría final y preparación para GitHub

El 21 de octubre de 2026 se revisó el frontend sin modificar su arquitectura ni actualizar dependencias. TypeScript utiliza `strict`. Editar los datos del formulario descarta el análisis anterior para evitar mostrar hechos desactualizados; los datos introducidos se conservan. Las validaciones del frontend deben repetirse en el backend.

Ejecute también `npm audit`. No coloque credenciales en variables `VITE_`: todo valor expuesto al cliente puede leerse desde el navegador. `.env` y sus variantes están excluidos de Git; `.env.example`, si se necesita, solo debe contener valores públicos de ejemplo. No se necesita ningún archivo de entorno para ejecutar la demostración.

El informe vigente está en `../docs/semana-2/10-auditoria-final.md`. Los documentos y el Word del 20 de octubre conservan las evidencias históricas de 63 pruebas; el informe final registra las 79 actuales. Los recursos de plantilla en `src/assets` y `public/icons.svg` no tienen referencias en la aplicación y se conservan por trazabilidad; no afectan al JavaScript generado.

Para integrar el backend, acuerde primero URL, métodos, esquema de respuestas, errores, CORS, autenticación y métricas. Implemente después el adaptador `analizar` de `ConfiguracionReclamos`, valide los datos desconocidos y llame a `solicitarJson`. No reintente un POST sin deduplicación del servidor y clave estable. No hay conexión a Ollama ni tokens reales en esta entrega.

Para publicar posteriormente, revise primero `git status --short` y el contenido preparado con `git diff --cached`. Versione fuente, pruebas, configuraciones, README, package.json, package-lock.json, documentos y evidencias académicas seleccionadas. Excluya node_modules, dist, entornos privados, logs, cachés, archivos temporales y frontend.zip. La publicación debe realizarla el responsable del repositorio; esta auditoría no ejecuta push ni crea remotos.
