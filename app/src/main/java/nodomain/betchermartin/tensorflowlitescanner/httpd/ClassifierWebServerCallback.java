package nodomain.betchermartin.tensorflowlitescanner.httpd;

import nodomain.betchermartin.tensorflowlitescanner.tflite.Classifier;

import java.util.List;

public interface ClassifierWebServerCallback {
    List<Classifier.Recognition> onServeReceived(String input);
}
