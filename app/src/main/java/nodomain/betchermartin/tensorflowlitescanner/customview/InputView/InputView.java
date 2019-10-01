package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;

import java.util.List;

import androidx.annotation.Nullable;

public abstract class InputView {
    protected List<Parcelable> listOfInputs;
    protected Context context;
    protected Object extras;

    public InputView(Context context, List<Parcelable> listOfInputs, @Nullable Object extras){
        this.context = context;
        this.listOfInputs = listOfInputs;
        this.extras = extras;
    }

    abstract public View createInputView();
}
