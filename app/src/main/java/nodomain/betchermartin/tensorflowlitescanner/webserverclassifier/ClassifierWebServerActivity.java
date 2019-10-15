package nodomain.betchermartin.tensorflowlitescanner.webserverclassifier;

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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import nodomain.betchermartin.tensorflowlitescanner.dataInput.PatientDataInputFragment;
import nodomain.betchermartin.tensorflowlitescanner.R;
import nodomain.betchermartin.tensorflowlitescanner.metadatawriter.CsvFileWriter;
import nodomain.betchermartin.tensorflowlitescanner.env.ImageUtils;
import nodomain.betchermartin.tensorflowlitescanner.env.Logger;
import nodomain.betchermartin.tensorflowlitescanner.metadatawriter.MetaDataWriterInterface;
import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;
import nodomain.betchermartin.tensorflowlitescanner.kernels.Classifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;

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
    private final int defaultPort = 8080;
    private String ipAdress;
    private Runnable pictureRunnable;
    private File destination;
    protected LinkedHashMap<String, List<Parcelable>> currentPatientData;
    private SharedPreferences sharedPreferences;
    private HandlerThread handlerThread;
    private Handler handler;
    private MetaDataWriterInterface metaDataWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier_web_server);
        curPic = findViewById(R.id.iwWebServerCurrentPicture);
        resultsTextView = findViewById(R.id.tvWebServerActivityResults);
        resultsTextViewTitle = findViewById(R.id.tfWebServerActivityResultTitle);
        ipAdressTextView = findViewById(R.id.tvIpAddressTextView);
        currentPatientData = getIntent().getParcelableExtra(PatientDataInputFragment.RESULT_STRING);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        metaDataWriter = CsvFileWriter.getInstance(new File(getExternalFilesDir(null), "out"));

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
        destination = new File(getExternalFilesDir(null), "out");
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
            classifier = Classifier.create(this, model, device, numThreads, currentPatientData);
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

    @Override
    public List<Classifier.Recognition> onServeReceived(String input) throws NullPointerException {
        Bitmap image = BitmapFactory.decodeFile(input);
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
                    currentPatientData.get("picture_id").clear();
                    currentPatientData.get("picture_id").add(new StringParcelable(destinationFile.getName()));
                    metaDataWriter.writeMetaData(currentPatientData);
                }
            };
        }
        return pictureRunnable;
    }
}
