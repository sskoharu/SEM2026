# Información Pública y Orientación Procedimental de la Defensoría del Pueblo del Ecuador (DPE)

> **Propósito de este documento:** Proveer el contexto práctico, procedimental y operativo de la Defensoría del Pueblo para enriquecer la **Capa 3 (Ruta Operativa)** y la **Capa 5 (Generador de Salidas / Guías de Acción)** del sistema agéntico de reclamos de consumo bajo la LODC.

---

## 1. Naturaleza Jurídica y Rol de la DPE en Consumo

- **Naturaleza de la Información:** La información de la DPE no altera el articulado de la LODC ni crea derecho sustantivo. Constituye una **guía procedimental y práctica** para que el ciudadano ejerza sus derechos de consumo.
- **Mecanismos de Protección:** La DPE actúa como canal de **mediación, conciliación y gestión defensorial** (mecanismos alternativos de solución de conflictos) antes de escalar a la vía judicial ante los Jueces de Contravenciones (Arts. 81 y 82 de la LODC).

---

## 2. Descripción del Trámite Institucional

- **Nombre del Trámite:** Atención de peticiones por posibles vulneraciones a los derechos humanos, de la naturaleza y del consumidor.
- **Objetivo Práctico:** Brindar asesoría defensorial, convocar a audiencias de mediación con proveedores y tutelar derechos vulnerados.
- **Costo:** Gratuito.
- **Fuentes Oficiales de Referencia:**
  - **Portal Único de Trámites:** [Guía Oficial GOB.EC - Atención de Peticiones DPE](https://www.gob.ec/dpe/tramites/atencion-peticiones-posibles-vulneraciones-derechos-humanos-naturaleza/)
  - **Sitio Web Institucional:** [dpe.gob.ec](https://www.defensoria.gob.ec)

---

## 3. Requisitos Exigidos para la Gestión del Reclamo

Para alimentar los parámetros de la **Capa 2** y construir los expedientes en el sistema agéntico, el usuario debe presentar:

1. **Documento de Identificación:** Cédula de ciudadanía, identidad o pasaporte.
2. **Prueba de la Relación de Consumo:** Factura, contrato, nota de venta, comprobante de pago o comprobante de transferencia bancaria que demuestre la transacción con el proveedor.
3. **Relato Cronológico de los Hechos:** Descripción sucinta del problema (defecto del bien, negativa de garantía, cobro indebido, interrupción de servicio, etc.).
4. **Respaldo de Reclamo Previo (Opcional/Recomendado):** Copia o registro del reclamo presentado directamente al proveedor (ej. hoja del Libro de Reclamos del establecimiento).

---

## 4. Canales de Atención Habilitados

El agente en la **Capa 5** debe indicar las siguientes vías de ingreso formal del trámite:

- **Canal Virtual / En Línea:** A través del Portal Único de Trámites (GOB.EC) o la plataforma web institucional ([dpe.gob.ec](https://www.defensoria.gob.ec)), permitiendo la carga de expedientes digitalizados las 24 horas.
- **Canal Presencial:** Delegaciones provinciales, coordinaciones zonales y centros de mediación de la Defensoría del Pueblo en todo el territorio nacional de Ecuador.

---

## 5. Ciclo de Vida del Trámite y siguientes pasos sugeridos

El sistema agéntico debe estructurar la guía de acción del usuario según las fases operativas de la DPE:

```
[ Reclamo Previo en Local / Libro de Reclamos ]
                       │
                       ▼ (Si no responde o rechaza)
[ Ingreso de Petición en DPE (Virtual / Presencial) ]
                       │
                       ▼
[ Notificación al Proveedor y Convocatoria a Mediación ]
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
    [ Acuerdo de Mediación ]   [ Sin Acuerdo ]
             │                   │
             ▼                   ▼
[ Acta con efecto de sentencia ] [ Emisión de Informe Motivado (Art. 83 LODC) ]
                                 │
                                 ▼
                   [ Demanda ante Juez de Contravenciones ]
```

1. **Fase 1 (Ingreso y Admisibilidad):** Presentación de requisitos y validación de competencia en consumo por parte de la DPE.
2. **Fase 2 (Convocatoria):** La DPE notifica al proveedor y fija fecha para audiencia de mediación.
3. **Fase 3 (Audiencia de Conciliación):** Espacio de diálogo asistido entre consumidor y proveedor para acordar reparación, reposición o devolución económica.
4. **Fase 4 (Cierre):**
   - **Con Acuerdo:** Firma de Acta de Mediación (con fuerza ejecutable).
   - **Sin Acuerdo:** La DPE emite un **Informe Motivado** (Art. 83 LODC) que servirá como prueba clave a favor del consumidor en la vía judicial (Jueces de Contravenciones / Art. 84 LODC).

---

## 6. Integración Técnica en la Arquitectura Agéntica

- **Colección Vectorial Dedicada (Qdrant):** 
  - Colección: `dpe_orientacion`
  - `documento_tipo`: `ORIENTACION_DPE`
  - Ingesta separada del corpus legal rígido de la LODC para no contaminar la clasificación de infracciones.
- **Uso en Capa 3 (Ruta Operativa):** Proporciona la lógica de escalamiento institucional (Defensoría del Pueblo como paso intermedio previo a la demanda judicial).
- **Uso en Capa 5 (Generador de Salidas):** Inyecta la sección **"Siguientes Pasos y Gestión ante la Defensoría del Pueblo"** en el documento o informe final que recibe el consumidor, incluyendo requisitos, canales y plantilla para solicitar la mediación.