package org.tensorflow.lite.examples.classification.httpd;

import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.InputStream;
import java.util.List;

public interface ClassifierWebServerCallback {
    List<Classifier.Recognition> onServeReceived(String input);
}
