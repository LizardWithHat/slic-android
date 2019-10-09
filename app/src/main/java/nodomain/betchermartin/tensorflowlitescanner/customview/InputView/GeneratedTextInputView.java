package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;


/**
 * Generates a view from a given StringParcelable with an already filled out EditText-Element
 */
public class GeneratedTextInputView extends InputView {
    public GeneratedTextInputView(Context context, List<Parcelable> listOfInputs, @Nullable Object extras) {
        super(context, listOfInputs, null);
    }

    @Override
    public View createInputView() {

        LinearLayout view = new LinearLayout(context);
        EditText textField = new EditText(context);

        view.addView(textField);

        if(listOfInputs.size() == 1) textField.setText(((StringParcelable) listOfInputs.get(0)).getValue());


        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
