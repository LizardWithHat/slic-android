package nodomain.betchermartin.tensorflowlitescanner.webserver;

import java.util.List;

import nodomain.betchermartin.tensorflowlitescanner.kernels.Classifier;

public interface ClassifierWebServerCallback {
    List<Classifier.Recognition> onServeReceived(String input);
}
