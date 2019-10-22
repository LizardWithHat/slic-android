package nodomain.betchermartin.tensorflowlitescanner.metadatawriter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

public interface MetaDataWriterInterface {
    boolean writeMetaData(LinkedHashMap<String, List<Serializable>> listOfObjects);
}
