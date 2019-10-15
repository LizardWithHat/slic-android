package nodomain.betchermartin.tensorflowlitescanner.updater;

import nodomain.betchermartin.tensorflowlitescanner.kernels.Classifier;

public interface KernelUpdaterInterface {
    void searchAllUpdates();
    void searchKernelUpdate(Classifier.Model kernel);
    void updateAllKernels();
    void updateKernel(Classifier.Model kernel);
}
