package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import nodomain.betchermartin.tensorflowlitescanner.R;

/**
 * Creates a View with an EditText that, on Click, spawns a Dialog with a configurable Number Wheel
 * to pick a Number from, and writes the selected Number to the first Input.
 * Expects HashMap<String, String> in extras with following keys:
 *      "step"          : Steps between values
 *      "intervalMax"   : Maximum value
 *      "intervalMin"   : Minimum value
 */
public class IntervalInputView extends InputView {
    public IntervalInputView(Context context, List<Serializable> listOfInputs, Map<String, Object> extras) {
        super(context, listOfInputs, extras);
    }

    @Override
    public View createInputView() {
        LinearLayout view = new LinearLayout(context);
        EditText textField = new EditText(context);
        textField.setSingleLine();
        textField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        view.addView(textField);

        // Feld nicht Editierbar, aber anklickbar für onClick-Methode stellen
        textField.setFocusable(false);
        textField.setFocusableInTouchMode(false);
        textField.setClickable(false);
        textField.setOnClickListener(v -> {
            LayoutInflater inflater = context.getSystemService(LayoutInflater.class);
            View dialogView = inflater.inflate(R.layout.number_picker_dialog_layout, null);
            NumberPicker numberPicker = dialogView.findViewById(R.id.numberPicker);

            int step = (int) extras.get("step");
            int intervalMin = (int) extras.get("intervalMin");
            int intervalMax = ((int) extras.get("intervalMax") - intervalMin) / step;
            NumberPicker.Formatter formatter = value -> Integer.toString((value * step)+intervalMin);
            numberPicker.setFormatter(formatter);
            numberPicker.setMinValue(intervalMin);
            numberPicker.setMaxValue(intervalMax);

            numberPicker.setWrapSelectorWheel(true);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            AlertDialog dialog;
            dialogBuilder.setView(dialogView);
            dialogBuilder.setTitle(context.getString(R.string.choose_number));
            dialogBuilder.setPositiveButton(context.getString(R.string.set), (dialog1, which) -> ((EditText) v).setText(Integer.toString(numberPicker.getValue() * step + intervalMin)));
            dialogBuilder.setNegativeButton(context.getString(R.string.cancel), (dialog12, which) -> dialog12.dismiss());
            dialog = dialogBuilder.create();
            dialog.show();
        });

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
                    listOfInputs.set(0, Integer.parseInt(s.toString()));
                } else {
                    listOfInputs.add(Integer.parseInt(s.toString()));
                }
            }
        });

        return view;
    }
}
