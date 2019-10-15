package nodomain.betchermartin.tensorflowlitescanner.datasender;

import java.io.File;

public interface DataSenderInterface {
    boolean compressFiles(File sourceFolder);
    boolean sendData(String destination);
}
