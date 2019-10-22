package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import android.os.Parcelable;

import java.util.List;

import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;

/**
 * Takes String input from the first element of the list and compares it to the
 * args elements. Returns 1 for match, 0 for no match or no arguments.
 */
public class SimpleStringComparisonStrategy implements InputProcessStrategy {
    @Override
    public float processInput(List<Parcelable> inputParameter, String ... args) {
        if(args.length == 0) return 0.0f;
        for(String arg : args){
            if(arg.equals(((StringParcelable) inputParameter.get(0)).getValue())) return 1.0f;
        }
        return 0.0f;
    }
}
