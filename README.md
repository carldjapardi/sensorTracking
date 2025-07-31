# Sensor Tracking with Neural Network Option

This project records IMU data and generates a path using a basic PDR algorithm. The
`Save` dialog now includes an option to post-process the recorded session with a
neural network model (e.g. L-IONet or RONIN).

To enable this feature, place an ONNX model named `path_model.onnx` in the
`app/src/main/assets` directory. When selected, the app will load the model via
[ONNX Runtime](https://onnxruntime.ai/) and attempt to compute a refined path.
The resulting image is saved alongside the original as `<session>_nn.png`.

The `NeuralPathProcessor` loads an ONNX model and runs inference on the
recorded IMU data. Place a compatible model (for example, a RONIN pre-trained
model converted to ONNX) in `app/src/main/assets/path_model.onnx`. Pre-trained
RONIN weights are available from the [RoNIN project website](https://ronin.cs.sfu.ca/).
The processor converts accelerometer, gyroscope and rotation vector samples to
world coordinates before feeding them to the network.
