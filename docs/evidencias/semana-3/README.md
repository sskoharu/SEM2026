# Evidencias de Semana 3

Las pruebas de software se registran en resultados/validacion.json y sus logs. calculos.json contiene quince referencias sintéticas ejecutadas por Java. evaluation.json y evaluation-summary.md explicitan que las veinte consultas RAG reales no se ejecutaron; no contienen respuestas fabricadas.

Los dobles de IA, Qdrant y Redis prueban conexiones y controles del código, no precisión del proveedor ni vigencia del corpus. FiveLayerWorkflowTest integra servicios reales y POI; flujoCompleto.test.tsx recorre la interfaz con respuestas HTTP sintéticas. SessionPersistenceTest comprueba el mapping Redis sin servidor.

Para reproducir: Maven clean verify desde backend/App; npm ci, npm run build y npm test desde frontend; node --test eval/evaluador.test.mjs desde la raíz. La corrida externa requiere una instancia disponible y observaciones revisadas.
