export function MensajeError({ mensaje }: { mensaje: string }) {
  return (
    <div className="error-message" role="alert">
      {mensaje}
    </div>
  )
}
