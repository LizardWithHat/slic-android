package nodomain.betchermartin.tensorflowlitescanner.env;

import android.content.Context;

import nodomain.betchermartin.tensorflowlitescanner.tflite.Classifier;

public interface KernelUpdaterInterface {
    void searchAllUpdates();
    void searchKernelUpdate(Classifier.Model kernel);
    void updateAllKernels();
    void updateKernel(Classifier.Model kernel);
}
