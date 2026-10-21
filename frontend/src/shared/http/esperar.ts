import { crearError } from './httpErrors'

export function esperar(milisegundos: number, senal?: AbortSignal): Promise<void> {
  return new Promise((resolver, rechazar) => {
    if (senal?.aborted) {
      rechazar(crearError('cancelacion'))
      return
    }
    // Los temporizadores del navegador aceptan como máximo un entero de 32 bits.
    const inicio = performance.now()
    let temporizador: ReturnType<typeof setTimeout>
    function limpiar() {
      clearTimeout(temporizador)
      senal?.removeEventListener('abort', cancelar)
    }
    function cancelar() {
      limpiar()
      rechazar(crearError('cancelacion'))
    }
    function continuar() {
      const restante = milisegundos - (performance.now() - inicio)
      if (restante > 0) {
        temporizador = setTimeout(continuar, Math.min(restante, 2147483647))
      } else {
        limpiar()
        resolver()
      }
    }
    senal?.addEventListener('abort', cancelar, { once: true })
    temporizador = setTimeout(continuar, Math.min(milisegundos, 2147483647))
  })
}
