package nodomain.betchermartin.tensorflowlitescanner.updater.WorkManagerUpdateService;

import android.content.Context;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import nodomain.betchermartin.tensorflowlitescanner.updater.AppUpdaterInterface;

public class WorkManagerAppUpdater implements AppUpdaterInterface {

    private static final String UPDATE_WORKER_KEY = "app_updater_worker_key";

    private Context mContext;
    private WorkManager workManager;

    public WorkManagerAppUpdater(Context context){
        this.mContext = context;
        workManager = WorkManager.getInstance(context);
    }

    @Override
    public String checkAppVersion() {
        Constraints updateWorkerConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest updateWorker = new PeriodicWorkRequest.Builder(AppUpdaterWorker.class, 7, TimeUnit.DAYS)
                .setConstraints(updateWorkerConstraints)
                .setInputData(new Data.Builder().putBoolean(AppUpdaterWorker.CHECK_ONLY, true).build())
                .build();

        workManager.enqueue(updateWorker);
        return "not implemented yet";
    }

    @Override
    public boolean updateApp() {
        Constraints updateWorkerConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest updateWorker = new PeriodicWorkRequest.Builder(AppUpdaterWorker.class, 7, TimeUnit.DAYS)
                .setConstraints(updateWorkerConstraints)
                .build();

        workManager.enqueueUniquePeriodicWork(UPDATE_WORKER_KEY, ExistingPeriodicWorkPolicy.REPLACE, updateWorker);
        return true;
    }
}
