package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import java.io.Serializable;
import java.util.List;

/**
 * Takes Integer input from the first element of the list,
 * divides it by 100 and returns it as float
 */
public class SimpleAgeProcessStrategy implements InputProcessStrategy {
    @Override
    public float processInput(List<Serializable> inputParameter, String ... args) {
        return (Integer) inputParameter.get(0) / 100.0f;
    }
}
