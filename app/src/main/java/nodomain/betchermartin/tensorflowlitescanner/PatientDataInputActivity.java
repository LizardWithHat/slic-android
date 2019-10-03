package nodomain.betchermartin.tensorflowlitescanner;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;

import nodomain.betchermartin.tensorflowlitescanner.env.DataSenderInterface;
import nodomain.betchermartin.tensorflowlitescanner.env.LocalDataSender;
import nodomain.betchermartin.tensorflowlitescanner.preferences.PreferenceActivity;
import nodomain.betchermartin.tensorflowlitescanner.tflite.Classifier;

public class PatientDataInputActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner modelChooser;
    private Classifier.Model chosenModel;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    // Kamera Genehmigung impliziert auch Licht/Blitz Nutzung
    // private static final String PERMISSION_FLASHLIGHT = Manifest.permission.FLASHLIGHT;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    // Um den Webserver zu benutzen muss die WiFi Nutzung genehmigt werden.
    private static final String PERMISSION_WIFI = Manifest.permission.ACCESS_WIFI_STATE;
    private static final String PERMISSION_INTERNET = Manifest.permission.INTERNET;
    private static final String PERMISSION_NETWORK = Manifest.permission.ACCESS_NETWORK_STATE;
    private static final int PERMISSIONS_REQUEST = 1;
    private Fragment inputMask;
    private DataSenderInterface dataSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_data_input);

        Toolbar toolbar = findViewById(R.id.patientDataToolbar);
        setSupportActionBar(toolbar);

        modelChooser = findViewById(R.id.spinnerModelChooser);
        modelChooser.setOnItemSelectedListener(this);

        if (!hasPermission()) {
            requestPermission();
        }
        dataSender = LocalDataSender.getInstance();
    }

    @Override
    protected void onDestroy(){
        File sourceDir = new File(Environment.getExternalStorageDirectory(), "SkinCancerScanner");
        if(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.send_data_preference_key), true))
            dataSender.compressFiles(new File(sourceDir, "out"));
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
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length <= 0
                    && grantResults[0] == PackageManager.PERMISSION_DENIED
                    && grantResults[1] == PackageManager.PERMISSION_DENIED
                    && grantResults[2] == PackageManager.PERMISSION_DENIED
                    && grantResults[3] == PackageManager.PERMISSION_DENIED
                    && grantResults[4] == PackageManager.PERMISSION_DENIED) {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &
                ContextCompat.checkSelfPermission(this, PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED &
                ContextCompat.checkSelfPermission(this, PERMISSION_WIFI) == PackageManager.PERMISSION_GRANTED &
                ContextCompat.checkSelfPermission(this, PERMISSION_INTERNET) == PackageManager.PERMISSION_GRANTED &
        ContextCompat.checkSelfPermission(this, PERMISSION_NETWORK) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_CAMERA)) {
            Toast.makeText(
                    this,
                    "Camera permission is required for this App",
                    Toast.LENGTH_LONG)
                    .show();
        }
        ActivityCompat.requestPermissions(this, new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE, PERMISSION_WIFI}, PERMISSIONS_REQUEST);
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
