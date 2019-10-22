package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import java.io.Serializable;
import java.util.List;

/**
 * Takes String input from the first element of the list and compares it to the
 * args elements. Returns 1 for match, 0 for no match or no arguments.
 */
public class SimpleStringComparisonStrategy implements InputProcessStrategy {
    @Override
    public float processInput(List<Serializable> inputParameter, String ... args) {
        if(args.length == 0) return 0.0f;
        for(String arg : args){
            if(arg.equals(inputParameter.get(0))) return 1.0f;
        }
        return 0.0f;
    }
}
