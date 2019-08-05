package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.misc.ChoiceDetail;
import org.tensorflow.lite.examples.classification.misc.IntervalDetail;
import org.tensorflow.lite.examples.classification.misc.SimpleDetail;
import org.tensorflow.lite.examples.classification.preferences.PreferenceActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class PatientDataInputActivity extends AppCompatActivity {

    public static final String JSONFILENAME = "skin-cancer-data-detail.json";
    public static final String RESULT_STRING = "patientdataresultstring";
    private static final Logger LOGGER = new Logger();
    private SharedPreferences sharedPreferences;
    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    // Kamera Genehmigung impliziert auch Licht/Blitz Nutzung
    // private static final String PERMISSION_FLASHLIGHT = Manifest.permission.FLASHLIGHT;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;


    private ArrayList<SimpleDetail> patientData;
    private ListView lwDataDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_data_input);
        patientData = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Erzeuge einzigartige Installations-ID, falls nicht vorhanden
        if(sharedPreferences.getString("UNIQUE_INSTALL", "(NULL)").equals("(NULL)")){
            sharedPreferences.edit().putString("UNIQUE_INSTALL", UUID.randomUUID().toString()).apply();
        }

        if (!hasPermission()) {
            requestPermission();
        }

        createHeaderFromJson(JSONFILENAME);

        Button save = findViewById(R.id.butPatiendDataInputSave);
        save.setOnClickListener(v -> {
            // Eingaben versenden
            Intent classifierIntent = new Intent(this, ClassifierActivity.class);
            classifierIntent.putParcelableArrayListExtra(RESULT_STRING, patientData);
            startActivity(classifierIntent);
        });

        Button cancel = findViewById(R.id.butPatiendDataInputExit);
        cancel.setOnClickListener(v -> finish());

        PatientDataAdapter dataAdapter = new PatientDataAdapter(patientData, getBaseContext());
        lwDataDetails = findViewById(R.id.lvPatientDataInput);
        lwDataDetails.setAdapter(dataAdapter);

        Toolbar toolbar = findViewById(R.id.patientDataToolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length <= 0
                    && grantResults[0] == PackageManager.PERMISSION_DENIED
                    && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &
                checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
            Toast.makeText(
                    this,
                    "Camera permission is required for this App",
                    Toast.LENGTH_LONG)
                    .show();
        }
        requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
    }

    private void createHeaderFromJson(String filename) {
        // Erzeuge Header für einzigartige Schlüssel / Bild-ID Spalte
        // und erzeuge einzigartige Patient ID
        patientData.add(new SimpleDetail("patient_id", "Patienten ID", sharedPreferences.
                getString("UNIQUE_INSTALL", "(NULL)") +"_"+ System.currentTimeMillis()));
        patientData.add(new SimpleDetail("image_id", "Image ID", ""));

        // lade JSON Datei aus Asset Ordner
        JSONObject jObj;
        JSONObject detailRoot;
        InputStream in;
        try{
            in = getAssets().open(filename);
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

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PatientDataInputActivity.this);
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
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PatientDataInputActivity.this);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.dataInput_toolbar_item) {
            startActivity(new Intent(this, PreferenceActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.data_input_toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


}
