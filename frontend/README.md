# Frontend del asistente de reclamos de consumo

Aplicación React + TypeScript para iniciar y continuar un reclamo de consumo en Ecuador mediante el backend Spring Boot del proyecto. El frontend no ejecuta el análisis jurídico localmente: valida la entrada, consume la API, presenta los estados de la sesión y descarga el documento generado por el backend.

**Frontend desplegado:** https://generadoreclamos.vercel.app/

## Requisitos y ejecución

- Node.js compatible con las dependencias del proyecto (la entrega fue validada con Node.js 24).
- npm.
- Backend disponible en `https://asistente-inteligente-4kiq.onrender.com` o en `http://localhost:8080` durante el desarrollo local.

Desde esta carpeta:

```powershell
npm ci
npm run dev
```

Para usar la versión pública, abra [https://generadoreclamos.vercel.app/](https://generadoreclamos.vercel.app/). En desarrollo, la aplicación de Vite queda disponible normalmente en `http://localhost:5173`. El cliente usa `https://asistente-inteligente-4kiq.onrender.com` por defecto. Para apuntar a un backend local o a otra instancia, cree `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

No coloque secretos en variables `VITE_`: Vite las expone en el navegador. El `.env.local` no debe versionarse.

## Scripts

```powershell
npm run build   # type-check y compilación de producción en dist/
npm run lint    # ESLint
npm run test    # type-check de tests y Vitest
npm run preview # sirve localmente la compilación de dist/
```

`npm run test` ejecuta las pruebas con un entorno Node y Vitest. Las pruebas del cliente HTTP, reintentos, métricas y flujo de reclamos usan mocks; no sustituyen una prueba de integración contra una instancia real del backend.

## Flujo funcional actual

1. El usuario acepta el aviso de transparencia de IA y completa el relato del incidente. El frontend exige entre 20 y 2000 caracteres; el backend vuelve a validar y procesa únicamente el relato inicial.
2. `POST /api/v1/claims/start` inicia la sesión. Si la Capa 1 necesita contexto, el frontend envía las aclaraciones mediante `/{sessionId}/layer1/clarify`.
3. Cuando la Capa 1 termina, se inicia la Capa 2 con `/{sessionId}/layer2/start`; sus preguntas se responden con `/{sessionId}/layer2/answer`.
4. El usuario puede solicitar la evaluación de viabilidad (`/evaluate/{sessionId}`), revisar el informe y aceptarlo (`/accept/{sessionId}`).
5. La Capa 3 puede presentar opciones de acción (`/select-action/{sessionId}`) y, si corresponde, pedir un formulario procedimental (`/submit-procedural-form/{sessionId}`).
6. La Capa 4 ejecuta cálculos deterministas con `/{sessionId}/layer4/calculate`.
7. La Capa 5 genera y descarga el Word con `/{sessionId}/layer5/generate`. Los datos personales del documento se solicitan en el último tramo y se insertan por código.
8. El progreso puede guardarse con `/{sessionId}/save`, consultarse por identificación con `GET /saved` y reanudarse con `POST /saved/{id}/resume`.

El cliente conserva el token de sesión en memoria mediante `sessionAccess.ts`; no lo guarda en `localStorage`. Recargar la página pierde el acceso de la sesión activa, aunque un reclamo guardado puede reanudarse con su identificación. Las solicitudes tienen timeout, cancelación con `AbortController`, errores tipados, métricas técnicas y reintentos solo donde la operación es segura.

## Estructura

```text
frontend/
├── public/                         # favicon e iconos públicos
├── src/
│   ├── modules/reclamos/
│   │   ├── components/             # formulario, análisis, preguntas, guardado y resultado
│   │   ├── constants/              # valores iniciales y opciones de interfaz
│   │   ├── services/
│   │   │   ├── reclamosApi.ts      # contrato HTTP de las capas 1 a 5
│   │   │   ├── savedClaimsService.ts
│   │   │   ├── savedClaimsMapper.ts
│   │   │   └── sessionAccess.ts
│   │   ├── types/                  # tipos de reclamo y respuestas
│   │   └── utils/                  # validación del formulario
│   ├── shared/http/                # fetch, timeout, errores y política de reintentos
│   ├── shared/metrics/             # métricas técnicas sin datos del formulario
│   ├── App.tsx                     # orquestación del estado visual y del flujo
│   ├── App.css
│   └── main.tsx
├── tests/                          # pruebas unitarias y de flujo con mocks
├── package.json
└── vite.config.ts
```

Editar el formulario invalida el análisis anterior para no mostrar hechos desactualizados. Durante una solicitud se bloquea el doble envío; cancelar o recibir un error conserva los datos introducidos y permite reintentar.

## Privacidad y límites

- No escriba cédula, teléfono, dirección ni otras credenciales en el relato libre: ese texto se envía al modelo a través del backend.
- La identificación, nombre y contacto para guardar o generar el documento se manejan en etapas separadas del relato.
- El frontend no anonimiza el relato, no verifica la vigencia de las fuentes jurídicas y no reemplaza la revisión humana.
- No existe un modo offline ni una base de datos local de reclamos. La sección de reclamos guardados depende de los endpoints de persistencia y PostgreSQL del backend.
- El documento descargado es un borrador de apoyo y no constituye asesoría legal vinculante.

La configuración, API completa, corpus, despliegue y limitaciones del backend están documentados en [`../backend/App/README.md`](../backend/App/README.md) y en [`../README.md`](../README.md).
