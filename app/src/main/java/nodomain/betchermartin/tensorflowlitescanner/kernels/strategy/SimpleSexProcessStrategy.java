package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import java.io.Serializable;
import java.util.List;

/**
 * Evauluates a String input from the first element in the list
 * and outputs 0 if it equals "female" or 1 if not.
 */
public class SimpleSexProcessStrategy implements InputProcessStrategy {
    @Override
    public float processInput(List<Serializable> inputParameter, String ... args) {
        return inputParameter.get(0).equals("female") ? 0.0f : 1.0f;
    }
}
