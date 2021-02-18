package nodomain.betchermartin.tensorflowlitescanner.updater.WorkManagerUpdateService;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import nodomain.betchermartin.tensorflowlitescanner.R;
import nodomain.betchermartin.tensorflowlitescanner.BuildConfig;

public class AppUpdaterWorker extends Worker {
    public static final String CHECK_ONLY = "only_check_version";

    public AppUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean versionCheckOnly = getInputData().getBoolean(CHECK_ONLY, false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currVersion = BuildConfig.VERSION_NAME;
        String onlineVersion = "unavailable";
        try {
            onlineVersion = getOnlineVersion(new URL(sharedPreferences.getString("app_updater_server_preference_key", "http://api.github.com/repos/lizardwithhat/slic-android/releases?per_page=1")));
        } catch (MalformedURLException e) {
            Log.e("AppUpdaterWorker", e.getMessage());
        }
        if(onlineVersion.equals("unavailable")){
            Log.e("AppUpdateWorker", "Remote Repo not available");
        } else if(onlineVersion.equals(currVersion)) {
            Log.i("AppIpdateWorker", "No updates available.");
        } else {
            String notifMessage = "Neue SLIC App Version online ("+onlineVersion+"). Tippe zum installieren.";
            Context context = getApplicationContext();
            NotificationManagerCompat notifManager = NotificationManagerCompat.from(context);
            int notifId = (int) (Math.random() * 100.0);
            //Intent pendingIntent = new Intent();
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context, context.getString(R.string.channel_id))
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setContentText(notifMessage)
                    .setContentTitle("SLIC - App Update")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    //.setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            notifManager.notify(notifId, notifBuilder.build());
        }
        return Result.success();
    }

    private String getOnlineVersion(URL repo){
        String result = "unavailable";
        try {
            URLConnection request = repo.openConnection();
            request.connect();
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonRoot = jsonParser.parse(new InputStreamReader((InputStream) request.getContent()));
            JsonArray jsonResponse = jsonRoot.getAsJsonArray();
            result = jsonResponse.get(0).getAsJsonObject().get("name").getAsString();
        } catch (IOException e) {
            Log.e("AppUpdaterWorker", e.getMessage());
        }
        return result;
    }
}
