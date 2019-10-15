package nodomain.betchermartin.tensorflowlitescanner.customview.InputView;

import android.content.Context;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import nodomain.betchermartin.tensorflowlitescanner.R;
import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;

/**
 * Creates a View with an EditText that, on Click, spawns a Dialog with a configurable List of
 * String Choices to pick a String from and writes the Choice to the first Input.
 * Expects HashMap<String, List<String>> in extras with following keys:
 *      "choices"          : all available choices
 */
public class ChoiceInputView extends InputView {
    public ChoiceInputView(Context context, List<Parcelable> listOfInputs, Map<String, Object> extras) {
        super(context, listOfInputs, extras);
    }

    @Override
    public View createInputView() {
        LinearLayout view = new LinearLayout(context);
        EditText textField = new EditText(context);
        view.addView(textField);

        // Feld nicht Editierbar, aber anklickbar für onClick-Methode stellen
        textField.setFocusable(false);
        textField.setFocusableInTouchMode(false);
        textField.setClickable(false);

        textField.setOnClickListener(v -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setTitle(context.getString(R.string.choose_list));

            JSONArray jsonArray = (JSONArray) extras.get("values");
            List<String> choices = new ArrayList<>();
            for(int i = 0; i < jsonArray.length(); i++){
                try {
                    choices.add(jsonArray.getString(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            dialogBuilder.setItems(choices.toArray(new String[choices.size()]), (dialogInterface, i) -> ((EditText) v).setText(choices.get(i)));
            AlertDialog dialog = dialogBuilder.create();
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
                    ((StringParcelable) listOfInputs.get(0)).setValue(s.toString());
                } else {
                    listOfInputs.add(new StringParcelable(s.toString()));
                }
            }
        });

        return view;
    }
}