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

package nodomain.betchermartin.tensorflowlitescanner.cameraclassifier;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Size;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import nodomain.betchermartin.tensorflowlitescanner.dataInput.PatientDataInputFragment;
import nodomain.betchermartin.tensorflowlitescanner.R;
import nodomain.betchermartin.tensorflowlitescanner.customview.AutoFitTextureView;
import nodomain.betchermartin.tensorflowlitescanner.customview.TargetView;
import nodomain.betchermartin.tensorflowlitescanner.env.BorderedText;
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
import java.util.LinkedHashMap;
import java.util.List;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final boolean MAINTAIN_ASPECT = true;
  // Initial size is Smartphones native resolution
  private static Size desiredPreviewSize = new Size(
          Resources.getSystem().getDisplayMetrics().widthPixels,
          Resources.getSystem().getDisplayMetrics().heightPixels);
  private static final float TEXT_SIZE_DIP = 10;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private Bitmap rotatedFrameBitmap = null;
  private long lastProcessingTimeMs;
  private Integer sensorOrientation;
  private Classifier classifier;
  private Matrix frameToRotatedTransform;
  private Matrix rotatedToFrameTransform;
  private Matrix rotatedToCropTransform;
  private BorderedText borderedText;
  private SharedPreferences sharedPreferences;
  private Runnable pictureRunnable;
  private FloatingActionButton fabTrigger;
  private File destination;
  private Boolean boolTriggerActivated = false;
  private float scaleFactor = 1.0f;
  private ScaleGestureDetector scaleGestureDetector;
  private MetaDataWriterInterface metaDataWriter;
  private LinkedHashMap<String, List<Parcelable>> currentPatientData;

  @Override
  public void onCreate(Bundle savedInstance){
      super.onCreate(savedInstance);

      sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      currentPatientData = getIntent().getParcelableExtra(PatientDataInputFragment.RESULT_STRING);

      metaDataWriter = CsvFileWriter.getInstance(new File(getExternalFilesDir(null), "out"));

      fabTrigger = findViewById(R.id.fabTrigger);
      fabTrigger.setOnClickListener(v -> {
          boolTriggerActivated = true;
          MediaActionSound soundEffectPlayer = new MediaActionSound();
          soundEffectPlayer.play(MediaActionSound.SHUTTER_CLICK);
          int animationTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
          FrameLayout cameraFlashOverlay = findViewById(R.id.flashLayout);
          cameraFlashOverlay.setAlpha(1f);
          cameraFlashOverlay.setVisibility(View.VISIBLE);
          cameraFlashOverlay.animate()
                  .alpha(0f)
                  .setDuration(animationTime)
                  .setListener(null);
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

      frameValueTextView.setOnClickListener((v -> {
          View dialogView = getLayoutInflater().inflate(R.layout.number_picker_dialog_layout, null);
          NumberPicker numberPicker = dialogView.findViewById(R.id.numberPicker);
          numberPicker.setMinValue(0);
          numberPicker.setMaxValue(supportedSizes.length-1);
          numberPicker.setWrapSelectorWheel(true);

          String[] labels = new String[supportedSizes.length];
          for(int i = 0; i < supportedSizes.length; i++){
              labels[i] = supportedSizes[i].getWidth()+"x"+supportedSizes[i].getHeight();
          }
          numberPicker.setDisplayedValues(labels);

          AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
          AlertDialog dialog;
          dialogBuilder.setView(dialogView);
          dialogBuilder.setTitle(getString(R.string.choose_resolution));
          dialogBuilder.setPositiveButton(getString(R.string.set), (dialog1, which) -> {
              desiredPreviewSize = supportedSizes[numberPicker.getValue()];
              finish();
              startActivity(getIntent());
          });
          dialogBuilder.setNegativeButton(getString(R.string.cancel), (dialog12, which) -> dialog12.dismiss());
          dialog = dialogBuilder.create();
          dialog.show();
      }));

      scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
      scaleGestureDetector.onTouchEvent(ev);
      return true;
  }


    @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return desiredPreviewSize;
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
    rotatedFrameBitmap = Bitmap.createBitmap(findViewById(R.id.targetLayout).getWidth(),
            findViewById(R.id.targetLayout).getHeight(), Config.ARGB_8888);
    croppedBitmap =
        Bitmap.createBitmap(
            classifier.getImageSizeX(), classifier.getImageSizeY(), Config.ARGB_8888);

      frameToRotatedTransform =
              ImageUtils.getTransformationMatrix(
                      previewWidth,
                      previewHeight,
                      rotatedFrameBitmap.getWidth(),
                      rotatedFrameBitmap.getHeight(),
                      sensorOrientation,
                      MAINTAIN_ASPECT);

    rotatedToFrameTransform = new Matrix();
    frameToRotatedTransform.invert(rotatedToFrameTransform);

    runOnUiThread(
            new Runnable() {
                @Override
                public void run() {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
                    showRotationInfo(String.valueOf(sensorOrientation));
                    showInference(0 + "ms");
                }
            });
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
            rotatedFrameBitmap.getWidth() / 2 - croppedBitmap.getWidth() / 2,
            rotatedFrameBitmap.getHeight() / 2 - croppedBitmap.getHeight() / 2,
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
    final Classifier.Device device = getDevice();
    final Classifier.Model model = getModel();
    final int numThreads = getNumThreads();
    runInBackground(() -> recreateClassifier(model, device, numThreads));
  }

  private void recreateClassifier(Classifier.Model model, Classifier.Device device, int numThreads) {
    if (classifier != null) {
      LOGGER.d("Closing classifier.");
      classifier.close();
      classifier = null;
    }
    try {
      LOGGER.d(
          "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
      classifier = Classifier.create(this, model, device, numThreads, currentPatientData);
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
                  currentPatientData.get("picture_id").clear();
                  currentPatientData.get("picture_id").add(new StringParcelable(destinationFile.getName()));
                  metaDataWriter.writeMetaData(currentPatientData);
              }
          };
      }
      return pictureRunnable;
  }

  @Override
  public void onImageAvailable(final ImageReader reader) {
      super.onImageAvailable(reader);
      //Zeichne Zielrechteck
      if(classifier != null) runOnUiThread( () -> drawRectangle(classifier.getImageSizeX(), classifier.getImageSizeY()));
  }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            AutoFitTextureView viewPort = findViewById(R.id.texture);
            TargetView targetRect = findViewById(R.id.targetLayout);

            scaleFactor *= scaleGestureDetector.getScaleFactor();
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 10.0f));

            viewPort.setScaleX(scaleFactor);
            viewPort.setScaleY(scaleFactor);
            targetRect.setScaleX(scaleFactor);
            targetRect.setScaleY(scaleFactor);

            return true;
        }
    }
}
