package nodomain.betchermartin.tensorflowlitescanner;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nodomain.betchermartin.tensorflowlitescanner.dataInput.PatientDataInputActivity;
import nodomain.betchermartin.tensorflowlitescanner.env.Logger;
import nodomain.betchermartin.tensorflowlitescanner.updater.AppUpdaterInterface;
import nodomain.betchermartin.tensorflowlitescanner.updater.KernelUpdaterInterface;
import nodomain.betchermartin.tensorflowlitescanner.updater.WorkManagerUpdateService.WorkManagerAppUpdater;
import nodomain.betchermartin.tensorflowlitescanner.updater.WorkManagerUpdateService.WorkManagerKernelUpdater;

public class LandingPageActivity extends AppCompatActivity {

    private static final int BUFFER_SIZE = 1024 * 4;

    private static final Logger LOGGER = new Logger();

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    // Kamera Genehmigung impliziert auch Licht/Blitz Nutzung
    private static final int PERMISSIONS_REQUEST = 1;
    private KernelUpdaterInterface kernelUpdater;
    private AppUpdaterInterface appUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);
        appUpdater = new WorkManagerAppUpdater(this.getApplicationContext());
        kernelUpdater = new WorkManagerKernelUpdater(this.getApplicationContext());
        createNotificationChannel();
    }

    @Override
    protected void onResume(){
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sharedPreferences.getBoolean(getString(R.string.initialSetup), false)){
            AsyncTask<Void, Void, Boolean> assetCopier = new AssetCopier();
            assetCopier.execute();
        } else {
            preparePermissions();
        }
    }

    private final class AssetCopier extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // Copy assets and subfolders to external files dir
            // from: https://stackoverflow.com/a/25988391
            try {
                copyDirorfileFromAssetManager("kernels", "kernels");
            } catch (IOException e) {
                LOGGER.e("Error while copying Assets: %s", e.getMessage());
            }
            return true;
        }

        private void copyDirorfileFromAssetManager(String arg_assetDir, String arg_destinationDir) throws IOException{
            File sd_path = getExternalFilesDir(null);
            String dest_dir_path = sd_path + File.separator + arg_destinationDir;
            File dest_dir = new File(dest_dir_path);

            createDir(dest_dir);

            AssetManager asset_manager = getApplicationContext().getAssets();
            String[] files = asset_manager.list(arg_assetDir);

            for(String asset_file_name : files){
                String abs_asset_file_path = arg_assetDir + File.separator + asset_file_name;
                String sub_files[] = asset_manager.list(abs_asset_file_path);

                if (sub_files.length == 0){
                    // It is a file
                    String dest_file_path = dest_dir_path + File.separator + asset_file_name;
                    copyAssetFile(abs_asset_file_path, dest_file_path);
                } else {
                    // It is a sub directory
                    copyDirorfileFromAssetManager(abs_asset_file_path, arg_destinationDir + File.separator + asset_file_name);
                }
            }

        }


        private void copyAssetFile(String assetFilePath, String destinationFilePath) throws IOException{
            InputStream in = getApplicationContext().getAssets().open(assetFilePath);
            OutputStream out = new FileOutputStream(destinationFilePath);

            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) > 0)
                out.write(buf, 0, len);
            in.close();
            out.close();
        }

        private void createDir(File dir) throws IOException{
            if (dir.exists()){
                if (!dir.isDirectory()){
                    throw new IOException("Can't create directory, a file is in the way");
                }
            } else{
                dir.mkdirs();
                if (!dir.isDirectory()){
                    throw new IOException("Unable to create directory");
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result){
                PreferenceManager.getDefaultSharedPreferences(LandingPageActivity.this).edit()
                        .putBoolean(getString(R.string.initialSetup), true).commit();
                preparePermissions();
            } else {
                Toast.makeText(LandingPageActivity.this, "Copying Assets failed, abort.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startBackgroundUpdate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean(getString(R.string.app_updater_switch_preference_key), true))
            appUpdater.checkAppVersion();
        if(sharedPreferences.getBoolean(getString(R.string.kernel_updater_switch_install_preference_key), true)){
            kernelUpdater.updateAllKernels();
        } else if (sharedPreferences.getBoolean(getString(R.string.kernel_updater_switch_install_preference_key), true)) {
            kernelUpdater.searchAllUpdates();
        }

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
                    || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                requestPermission();
            } else {
                exitActivity();
            }
        }
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
}

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_CAMERA)) {
            Toast.makeText(
                    this,
                    "Camera permission is required for this App",
                    Toast.LENGTH_LONG)
                    .show();
        }
        ActivityCompat.requestPermissions(this, new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }

    private void preparePermissions(){
        if(!hasPermission()) {
            requestPermission();
        } else {
            exitActivity();
        }
    }

    private void exitActivity(){
        startBackgroundUpdate();
        Intent patientInputIntent = new Intent(this, PatientDataInputActivity.class);
        patientInputIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(patientInputIntent);
        finish();
    }
}