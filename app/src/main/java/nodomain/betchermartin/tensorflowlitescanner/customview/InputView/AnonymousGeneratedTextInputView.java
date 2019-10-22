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
 * Generates a view from a given String with an uneditable EditText-Element,
 * no Inputs can be made and should have been made by the caller.
 */
public class AnonymousGeneratedTextInputView extends InputView {
    public AnonymousGeneratedTextInputView(Context context, List<Serializable> listOfInputs, @Nullable Object extras) {
        super(context, listOfInputs, null);
    }

    @Override
    public View createInputView() {

        LinearLayout view = new LinearLayout(context);
        EditText textField = new EditText(context);
        textField.setSingleLine();
        textField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        view.addView(textField);

        textField.setText("----------");
        textField.setEnabled(false);

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
