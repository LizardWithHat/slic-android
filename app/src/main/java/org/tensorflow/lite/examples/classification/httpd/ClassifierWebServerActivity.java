package org.tensorflow.lite.examples.classification.httpd;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.tensorflow.lite.examples.classification.PatientDataInputFragment;
import org.tensorflow.lite.examples.classification.R;
import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.misc.SimpleDetail;
import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ClassifierWebServerActivity extends AppCompatActivity implements ClassifierWebServerCallback{

    private static Logger LOGGER = new Logger();
    private Classifier.Model model;
    private Classifier.Device device;
    private int numThreads;
    public static final String CHOSENMODEL = "CHOSENMODEL";
    private ClassifierWebServer webServer;
    private Classifier interpreter;
    private Bitmap rgbFrame;
    private Bitmap rgbFrameScaled;
    private Bitmap rgbFrameCropped;
    private Matrix transformationMatrix;
    private long lastProcessingTimeMs;
    private List<Classifier.Recognition> results;
    private ImageView curPic;
    private TextView resultsTextView;
    private TextView resultsTextViewTitle;
    private TextView ipAdressTextView;
    private int defaultPort = 8080;
    private String ipAdress;
    private Runnable pictureRunnable;
    private Runnable dataRunnable;
    private File destination;
    private String[] patientDataHeaders;
    protected File currentCsvFile = null;
    protected String[] currentPatientData;
    private SharedPreferences sharedPreferences;
    private HandlerThread handlerThread;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier_web_server);
        curPic = findViewById(R.id.iwWebServerCurrentPicture);
        resultsTextView = findViewById(R.id.tvWebServerActivityResults);
        resultsTextViewTitle = findViewById(R.id.tfWebServerActivityResultTitle);
        ipAdressTextView = findViewById(R.id.tvIpAddressTextView);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ipAdress = getNetworkIpAdress();
        ipAdressTextView.setText(ipAdress+":"+defaultPort);
        Bitmap qrCode = generateQrCode(ipAdress, defaultPort);
        if(qrCode != null) curPic.setImageBitmap(qrCode);

        model = Classifier.Model.valueOf(getIntent().getStringExtra(CHOSENMODEL).toUpperCase());
        device = Classifier.Device.CPU;
        numThreads = 2;
        interpreter = createClassifier(model, device, numThreads);
        webServer = new ClassifierWebServer( defaultPort,
                new Size(interpreter.getImageSizeX(), interpreter.getImageSizeY()), this);
        try {
            webServer.start();
        } catch (IOException e) {
            LOGGER.e(e, "Failed starting web server");
        }

        // Ordner anlegen und .nomedia hinterlegen, falls neu angelegt
        destination = new File(Environment.getExternalStorageDirectory(), "SkinCancerScanner");
        if (destination.mkdir()) {
            File nomedia = new File(destination, ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
                LOGGER.e("Error creating .nomedia File: " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized void onDestroy() {
        if(sharedPreferences.getBoolean(getString(R.string.send_data_preference_key), true)) runOnUiThread(getSendDataRunnable());
        webServer.stop();
        super.onDestroy();
    }


    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // Lege CSV Header nur an wenn Präferenz dafür aktiviert
        if(sharedPreferences.getBoolean(getString(R.string.collect_data_preference_key), true)) {
            setUpCsvFile();
        }
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    private Bitmap generateQrCode(String ipAdress, int defaultPort) {
        // Methode von hier https://stackoverflow.com/a/25283174
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix qrCodeMatrix = null;
        Bitmap qrCode = null;
        try {
            qrCodeMatrix = qrCodeWriter.encode(ipAdress+":"+defaultPort, BarcodeFormat.QR_CODE, 512, 512);
            qrCode = Bitmap.createBitmap(qrCodeMatrix.getWidth(), qrCodeMatrix.getHeight(), Bitmap.Config.ARGB_8888);
            for (int x = 0; x < qrCodeMatrix.getWidth(); x++) {
                for (int y = 0; y < qrCodeMatrix.getHeight(); y++) {
                    qrCode.setPixel(x, y, qrCodeMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            LOGGER.e(e,"Error creating QR Code");
        }
        return  qrCode;
    }

    private String getNetworkIpAdress() {
        //TODO: Ethernet IP Adresse berücksichtigen
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAdressNumbers = wifiInfo.getIpAddress();

        // Methode aus https://stackoverflow.com/a/18638588
        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAdressNumbers = Integer.reverseBytes(ipAdressNumbers);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAdressNumbers).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    private Classifier createClassifier(Classifier.Model model, Classifier.Device device, int numThreads) {
        Classifier classifier = null;
        try {
            classifier = Classifier.create(this, model, device, numThreads);
        } catch (IOException e) {
            LOGGER.e(e, "Failed to create classifier.");
        }
        finally {
            return classifier;
        }
    }

    private Matrix createTransormationMatrix(Size size, int rotation){
        rgbFrame = Bitmap.createBitmap(size.getWidth(), size.getWidth(), Bitmap.Config.ARGB_8888);
        //TODO: Skalierung von größeren Bildern implementieren
        rgbFrameScaled = null;

        rgbFrameCropped = Bitmap.createBitmap(interpreter.getImageSizeX(), interpreter.getImageSizeY(), Bitmap.Config.ARGB_8888);

        return ImageUtils.getTransformationMatrix(
                size.getWidth(),
                size.getHeight(),
                rgbFrameCropped.getWidth(),
                rgbFrameCropped.getHeight(),
                rotation,
                true);
    }

    private List<Classifier.Recognition> processImage(Bitmap image){
        Canvas canvas = new Canvas(rgbFrame);
        canvas.drawBitmap(image, 0, 0, null);
        rgbFrameCropped = Bitmap.createBitmap(rgbFrame,
                rgbFrame.getWidth() / 2 - rgbFrameCropped.getWidth() / 2,
                rgbFrame.getHeight() / 2 - rgbFrameCropped.getHeight() / 2,
                rgbFrameCropped.getWidth(), rgbFrameCropped.getHeight());

        final long startTime = SystemClock.uptimeMillis();
        results = interpreter.recognizeImage(rgbFrameCropped);
        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
        StringBuilder s = new StringBuilder();
        for(Classifier.Recognition recog : results){
            s.append(recog.getTitle());
            s.append(": ");
            s.append(String.format("%.2f", (100 * recog.getConfidence())));
            s.append("%\n");
        }
        s.trimToSize();
        if(sharedPreferences.getBoolean(getString(R.string.collect_data_preference_key), true)){
            handler.post(getImageSaverRunnable());
        }
        runOnUiThread(()->{
            curPic.setImageBitmap(rgbFrameCropped);
            curPic.setVisibility(View.VISIBLE);
            resultsTextView.setText(s.toString());
            resultsTextView.setVisibility(View.VISIBLE);
            resultsTextViewTitle.setVisibility(View.VISIBLE);
        });

        return interpreter.recognizeImage(rgbFrameCropped);
    }

    private Bitmap decodeBASE64Image(String base64String){
        Bitmap result = null;
        byte[] resultBytes = Base64.decode(base64String, Base64.DEFAULT);
        result = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
        return result;
    }

    @Override
    public List<Classifier.Recognition> onServeReceived(String base64String) {
        Bitmap image = decodeBASE64Image(base64String);
        transformationMatrix = createTransormationMatrix(
                new Size(image.getWidth(), image.getHeight()), 0);
        return results = processImage(image);
    }

    private Runnable getImageSaverRunnable(){
        if(pictureRunnable == null) {
            pictureRunnable = new Runnable() {

                @Override
                public void run() {
                    if (rgbFrameCropped == null) return;
                    File destinationFile = new File(destination, "picture_" + System.currentTimeMillis() + ".jpg");
                    Bitmap copy = Bitmap.createBitmap(rgbFrameCropped);
                    try {
                        FileOutputStream out = new FileOutputStream(destinationFile);
                        copy.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    } catch (FileNotFoundException e) {
                        LOGGER.e("Error saving Image: " + e.getMessage());
                    }
                    // If Patient data entered, write metadata to csv file
                    if(currentPatientData != null){
                        currentPatientData[1] = destinationFile.getName();
                        writeCsvLine(currentPatientData);
                    }
                }
            };
        }
        return pictureRunnable;
    }

    private Runnable getSendDataRunnable(){
        if(dataRunnable == null) {
            dataRunnable = new Runnable() {
                @Override
                public void run() {
                    // Zippe alle Dateien im Datenordner in ein Archiv mit Zeitstempel in "out" und "sende" sie
                    File archiveFolder = new File(destination, "out");
                    File archiveFile = new File(archiveFolder, "archive_"+System.currentTimeMillis()+".zip");
                    File[] toBeArchived = destination.listFiles();
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
                    }
                    LOGGER.d("Ich zippe/sende Daten!");
                }
            };
        }
        return dataRunnable;
    }


    private void setUpCsvFile(){
        ArrayList<SimpleDetail> patientData = getIntent().getExtras().getParcelableArrayList(PatientDataInputFragment.RESULT_STRING);

        // Built Header and Patient Data text arrays from ArrayList
        ArrayList<String> patientDataList = new ArrayList<>();
        ArrayList<String> patientDataHeaderList = new ArrayList<>();
        for(SimpleDetail s : patientData){
            patientDataList.add(s.getValue());
            patientDataHeaderList.add(s.getKey());

        }
        currentPatientData = patientDataList.toArray(new String[0]);
        patientDataHeaders = patientDataHeaderList.toArray(new String[0]);
        // Set new CSV File
        File destination = new File(Environment.getExternalStorageDirectory(), "SkinCancerScanner");
        currentCsvFile = new File(destination, "patient_" + currentPatientData[0] + ".csv");

        if(currentCsvFile.exists()) return;

        // Write Header to new CSV
        writeCsvLine(patientDataHeaders);
    }

    protected void writeCsvLine(String[] data){
        if(currentCsvFile != null){
            FileWriter csvWriter;
            try {
                csvWriter = new FileWriter(currentCsvFile, true);
                StringBuilder sb = new StringBuilder();
                for(String s : data){
                    if(sb.length() == 0){
                        sb.append(s);
                    }else {
                        sb.append(",");
                        sb.append(s);
                    }
                }
                csvWriter.append(sb).append("\n");
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException e) {
                LOGGER.e("Error writing CSV: " + e.getMessage());
            }
        }
    }
}
