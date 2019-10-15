package nodomain.betchermartin.tensorflowlitescanner.metadatawriter;

import android.os.Parcelable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public interface MetaDataWriterInterface {
    boolean writeMetaData(LinkedHashMap<String, List<Parcelable>> listOfObjects);
}
