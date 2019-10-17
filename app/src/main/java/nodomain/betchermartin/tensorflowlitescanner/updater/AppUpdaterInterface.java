package nodomain.betchermartin.tensorflowlitescanner.updater;

public interface AppUpdaterInterface {
    /**
     * checks for version number from the configured Update-Servcer
     * @return the app version number as returned from the server
     */
    String checkAppVersion();

    /**
     * Updates the app from the configured Update-Server
     * @return success downloading new APK
     */
    boolean updateApp();
}
