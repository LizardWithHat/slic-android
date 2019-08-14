package org.tensorflow.lite.examples.classification;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.tensorflow.lite.examples.classification.preferences.PreferenceActivity;
import org.tensorflow.lite.examples.classification.tflite.Classifier;

public class PatientDataInputActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner modelChooser;
    private Classifier.Model chosenModel;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    // Kamera Genehmigung impliziert auch Licht/Blitz Nutzung
    // private static final String PERMISSION_FLASHLIGHT = Manifest.permission.FLASHLIGHT;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int PERMISSIONS_REQUEST = 1;
    private Fragment inputMask;

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
        boolean firstReplacement = false;
        if(inputMask == null) firstReplacement = true;
        inputMask = new PatientDataInputFragment();
        inputMask.setArguments(extras);
        if(firstReplacement) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.viewFragmentPlaceholder, inputMask).commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentDataInput, inputMask).commit();
        }
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        chosenModel = Classifier.Model.valueOf(parent.getItemAtPosition(position).toString().toUpperCase());
        setUpFragment(chosenModel);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
