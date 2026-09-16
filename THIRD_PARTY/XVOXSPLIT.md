# XvoxSplit model / runtime notices

XvoxSplit uses real, on-device neural source separation, not an EQ or mid/side approximation.

## Model

- **UVR MDX-Net 9482** — Ultimate Vocal Remover, Anjok07 and contributors; MDX-Net architecture by Kuielab / Woosung Choi.
- The UVR README documents MIT licensing and asks third-party model users to credit UVR and its developers: https://github.com/Anjok07/ultimatevocalremovergui#license
- ONNX model with preprocessing metadata distributed by **sherpa-onnx**, k2-fsa / Xiaomi contributors: https://k2-fsa.github.io/sherpa/onnx/source-separation/models.html
- Pinned download: https://github.com/k2-fsa/sherpa-onnx/releases/download/source-separation-models/UVR_MDXNET_9482.onnx
- Bytes: 29,704,738. SHA-256: `9d78f8566fa8198065214ab628be1de966a500c57786695aa4b13e2b27a7727d`.
- Model contract: 44,100 Hz; periodic Hann; centred STFT, FFT 4096 / hop 1024; `[1,4,2048,256]` float input/output. The predicted output is vocals; instrumental accompaniment is the mixture residual.
- Weights are **not bundled** in the repository or APK. The app downloads them only after explicit setup consent and checks the pinned SHA-256 before inference.

## Runtime and implementation references

- Microsoft **ONNX Runtime Android 1.23.2**, MIT: https://github.com/microsoft/onnxruntime/blob/v1.23.2/LICENSE
- sherpa-onnx source-separation implementation reference, Apache 2.0: https://github.com/k2-fsa/sherpa-onnx/tree/master/sherpa-onnx/csrc
- MDX inference/STFT layout reference, Apache 2.0: https://github.com/seanghay/uvr-mdx-infer

The app's FFT, streaming cache/resampler, overlap stitching, background queue handling and routing are implemented in the XVOX source. No third-party music fixture is redistributed. Only synthetic signals were used for the local inference smoke checks.

## Important limitations

Neural separation can leave bleed and artifacts. “Beat” refers to instrumental accompaniment, not isolated drums. Stereo stems are folded to separate mono channels for left/right routing; prepared files are internal stem-pair WAVs, not ordinary stereo mixes. They use roughly 10 MB per minute, plus temporary decoded audio. Processing throughput, RAM and battery use depend on the phone. A two-track buffer reduces interruptions but does not guarantee that a device can prepare a long queue faster than real time.
