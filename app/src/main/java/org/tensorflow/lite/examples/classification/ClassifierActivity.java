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

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Size;
import android.util.TypedValue;
import android.view.TextureView;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.tensorflow.lite.examples.classification.customview.TargetView;
import org.tensorflow.lite.examples.classification.env.BorderedText;
import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final boolean MAINTAIN_ASPECT = true;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final float TEXT_SIZE_DIP = 10;
  private static final int PERMISSIONS_REQUEST = 1;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private long lastProcessingTimeMs;
  private Integer sensorOrientation;
  private Classifier classifier;
  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;
  private BorderedText borderedText;
  private ScheduledThreadPoolExecutor poolScheduler;
  private SharedPreferences sharedPreferences;
  private SharedPreferences.OnSharedPreferenceChangeListener listener;
  private Runnable pictureRunnable;
  private Runnable dataRunnable;

  @Override
  public void onCreate(Bundle savedInstance){
      super.onCreate(savedInstance);
      sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      askForPermissions();
      poolScheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
      setUpPictureSaveInterval();
      setUpSendDataInterval();
      listener = (sharedPreferences, key) -> {
          if (getString(R.string.interval_collect_picture_preference_key).equals(key)) {
              setUpPictureSaveInterval();
          } else if(getString(R.string.interval_send_data_preference_key).equals(key) |
          getString(R.string.switch_auto_send_preference_key).equals(key)){
              setUpSendDataInterval();
          }
      };
      sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
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
    croppedBitmap =
        Bitmap.createBitmap(
            classifier.getImageSizeX(), classifier.getImageSizeY(), Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth,
            previewHeight,
            classifier.getImageSizeX(),
            classifier.getImageSizeY(),
            sensorOrientation,
            MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);
  }

  @Override
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    if(classifier != null) runOnUiThread( () -> drawRectangle(classifier.getImageSizeX(), classifier.getImageSizeY()) );

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
    targetLayout.setLayoutParams(new RelativeLayout.LayoutParams(
            textureView.getMeasuredWidth(),
            textureView.getMeasuredHeight()
    ));
    targetLayout.setInputHeight(inputHeight);
    targetLayout.setInputWidth(inputWidth);
    targetLayout.setVisibility(View.VISIBLE);
    targetLayout.bringToFront();
  }

  private Runnable createImageSaverRunnable(){
      if(pictureRunnable == null) {
          pictureRunnable = new Runnable() {
              File destination = new File(Environment.getExternalStorageDirectory(), "SkinCancerScanner");

              @Override
              public void run() {
                  if (croppedBitmap == null | !isExternalStorageWritable()) return;
                  destination.mkdir();
                  File destinationFile = new File(destination, "picture_" + System.currentTimeMillis() + ".jpg");
                  Bitmap copy = Bitmap.createBitmap(croppedBitmap);
                  try {
                      FileOutputStream out = new FileOutputStream(destinationFile);
                      copy.compress(Bitmap.CompressFormat.JPEG, 100, out);
                  } catch (FileNotFoundException e) {
                      LOGGER.e("Error saving Image: " + e.getMessage());
                  }
              }
          };
      }
      return pictureRunnable;
  }

  private Runnable createSendDataRunnable(){
      if(dataRunnable == null) {
          dataRunnable = new Runnable() {
              @Override
              public void run() {
                  LOGGER.d("Ich sende Daten!");
              }
          };
      }
      return dataRunnable;
  }

  public boolean isExternalStorageWritable() {
      String state = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(state)) {
          return true;
      }
      return false;
  }

  private void askForPermissions(){
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
              PackageManager.PERMISSION_GRANTED ||
              ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                      PackageManager.PERMISSION_GRANTED )
          ActivityCompat.requestPermissions(this,
                  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                          Manifest.permission.CAMERA}, PERMISSIONS_REQUEST);
  }
  private void setUpPictureSaveInterval(){
      poolScheduler.remove(pictureRunnable);
      int imageSaveInterval = Integer.parseInt(
              sharedPreferences.getString(getString(R.string.interval_collect_picture_preference_key), "0"));
      if(imageSaveInterval != 0) {
          poolScheduler.scheduleWithFixedDelay(createImageSaverRunnable(),
                  5 ,imageSaveInterval, TimeUnit.SECONDS);
      }
  }

  private void setUpSendDataInterval(){
      poolScheduler.remove(dataRunnable);
      int sendDataInterval = Integer.parseInt(
              sharedPreferences.getString(getString(R.string.interval_send_data_preference_key), "0"));
      if(sendDataInterval != 0
              & sharedPreferences.getBoolean(getString(R.string.switch_auto_send_preference_key), false)) {
          poolScheduler.scheduleWithFixedDelay(createSendDataRunnable(),
                  0, sendDataInterval, TimeUnit.SECONDS);
      }
  }
}
