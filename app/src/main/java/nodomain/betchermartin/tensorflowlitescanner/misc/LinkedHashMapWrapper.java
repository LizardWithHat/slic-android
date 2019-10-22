package nodomain.betchermartin.tensorflowlitescanner.misc;

import java.io.Serializable;

/**
 *  Wrapper Class for LinkedHashMap to prevent loosing the order by
 *  parsing it to other Activities through an intent's extras.
 *  Taken from: https://stackoverflow.com/questions/12300886/linkedlist-put-into-intent-extra-gets-recast-to-arraylist-when-retrieving-in-nex/12305459#12305459
 */
public class LinkedHashMapWrapper<T extends Serializable> implements Serializable {
    private T wrapped;

    public LinkedHashMapWrapper(T wrapped) {
        this.wrapped = wrapped;
    }

    public T get() {
        return wrapped;
    }
}

