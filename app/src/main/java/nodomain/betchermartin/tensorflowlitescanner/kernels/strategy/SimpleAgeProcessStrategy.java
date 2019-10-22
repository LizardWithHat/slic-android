package nodomain.betchermartin.tensorflowlitescanner.kernels.strategy;

import android.os.Parcelable;

import java.util.List;

import nodomain.betchermartin.tensorflowlitescanner.misc.IntegerParcelable;

/**
 * Takes Integer input from the first element of the list,
 * divides it by 100 and returns it as float
 */
public class SimpleAgeProcessStrategy implements InputProcessStrategy {
    @Override
    public float processInput(List<Parcelable> inputParameter, String ... args) {
        return ((IntegerParcelable) inputParameter.get(0)).getValue() / 100.0f;
    }
}
