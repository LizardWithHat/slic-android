package org.tensorflow.lite.examples.classification.httpd;

import android.util.Size;

import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ClassifierWebServer extends NanoHTTPD {
    private static final Logger LOGGER = new Logger();
    Size recognitionSize;
    ClassifierWebServerCallback callback;

    public ClassifierWebServer(String hostname, int port, Size recognitionSize, ClassifierWebServerCallback callback) {
        super(hostname, port);
        this.recognitionSize = recognitionSize;
        this.callback = callback;
    }

    public ClassifierWebServer(int port, Size recognitionSize, ClassifierWebServerCallback callback) {
        super(port);
        this.recognitionSize = recognitionSize;
        this.callback = callback;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getMethod() == Method.GET) {
            LOGGER.i("Send size in GET-Request.");
            return newFixedLengthResponse(recognitionSize.toString());
        }
        else  if (session.getMethod() == Method.POST) {
            LOGGER.i("Received POST, running inference.");
            //TODO: Hier aus base64 Bild ziehen und Inferenz laufen lassen. Ergebnis als Response
            try {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                if(!files.containsKey("image")) return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "400 - Bad Request. Tag \"image\" not found");
                List<Classifier.Recognition> results = callback.onServeReceived(files.get("image"));
                StringBuilder resultsString = new StringBuilder();
                for(Classifier.Recognition i : results){
                    resultsString.append(i.getTitle());
                    resultsString.append(",");
                    resultsString.append(i.getConfidence());
                    resultsString.append("\n");
                }
                resultsString.trimToSize();
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, resultsString.toString());
            } catch (IOException | ResponseException e) {
                LOGGER.e(e, "Error on POST Body Parse");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "500 - Interal Server Error");
            } catch (NullPointerException e){
                LOGGER.e(e, "Error parsing Image");
                return newFixedLengthResponse(Response.Status.UNSUPPORTED_MEDIA_TYPE, MIME_PLAINTEXT, "415 - Unsupported Media. Did you send a base64 Encoded Bitmap?");
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                "The requested resource does not exist");
    }
}
