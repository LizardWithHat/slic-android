package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import android.os.Parcelable;

import java.util.List;

public interface InputProcessStrategy {
    float processInput(List<Parcelable> inputParameter, String ... args);
}
