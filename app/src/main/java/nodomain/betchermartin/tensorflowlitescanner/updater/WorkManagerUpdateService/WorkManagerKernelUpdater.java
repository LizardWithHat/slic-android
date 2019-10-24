package nodomain.betchermartin.tensorflowlitescanner.updater.WorkManagerUpdateService;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import nodomain.betchermartin.tensorflowlitescanner.kernels.Classifier;
import nodomain.betchermartin.tensorflowlitescanner.updater.KernelUpdaterInterface;

public class WorkManagerKernelUpdater implements KernelUpdaterInterface {

    private static final String UPDATE_WORKER_KEY = "kernel_updater_worker_key";
    private static final String KERNEL_NAME_KEY = "kernel_name_key";
    private Context mContext;
    private WorkManager workManager;

    public WorkManagerKernelUpdater(Context context){
        mContext = context;
        workManager = WorkManager.getInstance(context);
    }

    @Override
    public Map<String, String> searchAllUpdates() {
        Map<String, String> availableKernelUpdates = new HashMap<String, String>();
        availableKernelUpdates.put("not implemented yet", "not implemented yet");
        Constraints updateWorkerConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest updateWorker = new PeriodicWorkRequest.Builder(KernelUpdaterWorker.class, 7, TimeUnit.DAYS)
                .setConstraints(updateWorkerConstraints)
                .setInputData(new Data.Builder().putBoolean(KernelUpdaterWorker.CHECK_ONLY, true).build())
                .build();

        workManager.enqueueUniquePeriodicWork(UPDATE_WORKER_KEY, ExistingPeriodicWorkPolicy.REPLACE, updateWorker);

        return availableKernelUpdates;
    }

    @Override
    public String searchKernelUpdate(Classifier.Model kernel) {
        Constraints updateWorkerConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest updateWorker = new PeriodicWorkRequest.Builder(KernelUpdaterWorker.class, 7, TimeUnit.DAYS)
                .setConstraints(updateWorkerConstraints)
                .setInputData(new Data.Builder()
                        .putBoolean(KernelUpdaterWorker.CHECK_ONLY, true)
                        .putString(KERNEL_NAME_KEY, kernel.toString())
                        .build())
                .build();

        workManager.enqueueUniquePeriodicWork(UPDATE_WORKER_KEY, ExistingPeriodicWorkPolicy.REPLACE, updateWorker);

        return null;
    }

    @Override
    public boolean updateAllKernels() {
        Constraints updateWorkerConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest updateWorker = new PeriodicWorkRequest.Builder(KernelUpdaterWorker.class, 7, TimeUnit.DAYS)
                .setConstraints(updateWorkerConstraints)
                .build();

        workManager.enqueueUniquePeriodicWork(UPDATE_WORKER_KEY, ExistingPeriodicWorkPolicy.REPLACE, updateWorker);
        return true;
    }

    @Override
    public boolean updateKernel(Classifier.Model kernel) {
        Constraints updateWorkerConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest updateWorker = new PeriodicWorkRequest.Builder(KernelUpdaterWorker.class, 7, TimeUnit.DAYS)
                .setConstraints(updateWorkerConstraints)
                .setInputData(new Data.Builder()
                        .putString(KERNEL_NAME_KEY, kernel.toString())
                        .build())
                .build();
        workManager.enqueueUniquePeriodicWork(UPDATE_WORKER_KEY, ExistingPeriodicWorkPolicy.REPLACE, updateWorker);
        return true;
    }
}
