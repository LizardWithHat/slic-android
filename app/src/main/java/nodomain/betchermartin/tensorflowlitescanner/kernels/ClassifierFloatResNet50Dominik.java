/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package nodomain.betchermartin.tensorflowlitescanner.kernels;

import android.app.Activity;
import android.os.Parcelable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.betchermartin.tensorflowlitescanner.misc.IntegerParcelable;
import nodomain.betchermartin.tensorflowlitescanner.misc.StringParcelable;

/** This TensorFlowLite classifier works with the float ResNet50 model. */
public class ClassifierFloatResNet50Dominik extends Classifier {

  private float[] metaDataArray;

  /** Classifier requires additional normalization of the used input. */
  private static final float IMAGE_MEAN = 127.5f;
  private static final float IMAGE_STD = 127.5f;

  /**
   * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
   * of the super class, because we need a primitive array here.
   */
  private float[][] labelProbArray = null;

  /**
   * Initializes a {@code ClassifierFloatMobileNetDominik}.
   *
   * @param activity
   */
  public ClassifierFloatResNet50Dominik(Activity activity, Device device, int numThreads, Map<String, List<Parcelable>> metaDataInput)
          throws IOException {
    super(activity, device, numThreads, metaDataInput);
    labelProbArray = new float[1][getNumLabels()];
  }

  // Als Eingabe wird ein Bild im Format 224x224 Pixeln erwartet, bei dem die Pixel-Farbwerte zwischen 0 und 1 liegen. Jedes Pixel wird als 3 float64 (Double Precision) abgebildet.
  @Override
  public int getImageSizeX() {
    return 224;
  }

  @Override
  public int getImageSizeY() {
    return 224;
  }

  @Override
  protected String getModelPath() {
    return context.getExternalFilesDir(null).getPath() + File.separator + "kernels/dominikresnet50/skin-cancer-ResNet50.tflite";
  }

  @Override
  protected String getLabelPath() {
    return context.getExternalFilesDir(null).getPath() + File.separator + "kernels/dominikmobilenet/labels.txt";
  }

  @Override
  protected int getNumBytesPerChannel() {
    return 4; // Float.SIZE / Byte.SIZE;
  }

  @Override
  protected void addPixelValue(int pixelValue, float extremeValues[]) {
    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
  }

  @Override
  protected float getProbability(int labelIndex) {
    return labelProbArray[0][labelIndex];
  }

  @Override
  protected void setProbability(int labelIndex, Number value) {
    labelProbArray[0][labelIndex] = value.floatValue();
  }

  @Override
  protected float getNormalizedProbability(int labelIndex) {
    return labelProbArray[0][labelIndex];
  }

  @Override
  protected void runInference() {
    float[][] result = new float[1][1];
    processInput();
    Object[] inputArray = {imgData, metaDataArray};
    Map<Integer, Object> outputMap = new HashMap<>();
    outputMap.put(0, result);
    tflite.runForMultipleInputsOutputs(inputArray, outputMap);
    setProbability(0, result[0][0]);
    setProbability(1, 1 - result[0][0]);
  }

  @Override
  public String getDataDetailPath() {
    return context.getExternalFilesDir(null).getPath() + File.separator + "kernels/dominikmobilenet/skin-cancer-data-detail.json"; }

  @Override
  protected void processInput() {
    /*
     * - Metadaten:
     * - Alter (dividiert durch 100)
     * - Geschlecht (0=female, 1=male)
     * - Körperstelle (jeweils eine Stelle 0 oder 1 ['anterior torso', 'lower extremity', 'posterior torso', 'head/neck', 'upper extremity', 'lateral torso', 'palms/soles'])
     */

    metaDataArray = new float[9];
    for (String key : metaData.keySet()) {
      switch (key) {
        case "age":
          metaDataArray[0] = ((IntegerParcelable) metaData.get(key).get(0)).getValue() / 100.0f;
          break;
        case "sex":
          metaDataArray[1] = ((StringParcelable) metaData.get(key).get(0)).getValue().equals("female") ? 0.0f : 1.0f;
          break;
        case "localization":
          String value = ((StringParcelable) metaData.get(key).get(0)).getValue();

          // initially fill with 0f, more efficient with copy paste instead of loops
          metaDataArray[2] = 0.0f;
          metaDataArray[3] = 0.0f;
          metaDataArray[4] = 0.0f;
          metaDataArray[5] = 0.0f;
          metaDataArray[6] = 0.0f;
          metaDataArray[7] = 0.0f;
          metaDataArray[8] = 0.0f;

          switch (value) {
            case "anterior torso":
              metaDataArray[2] = 1.0f;
              break;
            case "lower extremity":
              metaDataArray[3] = 1.0f;
              break;
            case "posterior torso":
              metaDataArray[4] = 1.0f;
              break;
            case "head":
            case "neck":
              metaDataArray[5] = 1.0f;
              break;
            case "upper extremity":
              metaDataArray[6] = 1.0f;
              break;
            case "lateral torso":
              metaDataArray[7] = 1.0f;
              break;
            case "palms":
            case "soles":
              metaDataArray[8] = 1.0f;
              break;
          }
          break;
      }
    }
  }
}