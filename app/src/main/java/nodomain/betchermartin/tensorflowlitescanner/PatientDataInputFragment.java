package nodomain.betchermartin.tensorflowlitescanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nodomain.betchermartin.tensorflowlitescanner.env.Logger;
import nodomain.betchermartin.tensorflowlitescanner.httpd.ClassifierWebServerActivity;
import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;
import nodomain.betchermartin.tensorflowlitescanner.tflite.Classifier;

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


    private ArrayList<StringParcelable> headerStrings;
    private ListView lwDataDetails;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = getActivity().getBaseContext();
        headerStrings = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent);
        chosenModel = (Classifier.Model) getArguments().getSerializable(CHOSEN_MODEL_KEY);

        // create unique installation key, if not already created
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

        PatientDataAdapter dataAdapter = new PatientDataAdapter(headerStrings, getContext());
        lwDataDetails = parentActivity.findViewById(R.id.lvPatientDataInput);
        lwDataDetails.setAdapter(dataAdapter);

        Button save = getActivity().findViewById(R.id.butPatiendDataInputSave);
        save.setOnClickListener(s -> {
            // Eingaben versenden
            Intent classifierIntent = new Intent(parent, ClassifierActivity.class);
            classifierIntent.putParcelableArrayListExtra(RESULT_STRING, headerStrings);
            classifierIntent.putExtra(CameraActivity.CHOSENMODEL, chosenModel.toString());
            startActivity(classifierIntent);
        });

        Button startWebServer = getActivity().findViewById(R.id.butStartWebServer);
        startWebServer.setOnClickListener(s -> {
            // Eingaben versenden
            Intent classifierIntent = new Intent(parent, ClassifierWebServerActivity.class);
            classifierIntent.putParcelableArrayListExtra(RESULT_STRING, headerStrings);
            classifierIntent.putExtra(ClassifierWebServerActivity.CHOSENMODEL, chosenModel.toString());
            startActivity(classifierIntent);
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
        // TODO: CSVWriter + Interface implementieren und hier nutzen
        // TODO: InputViewFactory hier nutzen
        // TODO: ImageWriter + Interface implementieren
        // TODO: Strategy Pattern in Classifiers nutzen
        // Erzeuge Header für einzigartige Schlüssel / Bild-ID Spalte
        // und erzeuge einzigartige Patient ID
        headerStrings.add(new StringParcelable(System.currentTimeMillis()+"_"+
                sharedPreferences.getString("UNIQUE_INSTALL", "(NULL)")));
        headerStrings.add(new StringParcelable(""));

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
                        headerStrings.add(intervalDetail);
                        break;
                    case "choice":
                        JSONArray choices = detailRoot.getJSONObject(key).optJSONArray("values");
                        ChoiceDetail choiceDetail = new ChoiceDetail(key, description, "");
                        for(int j = 0; j < choices.length(); j++){
                            choiceDetail.addChoice(choices.getString(j));
                        }
                        headerStrings.add(choiceDetail);
                        break;
                    default:
                        headerStrings.add(new StringParcelable(key, description, ""));
                }
            }
        } catch (IOException e){
            LOGGER.e("File Error: "+e.getMessage());
        } catch (JSONException e){
            LOGGER.e("JSON Error: "+e.getMessage());
        }
    }

    private class PatientDataAdapter extends ArrayAdapter<StringParcelable>{
        ArrayList<StringParcelable> data;
        Context context;

        PatientDataAdapter(ArrayList<StringParcelable> data, Context context){
            super(context, R.layout.patient_data_input_item, data);
            this.data = data;
            this.context = context;
        }

        private class ViewHolder{
            TextView label;
            View inputContainer;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            StringParcelable dataRow = data.get(position);
            ViewHolder vh;
            vh = new ViewHolder();

            convertView = LayoutInflater.from(getContext()).
                    inflate(R.layout.patient_data_input_item, parent, false);
            vh.inputContainer = convertView.findViewById(R.id.inputContainerLayout);
            vh.label = convertView.findViewById(R.id.tlItemTitle);
            vh.label.setText(dataRow.getDescription());

            return convertView;
        }
    }
}
