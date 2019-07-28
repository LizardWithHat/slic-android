package org.tensorflow.lite.examples.classification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.misc.DataDetail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

public class PatientDataInputActivity extends AppCompatActivity {

    public static final String JSONFILENAME = "jsonfilename";
    public static final String RESULT_STRING = "patientdataresultstring";
    private static final Logger LOGGER = new Logger();

    private ArrayList<DataDetail> patientData;
    private ListView lwDataDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_data_input);
        patientData = new ArrayList<>();

        createHeaderFromJson(getIntent().getStringExtra(JSONFILENAME));

        Button save = findViewById(R.id.butPatiendDataInputSave);
        save.setOnClickListener(v -> {
            // Eingaben versenden
            Intent extraData = new Intent();
            extraData.putParcelableArrayListExtra(RESULT_STRING, patientData);
            setResult(RESULT_OK, extraData);
            finish();
        });

        Button cancel = findViewById(R.id.butPatiendDataInputCancel);
        cancel.setOnClickListener(v -> {
            Intent extraData = new Intent();
            setResult(RESULT_CANCELED, extraData);
            finish();
        });

        PatientDataAdapter dataAdapter = new PatientDataAdapter(patientData, getBaseContext());
        lwDataDetails = findViewById(R.id.lvPatientDataInput);
        lwDataDetails.setAdapter(dataAdapter);
    }

    private void createHeaderFromJson(String filename) {
        // Erzeuge Header für einzigartige Schlüssel / Bild-ID Spalte
        // und erzeuge einzigartige Patient ID
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        patientData.add(new DataDetail("patient_id", "Patienten ID", sharedPreferences.
                getString("UNIQUE_INSTALL", "(NULL)") +"_"+ System.currentTimeMillis()));
        patientData.add(new DataDetail("image_id", "Image ID", ""));

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
                patientData.add(new DataDetail(key, description, ""));
            }
        } catch (IOException e){
            LOGGER.e("File Error: "+e.getMessage());
        } catch (JSONException e){
            LOGGER.e("JSON Error: "+e.getMessage());
        }
    }

    private class PatientDataAdapter extends ArrayAdapter<DataDetail>{
        ArrayList<DataDetail> data;
        Context context;

        PatientDataAdapter(ArrayList<DataDetail> data, Context context){
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
            DataDetail dataRow = data.get(position);
            ViewHolder vh;

            if( convertView == null){
                vh = new ViewHolder();

                convertView = LayoutInflater.from(getContext()).
                        inflate(R.layout.patient_data_input_item, parent, false);

                vh.input = convertView.findViewById(R.id.tfItemInput);
                vh.label = convertView.findViewById(R.id.tlItemTitle);

                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            vh.label.setText(dataRow.getDescription());
            vh.input.setHint(dataRow.getKey());
            if(dataRow.getKey().equals("patient_id")) vh.input.setText(dataRow.getValue());
            if(dataRow.getKey().equals("image_id")){
                vh.input.setText("------------");
                vh.input.setEnabled(false);
            }

            vh.input.addTextChangedListener(new TextWatcher(){

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
