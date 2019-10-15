package nodomain.betchermartin.tensorflowlitescanner.dataInput;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import nodomain.betchermartin.tensorflowlitescanner.R;
import nodomain.betchermartin.tensorflowlitescanner.datasender.DataSenderInterface;
import nodomain.betchermartin.tensorflowlitescanner.datasender.LocalDataSender;
import nodomain.betchermartin.tensorflowlitescanner.preferences.PreferenceActivity;
import nodomain.betchermartin.tensorflowlitescanner.kernels.Classifier;

import static android.os.Environment.MEDIA_MOUNTED;
import static android.os.Environment.getExternalStorageState;

public class PatientDataInputActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner modelChooser;
    private Classifier.Model chosenModel;
    private Fragment inputMask;
    private DataSenderInterface dataSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_data_input);

        Toolbar toolbar = findViewById(R.id.patientDataToolbar);
        setSupportActionBar(toolbar);

        modelChooser = findViewById(R.id.spinnerModelChooser);
        ArrayAdapter<String> modelChooserAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);

        for(Classifier.Model m : Classifier.Model.values()){
            modelChooserAdapter.add(m.toString());
        }

        modelChooser.setAdapter(modelChooserAdapter);
        modelChooser.setOnItemSelectedListener(this);

        dataSender = LocalDataSender.getInstance();
    }

    @Override
    protected void onDestroy(){
        if(getExternalStorageState().equals(MEDIA_MOUNTED)) {
            File sourceDir = getExternalFilesDir(null);
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(getString(R.string.send_data_preference_key), true)
                    && sourceDir.exists())
                dataSender.compressFiles(new File(sourceDir, "out"));
        }
        super.onDestroy();
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

    private void setUpFragment(Classifier.Model chosenModel) {
        Bundle extras = new Bundle();
        extras.putSerializable(PatientDataInputFragment.CHOSEN_MODEL_KEY, chosenModel);
        if(inputMask != null) getSupportFragmentManager().beginTransaction().remove(inputMask).commit();
        inputMask = new PatientDataInputFragment();
        inputMask.setArguments(extras);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.viewFragmentPlaceholder, inputMask).commit();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        chosenModel = Classifier.Model.valueOf(parent.getItemAtPosition(position).toString().toUpperCase());
        setUpFragment(chosenModel);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
