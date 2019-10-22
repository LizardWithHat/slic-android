package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import android.os.Parcelable;

import java.util.List;

import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;

/**
 * Evauluates a String input from the first element in the list
 * and outputs 0 if it equals "female" or 1 if not.
 */
public class SimpleSexProcessStrategy implements InputProcessStrategy {
    @Override
    public float processInput(List<Parcelable> inputParameter, String ... args) {
        return ((StringParcelable) inputParameter.get(0)).getValue().equals("female") ? 0.0f : 1.0f;
    }
}
