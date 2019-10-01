package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;

import java.util.List;

import androidx.annotation.Nullable;

public class InputViewFactory {
    public static View createInputView(Context context, String type, List<Parcelable> inputList){
        switch(type.toLowerCase()){
            // Register new InputView Classes here
            case "text":
                return new TextInputView(context, inputList, null).createInputView();
            case "generatedtext":
                return new GeneratedTextInputView(context, inputList, null).createInputView();
            case "anonymousgeneratedtext":
                return new AnonymousGeneratedTextInputView(context, inputList, null).createInputView();
            default:
                throw new ExceptionInInitializerError();
        }
    }

    public static View createInputView(Context context, String type, List<Parcelable> inputList, @Nullable Object extras){
        switch(type.toLowerCase()){
            // Register new InputView Classes with Extra Elements here
            case "choice":
                return new ChoiceInputView(context, inputList, extras).createInputView();
            case "interval":
                return new IntervalInputView(context, inputList, extras).createInputView();
            default:
                throw new ExceptionInInitializerError();
        }
    }
}
