package nodomain.betchermartin.tensorflowlitescanner.env;

import android.content.Context;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import nodomain.betchermartin.tensorflowlitescanner.R;
import nodomain.betchermartin.tensorflowlitescanner.tflite.Classifier;

public class MockKernelUpdater implements KernelUpdaterInterface {

    private final Context context;

    public MockKernelUpdater(Context context){
        this.context = context;
    }

    @Override
    public void searchAllUpdates() {
        Toast.makeText(context, "Not Implemented yet.", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void searchKernelUpdate(Classifier.Model kernel) {
        Toast.makeText(context, "Not Implemented yet.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateAllKernels() {
        NotificationManagerCompat notifManager = NotificationManagerCompat.from(context);
        int notifId = (int) (Math.random() * 100.0);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context, context.getString(R.string.channel_id))
                .setContentText("Dies ist ein Mock-Update!")
                .setContentTitle("Seelab-Scanner - Mock Update")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);
        notifManager.notify(notifId, notifBuilder.build());
    }

    @Override
    public void updateKernel(Classifier.Model kernel) {
        Toast.makeText(context, "Not Implemented yet.", Toast.LENGTH_SHORT).show();
    }
}
