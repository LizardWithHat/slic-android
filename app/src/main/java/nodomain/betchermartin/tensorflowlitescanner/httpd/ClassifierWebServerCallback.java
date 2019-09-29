package nodomain.betchermartin.tensorflowlitescanner.httpd;

import java.io.InputStream;
import java.util.List;

import nodomain.betchermartin.tensorflowlitescanner.tflite.Classifier;

public interface ClassifierWebServerCallback {
    List<Classifier.Recognition> onServeReceived(String input);
}
