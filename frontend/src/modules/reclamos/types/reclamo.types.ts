export type Tipo = 'producto' | 'servicio' | 'garantia' | 'otro'
export type Estado = 'inicio' | 'cargando' | 'preguntas' | 'final' | 'error'
export type Reclamo = {
  nombre: string
  correo: string
  telefono: string
  cedula: string
  tipo: Tipo
  producto: string
  proveedor: string
  fecha: string
  valor: string
  comprobante: boolean
  tipoComprobante: string
  numeroComprobante: string
  descripcion: string
  solucion: string
  otraSolucion: string
}
export type Pregunta = {
  id: string
  texto: string
}
export type Analisis = {
  hechos: string[]
  preguntas: Pregunta[]
  orientaciones: string[]
}
