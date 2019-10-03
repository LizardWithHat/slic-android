package nodomain.betchermartin.tensorflowlitescanner.env;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LocalDataSender implements DataSenderInterface {
    private final Logger LOGGER = new Logger();
    private static LocalDataSender instance = null;

    private LocalDataSender(){}

    public static LocalDataSender getInstance(){
        if(instance == null) instance = new LocalDataSender();
        return instance;
    }

    @Override
    public boolean compressFiles(File sourceFolder) {
        File archiveFolder = new File(sourceFolder, "out");
        File archiveFile = new File(archiveFolder, "archive_"+System.currentTimeMillis()+".zip");
        File[] toBeArchived = sourceFolder.listFiles();
        archiveFolder.mkdir();
        BufferedInputStream in;
        FileOutputStream fileOut;
        ZipOutputStream zipOut;
        try {
            byte[] rawData = new byte[1024];
            fileOut = new FileOutputStream(archiveFile);
            zipOut = new ZipOutputStream(new BufferedOutputStream(fileOut));
            for(File f : toBeArchived){
                // Überspringe Unterordner (wie etwa "out" Ordner) und .nomedia-Datei
                if(f.isDirectory() || f.getName().equals(".nomedia")) continue;
                LOGGER.d("Archiving File "+f.getName());
                in = new BufferedInputStream(new FileInputStream(f), rawData.length);
                ZipEntry entry = new ZipEntry(f.getName());
                zipOut.putNextEntry(entry);
                int count;
                while((count = in.read(rawData, 0, rawData.length)) != -1) {
                    zipOut.write(rawData, 0, count);
                }
                f.delete();
            }
            zipOut.close();
        } catch (Exception e){
            LOGGER.e(e.getMessage());
            return false;
        }
        LOGGER.d("Finished zipping Data in "+sourceFolder.getAbsolutePath());
        return true;
    }

    @Override
    public boolean sendData(String destination) {
        LOGGER.d("Sending not implemented, local saving only");
        return false;
    }
}
