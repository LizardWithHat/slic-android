/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.classification;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Size;
import android.util.TypedValue;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.examples.classification.customview.TargetView;
import org.tensorflow.lite.examples.classification.env.BorderedText;
import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.misc.SimpleDetail;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final boolean MAINTAIN_ASPECT = true;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(
          Resources.getSystem().getDisplayMetrics().widthPixels,
          Resources.getSystem().getDisplayMetrics().heightPixels);
  private static final float TEXT_SIZE_DIP = 10;
  private static final int PATIENT_DATA_REQUEST = 2;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private Bitmap rotatedFrameBitmap = null;
  private long lastProcessingTimeMs;
  private Integer sensorOrientation;
  private Classifier classifier;
  private Matrix frameToRotatedTransform;
  private Matrix rotatedToFrameTransform;
  private BorderedText borderedText;
  private SharedPreferences sharedPreferences;
  private Runnable pictureRunnable;
  private Runnable dataRunnable;
  private ImageButton butPatientData;
  private FloatingActionButton fabTrigger;
  private File destination;
  private String[] patientDataHeaders;
  private Boolean boolTriggerActivated = false;
  protected File currentCsvFile = null;

  @Override
  public void onCreate(Bundle savedInstance){
      super.onCreate(savedInstance);
      sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

      butPatientData = findViewById(R.id.butPatientData);
      // Patienten Daten neu angeben verhält sich wie die Aktivität neu zu starten.
      butPatientData.setOnClickListener(v -> finish());

      fabTrigger = findViewById(R.id.fabTrigger);
      fabTrigger.setOnClickListener(v -> {
          boolTriggerActivated = true;
      });

      // Ordner anlegen und .nomedia hinterlegen, falls neu angelegt
      destination = new File(Environment.getExternalStorageDirectory(), "SkinCancerScanner");
      if (destination.mkdir()){
          File nomedia = new File(destination, ".nomedia");
          try {
              nomedia.createNewFile();
          } catch (IOException e) {
              LOGGER.e("Error creating .nomedia File: "+e.getMessage());
          }
      }
  }

  @Override
  public synchronized  void onResume() {
      // Lege CSV Header nur an wenn Präferenz dafür aktiviert
      if(sharedPreferences.getBoolean(getString(R.string.collect_data_preference_key), true)) {
          setUpCsvFile();
      }
      super.onResume();
  }

  @Override
  public synchronized void onDestroy() {
      if(sharedPreferences.getBoolean(getString(R.string.send_data_preference_key), true)) runOnUiThread(getSendDataRunnable());
      super.onDestroy();
  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    recreateClassifier(getModel(), getDevice(), getNumThreads());
    if (classifier == null) {
      LOGGER.e("No classifier on preview!");
      return;
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    int maximumDimension = previewWidth >= previewHeight ? previewWidth : previewHeight;
    rotatedFrameBitmap = Bitmap.createBitmap(maximumDimension, maximumDimension, Config.ARGB_8888);
    croppedBitmap =
        Bitmap.createBitmap(
            classifier.getImageSizeX(), classifier.getImageSizeY(), Config.ARGB_8888);

    frameToRotatedTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth,
            previewHeight,
            maximumDimension,
            maximumDimension,
            sensorOrientation,
            MAINTAIN_ASPECT);

    rotatedToFrameTransform = new Matrix();
    frameToRotatedTransform.invert(rotatedToFrameTransform);
  }

  @Override
  protected void processImage() {
    if(!boolTriggerActivated){
        readyForNextImage();
        return;
    }
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final Canvas canvas = new Canvas(rotatedFrameBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToRotatedTransform, null);
    croppedBitmap = Bitmap.createBitmap(rotatedFrameBitmap,
            rotatedFrameBitmap.getHeight() / 2 - croppedBitmap.getHeight(),
            rotatedFrameBitmap.getWidth() / 2 - croppedBitmap.getWidth(),
            croppedBitmap.getWidth(), croppedBitmap.getHeight());

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            if (classifier != null) {
              final long startTime = SystemClock.uptimeMillis();
              final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
              lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
              LOGGER.v("Detect: %s", results);
              cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                if(sharedPreferences.getBoolean(getString(R.string.collect_data_preference_key), true)){
                    runInBackground(getImageSaverRunnable());
                }

              runOnUiThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      showResultsInBottomSheet(results);
                      showFrameInfo(previewWidth + "x" + previewHeight);
                      showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                      showCameraResolution(canvas.getWidth() + "x" + canvas.getHeight());
                      showRotationInfo(String.valueOf(sensorOrientation));
                      showInference(lastProcessingTimeMs + "ms");
                    }
                  });
            }
            boolTriggerActivated = false;
            readyForNextImage();
          }
        });
  }

  @Override
  protected void onInferenceConfigurationChanged() {
    if (croppedBitmap == null) {
      // Defer creation until we're getting camera frames.
      return;
    }
    final Device device = getDevice();
    final Model model = getModel();
    final int numThreads = getNumThreads();
    runInBackground(() -> recreateClassifier(model, device, numThreads));
  }

  private void recreateClassifier(Model model, Device device, int numThreads) {
    if (classifier != null) {
      LOGGER.d("Closing classifier.");
      classifier.close();
      classifier = null;
    }
    if (device == Device.GPU && model == Model.QUANTIZED) {
      LOGGER.d("Not creating classifier: GPU doesn't support quantized models.");
      runOnUiThread(
          () -> {
            Toast.makeText(this, "GPU does not yet supported quantized models.", Toast.LENGTH_LONG)
                .show();
          });
      return;
    }
    try {
      LOGGER.d(
          "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
      classifier = Classifier.create(this, model, device, numThreads);
    } catch (IOException e) {
      LOGGER.e(e, "Failed to create classifier.");
    }
  }

  public void drawRectangle(int inputWidth, int inputHeight){
    TargetView targetLayout = findViewById(R.id.targetLayout);
    TextureView textureView = findViewById(R.id.texture);

    if(!sharedPreferences.getBoolean(getString(R.string.target_square_preference_key), true)){
       targetLayout.setVisibility(View.INVISIBLE);
       return;
    }
    // Draw targeting Rectangle
    // Input wird noch gedreht
    targetLayout.setLayoutParams(new ConstraintLayout.LayoutParams(
            textureView.getMeasuredWidth(),
            textureView.getMeasuredHeight()
    ));
    targetLayout.setInputHeight(inputHeight);
    targetLayout.setInputWidth(inputWidth);
    targetLayout.setVisibility(View.VISIBLE);
    targetLayout.bringToFront();
  }

  private Runnable getImageSaverRunnable(){
      if(pictureRunnable == null) {
          pictureRunnable = new Runnable() {

              @Override
              public void run() {
                  if (croppedBitmap == null) return;
                  File destinationFile = new File(destination, "picture_" + System.currentTimeMillis() + ".jpg");
                  Bitmap copy = Bitmap.createBitmap(croppedBitmap);
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
      ArrayList<SimpleDetail> patientData = getIntent().getExtras().getParcelableArrayList(PatientDataInputActivity.RESULT_STRING);

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
                String line = "";
                for(String s : data){
                    if(line.isEmpty()){
                        line = s;
                    }else {
                        line = String.join(",", line, s);
                    }
                }
                csvWriter.append(line).append("\n");
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException e) {
                LOGGER.e("Error writing CSV: " + e.getMessage());
            }
        }
    }

  @Override
  public void onImageAvailable(final ImageReader reader) {
      super.onImageAvailable(reader);
      //Zeichne Zielrechteck
      if(classifier != null) runOnUiThread( () -> drawRectangle(classifier.getImageSizeX(), classifier.getImageSizeY()));
  }
}
