package nodomain.betchermartin.tensorflowlitescanner.updater.WorkManagerUpdateService;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import nodomain.betchermartin.tensorflowlitescanner.R;

public class KernelUpdaterWorker extends Worker {

    public static final String CHECK_ONLY = "check_only";

    public KernelUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean versionCheckOnly = getInputData().getBoolean(CHECK_ONLY, false);
        String notifMessage = "Dies ist ein Mock-Kernel-Update!";
        if(versionCheckOnly) notifMessage = "Dies ist ein Mock-Kernel-Update-Check!";
        Context context = getApplicationContext();
        NotificationManagerCompat notifManager = NotificationManagerCompat.from(context);
        int notifId = (int) (Math.random() * 100.0);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context, context.getString(R.string.channel_id))
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentText(notifMessage)
                .setContentTitle("Seelab-Scanner - Mock Update")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        notifManager.notify(notifId, notifBuilder.build());
        return Result.success();
    }
}
