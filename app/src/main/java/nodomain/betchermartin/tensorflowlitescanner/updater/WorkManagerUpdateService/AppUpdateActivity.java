package nodomain.betchermartin.tensorflowlitescanner.updater.WorkManagerUpdateService;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class AppUpdateActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: BAD, only use for debug purposes
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            URL repoURL = new URL(sharedPreferences.getString("app_updater_server_preference_key", "https://api.github.com/repos/lizardwithhat/slic-android/releases?per_page=1"));
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(getDownloadURL(repoURL)));
            downloadRequest.setTitle("SLIC Update Download");
            downloadRequest.setDescription("SLIC lädt ein Update herunter.");
            downloadRequest.allowScanningByMediaScanner();
            downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            downloadRequest.setDestinationInExternalFilesDir(this, null, "update.apk");

            downloadManager.enqueue(downloadRequest);
        } catch (IOException e) {
            Log.e("AppUpdateActivity", e.getMessage());
        } finally {
            finish();
        }
    }

    private String getDownloadURL(URL repo) throws IOException{
        String repoDownloadURL;
        URLConnection request = repo.openConnection();
        request.connect();
        JsonElement jsonRoot = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent()));
        JsonArray jsonResponse = jsonRoot.getAsJsonArray();
        repoDownloadURL = jsonResponse.get(0).getAsJsonObject()
                .get("assets").getAsJsonArray().get(0).getAsJsonObject()
                .get("browser_download_url").getAsString();
        return repoDownloadURL;
    }
}