package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.view.View;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public abstract class InputView {
    protected List<Serializable> listOfInputs;
    protected Context context;
    protected Map<String, Object> extras;

    public InputView(Context context, List<Serializable> listOfInputs, Map<String, Object> extras){
        this.context = context;
        this.listOfInputs = listOfInputs;
        this.extras = extras;
    }

    abstract public View createInputView();
}
