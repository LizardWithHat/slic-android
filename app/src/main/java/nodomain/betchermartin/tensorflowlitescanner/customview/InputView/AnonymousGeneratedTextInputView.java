package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;

/**
 * Generates a view from a given StringParcelable with an uneditable EditText-Element,
 * no Inputs can be made and should have been made by the caller.
 */
public class AnonymousGeneratedTextInputView extends InputView {
    public AnonymousGeneratedTextInputView(Context context, List<Parcelable> listOfInputs, @Nullable Object extras) {
        super(context, listOfInputs, null);
    }

    @Override
    public View createInputView() {

        ConstraintLayout view = new ConstraintLayout(context);
        EditText textField = new EditText(context);

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
                    ((StringParcelable) listOfInputs.get(0)).setValue(s.toString());
                } else {
                    listOfInputs.add(new StringParcelable(s.toString()));
                }
            }
        });

        return view;
    }
}
