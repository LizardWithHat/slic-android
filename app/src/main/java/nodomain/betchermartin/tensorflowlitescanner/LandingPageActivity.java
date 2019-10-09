package nodomain.betchermartin.tensorflowlitescanner;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import nodomain.betchermartin.tensorflowlitescanner.env.Logger;

public class LandingPageActivity extends AppCompatActivity {
    //TODO make Async Task for copying and properly show animation

    private static final Logger LOGGER = new Logger();

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    // Kamera Genehmigung impliziert auch Licht/Blitz Nutzung
    // private static final String PERMISSION_FLASHLIGHT = Manifest.permission.FLASHLIGHT;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    // Um den Webserver zu benutzen muss die WiFi Nutzung genehmigt werden.
    private static final String PERMISSION_WIFI = Manifest.permission.ACCESS_WIFI_STATE;
    private static final String PERMISSION_INTERNET = Manifest.permission.INTERNET;
    private static final String PERMISSION_NETWORK = Manifest.permission.ACCESS_NETWORK_STATE;
    private static final int PERMISSIONS_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        if (!hasPermission()) {
            requestPermission();
        }

        createNotificationChannel();
    }

    @Override
    protected void onResume(){
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sharedPreferences.getBoolean(getString(R.string.initialSetup), false)){
            if(copyAssetsToExternalDir() == true) {
                sharedPreferences.edit().putBoolean(getString(R.string.initialSetup), true).commit();
            } else {
                Toast.makeText(this, "Copying Assets failed, abort.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        if(sharedPreferences.getBoolean(getString(R.string.allow_updates_preference_key), false)){
            startBackgroundUpdate();
        }

        Intent patientInputIntent = new Intent(this, PatientDataInputActivity.class);
        patientInputIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(patientInputIntent);
        finish();
    }

    private boolean copyAssetsToExternalDir() {
        // invoke initial Setup, copying Kernel Assets to External Dirs
        File kernelDir = new File(getExternalFilesDir(null), "kernels");
        AssetManager assetManager = getResources().getAssets();
        ZipEntry zipEntry;
        byte[] buffer = new byte[1024];
        int length;
        try {
            ZipInputStream inputStream = new ZipInputStream(assetManager.open("kernels.zip"));
            while((zipEntry = inputStream.getNextEntry()) != null){
                if(zipEntry.isDirectory()) {
                    new File(kernelDir, zipEntry.getName()).mkdirs();
                    continue;
                }
                FileOutputStream fileOutputStream = null;
                fileOutputStream = new FileOutputStream(kernelDir + File.separator + zipEntry.getName());
                while ((length = inputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }

            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
            }
            return true;

    }

    private void startBackgroundUpdate() {
        // TODO invoke update Service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_name);
            String description = getString(R.string.notification_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(getString(R.string.channel_id), name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
}