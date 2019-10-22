package nodomain.betchermartin.tensorflowlitescanner.updater;

import java.util.Map;

import nodomain.betchermartin.tensorflowlitescanner.kernels.Classifier;

public interface KernelUpdaterInterface {
    /**
     * Checks all kernels for updates
     * @return A map of String, String, where Keys are Kernel names and values are Version Numbers
     */
    Map<String, String> searchAllUpdates();

    /**
     * Checks a specific kernel for updates
     * @param kernel The Kernel that should be checked
     * @return Kernel version
     */
    String searchKernelUpdate(Classifier.Model kernel);

    /**
     * Updates all kernels
     * @return Boolean indicates success if true, false if not.
     */
    boolean updateAllKernels();

    /**
     * Updates a specific kernel
     * @param kernel The kernel that should be updated
     * @return Boolean indicates success if true, false if not.
     */
    boolean updateKernel(Classifier.Model kernel);
}
