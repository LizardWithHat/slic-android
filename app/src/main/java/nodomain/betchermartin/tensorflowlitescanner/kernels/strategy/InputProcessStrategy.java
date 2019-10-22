package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import java.io.Serializable;
import java.util.List;

public interface InputProcessStrategy {
    float processInput(List<Serializable> inputParameter, String ... args);
}
