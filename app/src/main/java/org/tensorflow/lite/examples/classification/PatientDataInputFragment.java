package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.misc.ChoiceDetail;
import org.tensorflow.lite.examples.classification.misc.IntervalDetail;
import org.tensorflow.lite.examples.classification.misc.SimpleDetail;
import org.tensorflow.lite.examples.classification.preferences.PreferenceActivity;
import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class PatientDataInputFragment extends Fragment {

    public static final String RESULT_STRING = "patientdataresultstring";
    public static final String CHOSEN_MODEL_KEY = "chosenmodelkey";
    private static final Logger LOGGER = new Logger();
    private SharedPreferences sharedPreferences;
    private Context parent;
    private Classifier.Model chosenModel;


    private ArrayList<SimpleDetail> patientData;
    private ListView lwDataDetails;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = getActivity().getBaseContext();
        patientData = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent);
        chosenModel = (Classifier.Model) getArguments().getSerializable(CHOSEN_MODEL_KEY);

        //Erzeuge einzigartige Installations-ID, falls nicht vorhanden
        if(sharedPreferences.getString("UNIQUE_INSTALL", "(NULL)").equals("(NULL)")){
            sharedPreferences.edit().putString("UNIQUE_INSTALL", UUID.randomUUID().toString()).apply();
        }

        // create Dummy Interpreter to get DataDetailPath
        Classifier model = null;
        try {
            model = Classifier.create(getActivity(), chosenModel, Classifier.Device.CPU, 1);
        } catch (IOException e) {
            LOGGER.e("Error craeting dummy interpreter: " + e.getMessage());
        }
        createHeaderFromJson(model.getDataDetailPath());
        model.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance){
        return inflater.inflate(R.layout.fragment_patient_data_input, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        Activity parentActivity = getActivity();
        Button cancel = parentActivity.findViewById(R.id.butPatiendDataInputExit);
        cancel.setOnClickListener(s -> getActivity().finish());

        PatientDataAdapter dataAdapter = new PatientDataAdapter(patientData, getContext());
        lwDataDetails = parentActivity.findViewById(R.id.lvPatientDataInput);
        lwDataDetails.setAdapter(dataAdapter);

        Button save = getActivity().findViewById(R.id.butPatiendDataInputSave);
        save.setOnClickListener(s -> {
            // Eingaben versenden
            Intent classifierIntent = new Intent(parent, ClassifierActivity.class);
            classifierIntent.putParcelableArrayListExtra(RESULT_STRING, patientData);
            classifierIntent.putExtra(CameraActivity.CHOSENMODEL, chosenModel.toString());
            startActivity(classifierIntent);
        });

    }

    private void createHeaderFromJson(String filename) {
        // Erzeuge Header für einzigartige Schlüssel / Bild-ID Spalte
        // und erzeuge einzigartige Patient ID
        patientData.add(new SimpleDetail("patient_id", "Patienten ID",  System.currentTimeMillis()+"_"+
                sharedPreferences.getString("UNIQUE_INSTALL", "(NULL)")));
        patientData.add(new SimpleDetail("image_id", "Image ID", ""));

        // lade JSON Datei aus Asset Ordner
        JSONObject jObj;
        JSONObject detailRoot;
        InputStream in;
        try{
            in = parent.getAssets().open(filename);
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            jObj = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            detailRoot = jObj.getJSONObject("data_details");
            Iterator<String> i = detailRoot.keys();
            while(i.hasNext()){
                String key = i.next();
                String description = detailRoot.getJSONObject(key).getString("description");
                String type = detailRoot.getJSONObject(key).getString("type");
                switch(type){
                    case "interval":
                        JSONArray range = detailRoot.getJSONObject(key).optJSONArray("range");
                        IntervalDetail intervalDetail = new IntervalDetail(key, description, "");
                        intervalDetail.setStep(Integer.parseInt(detailRoot.getJSONObject(key).getString("step")));
                        intervalDetail.setIntervalMin(range.optInt(0));
                        intervalDetail.setIntervalMax(range.optInt(1));
                        patientData.add(intervalDetail);
                        break;
                    case "choice":
                        JSONArray choices = detailRoot.getJSONObject(key).optJSONArray("values");
                        ChoiceDetail choiceDetail = new ChoiceDetail(key, description, "");
                        for(int j = 0; j < choices.length(); j++){
                            choiceDetail.addChoice(choices.getString(j));
                        }
                        patientData.add(choiceDetail);
                        break;
                    default:
                        patientData.add(new SimpleDetail(key, description, ""));
                }
            }
        } catch (IOException e){
            LOGGER.e("File Error: "+e.getMessage());
        } catch (JSONException e){
            LOGGER.e("JSON Error: "+e.getMessage());
        }
    }

    private class PatientDataAdapter extends ArrayAdapter<SimpleDetail>{
        ArrayList<SimpleDetail> data;
        Context context;

        PatientDataAdapter(ArrayList<SimpleDetail> data, Context context){
            super(context, R.layout.patient_data_input_item, data);
            this.data = data;
            this.context = context;
        }

        private class ViewHolder{
            TextView label;
            EditText input;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            SimpleDetail dataRow = data.get(position);
            ViewHolder vh;
            vh = new ViewHolder();

            convertView = LayoutInflater.from(getContext()).
                    inflate(R.layout.patient_data_input_item, parent, false);
            vh.input = convertView.findViewById(R.id.tfItemInput);
            vh.label = convertView.findViewById(R.id.tlItemTitle);
            vh.label.setText(dataRow.getDescription());
            vh.input.setHint(dataRow.getKey());
            vh.input.setText(dataRow.getValue());
            if(dataRow.getKey().equals("patient_id")) { vh.input.setText(dataRow.getValue()); }
            else if(dataRow.getKey().equals("image_id")){
                vh.input.setText("------------");
                vh.input.setEnabled(false);
            } else if(dataRow instanceof IntervalDetail){
                // Feld nicht Editierbar, aber anklickbar für onClick-Methode stellen
                vh.input.setFocusable(false);
                vh.input.setFocusableInTouchMode(false);
                vh.input.setClickable(false);

                vh.input.setOnClickListener(v -> {
                    View dialogView = getLayoutInflater().inflate(R.layout.number_picker_dialog_layout, null);
                    NumberPicker numberPicker = dialogView.findViewById(R.id.numberPicker);

                    int step = ((IntervalDetail) dataRow).getStep();
                    int intervalMin = ((IntervalDetail) dataRow).getIntervalMin();
                    int intervalMax = (((IntervalDetail) dataRow).getIntervalMax() - intervalMin) / step;
                    NumberPicker.Formatter formatter = value -> Integer.toString((value * step)+intervalMin);
                    numberPicker.setFormatter(formatter);
                    numberPicker.setMinValue(intervalMin);
                    numberPicker.setMaxValue(intervalMax);

                    numberPicker.setWrapSelectorWheel(true);

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                    AlertDialog dialog;
                    dialogBuilder.setView(dialogView);
                    dialogBuilder.setTitle(getString(R.string.choose_number));
                    dialogBuilder.setPositiveButton(getString(R.string.set), (dialog1, which) -> ((EditText) v).setText(Integer.toString(numberPicker.getValue() * step + intervalMin)));
                    dialogBuilder.setNegativeButton(getString(R.string.cancel), (dialog12, which) -> dialog12.dismiss());
                    dialog = dialogBuilder.create();
                    dialog.show();
                });
            } else if(dataRow instanceof ChoiceDetail){
                // Feld nicht Editierbar, aber anklickbar für onClick-Methode stellen
                vh.input.setFocusable(false);
                vh.input.setFocusableInTouchMode(false);
                vh.input.setClickable(false);

                vh.input.setOnClickListener(v -> {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                    dialogBuilder.setTitle(getString(R.string.choose_list));
                    String[] choices = ((ChoiceDetail) dataRow).getChoices().toArray(new String[0]);
                    dialogBuilder.setItems(choices, (dialogInterface, i) -> ((EditText) v).setText(choices[i]));
                    AlertDialog dialog = dialogBuilder.create();
                    dialog.show();
                });
            }
            vh.input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    dataRow.setValue(s.toString());
                }
            });


            return convertView;
        }
    }
}
