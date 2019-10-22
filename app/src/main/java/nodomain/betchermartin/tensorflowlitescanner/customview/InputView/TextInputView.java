package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 *  Creates a View with an empty EditText-Element, Inputs are put into a String-Element in
 *  the first "listOfInputs" position
 */
public class TextInputView extends InputView {
    public TextInputView(Context context, List<Serializable> listOfInputs, @Nullable Object extras) {
        super(context, listOfInputs, null);
    }

    @Override
    public View createInputView() {

        LinearLayout view = new LinearLayout(context);
        EditText textField = new EditText(context);

        view.addView(textField);

        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(listOfInputs.size() > 0) {
                    listOfInputs.set(0, s.toString());
                } else {
                    listOfInputs.add(s.toString());
                }
            }
        });

        return view;
    }
}
