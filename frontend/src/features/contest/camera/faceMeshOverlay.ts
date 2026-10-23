import type { FaceMeshFrame } from './cameraCapture'

/** Draws the current face mesh locally. Coordinates never leave the browser. */
export function drawFaceMesh(canvas: HTMLCanvasElement, frame: FaceMeshFrame | null,
  width: number, height: number) {
  if (width <= 0 || height <= 0) return
  if (canvas.width !== width) canvas.width = width
  if (canvas.height !== height) canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) return
  context.clearRect(0, 0, width, height)
  if (!frame) return

  context.lineCap = 'round'
  for (const connection of frame.connections) {
    const from = frame.landmarks[connection.start]
    const to = frame.landmarks[connection.end]
    if (!from || !to) continue
    context.beginPath()
    context.moveTo(from.x * width, from.y * height)
    context.lineTo(to.x * width, to.y * height)
    context.lineWidth = connection.emphasized ? 1.7 : 0.65
    context.strokeStyle = connection.emphasized ? 'rgba(74, 222, 128, 0.96)' : 'rgba(34, 197, 94, 0.34)'
    context.stroke()
  }

  context.fillStyle = 'rgba(134, 239, 172, 0.9)'
  for (let index = 0; index < frame.landmarks.length; index += 5) {
    const point = frame.landmarks[index]
    context.beginPath()
    context.arc(point.x * width, point.y * height, 1.1, 0, Math.PI * 2)
    context.fill()
  }
}
