package nodomain.betchermartin.tensorflowlitescanner.dataInput;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.fragment.app.Fragment;

import nodomain.betchermartin.tensorflowlitescanner.R;
import nodomain.betchermartin.tensorflowlitescanner.cameraclassifier.CameraActivity;
import nodomain.betchermartin.tensorflowlitescanner.cameraclassifier.ClassifierActivity;
import nodomain.betchermartin.tensorflowlitescanner.customview.InputView.InputViewFactory;
import nodomain.betchermartin.tensorflowlitescanner.env.Logger;
import nodomain.betchermartin.tensorflowlitescanner.webserverclassifier.ClassifierWebServerActivity;
import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;
import nodomain.betchermartin.tensorflowlitescanner.kernels.Classifier;

public class PatientDataInputFragment extends Fragment {

    public static final String RESULT_STRING = "patientdataresultstring";
    public static final String CHOSEN_MODEL_KEY = "chosenmodelkey";
    private static final Logger LOGGER = new Logger();
    private SharedPreferences sharedPreferences;
    private Context parent;
    private Classifier.Model chosenModel;

    private LinkedHashMap<String, List<Parcelable>> patientData;
    private LinkedHashMap<String, HashMap<String, Object>> inputItemExtras;
    private ListView lwDataDetails;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parent = getActivity().getBaseContext();
        patientData = new LinkedHashMap<>();
        inputItemExtras = new LinkedHashMap<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent);
        chosenModel = (Classifier.Model) getArguments().getSerializable(CHOSEN_MODEL_KEY);

        // create unique installation key, if not already created
        if(sharedPreferences.getString("UNIQUE_INSTALL", "(NULL)").equals("(NULL)")){
            sharedPreferences.edit().putString("UNIQUE_INSTALL", UUID.randomUUID().toString()).apply();
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance){
        // create Dummy Interpreter to get DataDetailPath
        Classifier model = null;
        try {
            model = Classifier.create(getActivity(), chosenModel, Classifier.Device.CPU, 1, patientData);
        } catch (IOException e) {
            LOGGER.e("Error craeting dummy interpreter: " + e.getMessage());
        }
        model.close();
        patientData.clear();
        inputItemExtras.clear();
        createHeaderFromJson(model.getDataDetailPath());
        return inflater.inflate(R.layout.fragment_patient_data_input, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        Activity parentActivity = getActivity();
        Button cancel = parentActivity.findViewById(R.id.butPatiendDataInputExit);
        cancel.setOnClickListener(s -> getActivity().finish());

        PatientDataAdapter dataAdapter = new PatientDataAdapter(new ArrayList<>(patientData.keySet()), getContext());
        lwDataDetails = parentActivity.findViewById(R.id.lvPatientDataInput);
        lwDataDetails.setAdapter(dataAdapter);

        Button save = getActivity().findViewById(R.id.butPatiendDataInputSave);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Eingaben versenden
                Intent classifierIntent = new Intent(parent, ClassifierActivity.class);
                classifierIntent.putExtra(RESULT_STRING, patientData);
                classifierIntent.putExtra(CameraActivity.CHOSENMODEL, chosenModel.toString());
                startActivity(classifierIntent);
            }
        });

        Button startWebServer = getActivity().findViewById(R.id.butStartWebServer);
        startWebServer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // Eingaben versenden
                Intent classifierIntent = new Intent(parent, ClassifierWebServerActivity.class);
                classifierIntent.putExtra(RESULT_STRING, patientData);
                classifierIntent.putExtra(ClassifierWebServerActivity.CHOSENMODEL, chosenModel.toString());
                startActivity(classifierIntent);
            }
        });

        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connectivityManager.getAllNetworks();
        for(Network network : networks){
            // Button nur aktivieren wenn aktive WIFI oder Ethernet verbindung
            if (connectivityManager.getNetworkCapabilities(network).hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            connectivityManager.getNetworkCapabilities(network).hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                startWebServer.setEnabled(true);
            }
        }
    }

    private void createHeaderFromJson(String filename) {
        // Erzeuge Header für einzigartige Schlüssel / Bild-ID Spalte
        // und erzeuge einzigartige Patient ID
        List<Parcelable> patientIdList = new LinkedList<Parcelable>();
        patientIdList.add(new StringParcelable(System.currentTimeMillis() + "_" +
                sharedPreferences.getString("UNIQUE_INSTALL", "(NULL)")));
        patientData.put("patient_id", patientIdList);
        HashMap<String, Object> patientIdExtras = new HashMap<>();
        patientIdExtras.put("description", "Patienten ID");
        patientIdExtras.put("type", "generatedtext");
        inputItemExtras.put("patient_id", patientIdExtras);

        List<Parcelable> pictureIdList = new LinkedList<Parcelable>();
        pictureIdList.add(new StringParcelable(""));
        patientData.put("picture_id", pictureIdList);
        HashMap<String, Object> pictureIdExtras = new HashMap<>();
        pictureIdExtras.put("description", "Bild ID");
        pictureIdExtras.put("type", "anonymousgeneratedtext");
        inputItemExtras.put("picture_id", pictureIdExtras);

        // lade JSON Datei aus Asset Ordner
        JSONObject jObj;
        JSONObject detailRoot;
        InputStream in;
        try{
            in = new FileInputStream(filename);
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            jObj = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            detailRoot = jObj.getJSONObject("data_details");
            Iterator<String> rootIterator = detailRoot.keys();
            while(rootIterator.hasNext()){
                String key = rootIterator.next();

                List<Parcelable> keyList = new LinkedList<Parcelable>();
                patientData.put(key, keyList);
                HashMap<String, Object> childExtras = new HashMap<>();
                inputItemExtras.put(key, childExtras);

                JSONObject childObject = detailRoot.getJSONObject(key);
                Iterator<String> childIterator = childObject.keys();
                while(childIterator.hasNext()){
                    String subKey = childIterator.next();
                    childExtras.put(subKey, childObject.get(subKey));
                }
            }
        } catch (IOException e){
            LOGGER.e("File Error: "+e.getMessage());
        } catch (JSONException e){
            LOGGER.e("JSON Error: "+e.getMessage());
        }
    }

    private class PatientDataAdapter extends ArrayAdapter<String>{
        ArrayList<String> data;
        Context context;

        PatientDataAdapter(ArrayList<String> data, Context context){
            super(context, R.layout.patient_data_input_item, data);
            this.data = data;
            this.context = context;
        }

        private class ViewHolder{
            TextView label;
            LinearLayout inputContainer;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            List<Parcelable> dataInputList = patientData.get(data.get(position));
            Map<String, Object> dataExtras = inputItemExtras.get(data.get(position));
            ViewHolder vh;
            vh = new ViewHolder();

            convertView = LayoutInflater.from(getContext()).
                    inflate(R.layout.patient_data_input_item, parent, false);
            vh.label = convertView.findViewById(R.id.tlItemTitle);
            vh.label.setText((String) dataExtras.get("description"));

            String type = (String) dataExtras.get("type");
            vh.inputContainer = convertView.findViewById(R.id.inputContainerLayout);
            vh.inputContainer.addView(InputViewFactory.createInputView(this.getContext(), type, dataInputList, dataExtras));

            return convertView;
        }
    }
}
