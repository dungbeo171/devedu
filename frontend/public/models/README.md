# Face Landmarker model

Used only by the optional DevEdu Contest camera MVP. No images, landmarks or
video are uploaded. Downloaded model and WASM are served from DevEdu's origin,
not from a CDN at runtime.

- Official guide/model cards: https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker#models
- Pinned model: https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task
- SHA-256: `64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff`
- Runtime: `@mediapipe/tasks-vision@1.1.0` (Apache-2.0), pinned in package-lock.json.
- WASM/loader assets are imported by Vite directly from this package and emitted
  as hashed assets. Keep model provenance when updating or redistributing it.

The model locates faces; DevEdu's separate head-pose heuristic is not the
MP-HEADGAZE gaze-estimation model. It does not identify people, classify emotions,
estimate attention, or reliably detect eye movements/cheating.
