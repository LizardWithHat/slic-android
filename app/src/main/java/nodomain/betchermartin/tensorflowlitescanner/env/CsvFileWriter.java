package nodomain.betchermartin.tensorflowlitescanner.env;

import android.os.Parcelable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class CsvFileWriter implements MetaDataWriterInterface {
    private static CsvFileWriter instance;
    private File csvFile;
    private static final Logger LOGGER = new Logger();

    private CsvFileWriter(File destination){
        this.csvFile = new File(destination, "metadata_"+System.currentTimeMillis()+".csv");
    }

    public static CsvFileWriter getInstance(File destination){
        if(instance == null) instance = new CsvFileWriter(destination);
        return instance;
    }

    @Override
    public boolean writeMetaData(LinkedHashMap<String, List<Parcelable>> listOfObjects) {
        if(!csvFile.exists()) writeHeaderLine(listOfObjects.keySet());
        if(csvFile != null){
            FileWriter csvWriter;
            try {
                csvWriter = new FileWriter(csvFile, true);
                StringBuilder sb = new StringBuilder();
                // Write Line Cells
                for(String key : listOfObjects.keySet()){
                    // Write multiple Items in Cell
                    for(Parcelable item : listOfObjects.get(key)){
                        if(sb.length() == 0 || sb.charAt(sb.length() - 1) == ','){
                            sb.append(item.toString());
                        }else {
                            sb.append(";").append(item.toString());
                        }
                    }
                    sb.append(",");
                }
                // Finish Line
                csvWriter.append(sb).append("\n");
                csvWriter.flush();
                csvWriter.close();
                return true;
            } catch (IOException e) {
                LOGGER.e("Error writing CSV: " + e.getMessage());
                return false;
            }
        } else { return false; }
    }

    private void writeHeaderLine(Set<String> headers) {
        FileWriter csvWriter;
        try{
            csvWriter = new FileWriter(csvFile, true);
            StringBuilder sb = new StringBuilder();
            for(String key : headers){
                sb.append(key).append(",");
            }
            csvWriter.append(sb).append("\n");
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            LOGGER.e("Error writing CSV Header: " + e.getMessage());
        }
    }
}
