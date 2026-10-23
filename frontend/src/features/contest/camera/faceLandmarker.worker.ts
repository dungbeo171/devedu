import { FaceLandmarker, FilesetResolver } from '@mediapipe/tasks-vision'
import simdLoader from '@mediapipe/tasks-vision/vision_wasm_internal.js?url'
import simdBinary from '@mediapipe/tasks-vision/vision_wasm_internal.wasm?url'
import plainLoader from '@mediapipe/tasks-vision/vision_wasm_nosimd_internal.js?url'
import plainBinary from '@mediapipe/tasks-vision/vision_wasm_nosimd_internal.wasm?url'
import { poseFromMatrix } from './headPose'

let detector: FaceLandmarker | null = null
self.onmessage = async (event: MessageEvent<{ type: 'INIT'; origin: string } | { type: 'FRAME'; bitmap: ImageBitmap; at: number }>) => {
  const message = event.data
  try {
    if (message.type === 'INIT') {
      const simd = await FilesetResolver.isSimdSupported()
      detector = await FaceLandmarker.createFromOptions({
        wasmLoaderPath: new URL(simd ? simdLoader : plainLoader, message.origin).href,
        wasmBinaryPath: new URL(simd ? simdBinary : plainBinary, message.origin).href,
      }, {
        baseOptions: { modelAssetPath: new URL('/models/face_landmarker.task', message.origin).href, delegate: 'CPU' },
        runningMode: 'VIDEO', numFaces: 2, outputFacialTransformationMatrixes: true, outputFaceBlendshapes: false,
        minFaceDetectionConfidence: 0.7, minFacePresenceConfidence: 0.7, minTrackingConfidence: 0.7,
      })
      const emphasized = [
        ...FaceLandmarker.FACE_LANDMARKS_FACE_OVAL,
        ...FaceLandmarker.FACE_LANDMARKS_LEFT_EYE,
        ...FaceLandmarker.FACE_LANDMARKS_RIGHT_EYE,
        ...FaceLandmarker.FACE_LANDMARKS_LEFT_EYEBROW,
        ...FaceLandmarker.FACE_LANDMARKS_RIGHT_EYEBROW,
        ...FaceLandmarker.FACE_LANDMARKS_LIPS,
      ]
      self.postMessage({ type: 'READY', connections: [
        ...FaceLandmarker.FACE_LANDMARKS_TESSELATION.map(({ start, end }) => ({ start, end, emphasized: false })),
        ...emphasized.map(({ start, end }) => ({ start, end, emphasized: true })),
      ] })
    } else {
      if (!detector) throw new Error('Detector is not ready')
      const result = detector.detectForVideo(message.bitmap, message.at)
      self.postMessage({ type: 'POSE', faces: result.faceLandmarks.length,
        pose: result.faceLandmarks.length === 1 ? poseFromMatrix(result.facialTransformationMatrixes[0]?.data ?? []) : null,
        landmarks: result.faceLandmarks.length === 1
          ? result.faceLandmarks[0].map(({ x, y, z }) => ({ x, y, z }))
          : null,
        at: message.at })
    }
  } catch {
    self.postMessage({ type: 'ERROR' })
  } finally {
    if (message.type === 'FRAME') message.bitmap.close()
  }
}
