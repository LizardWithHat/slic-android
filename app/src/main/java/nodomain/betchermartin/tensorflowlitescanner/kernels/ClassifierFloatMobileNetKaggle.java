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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** This TensorFlowLite classifier works with the float MobileNet model. */
public class ClassifierFloatMobileNetKaggle extends Classifier {

  /** MobileNet requires additional normalization of the used input. */
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
  public ClassifierFloatMobileNetKaggle(Activity activity, Device device, int numThreads, Map<String, List<Serializable>> metaDataInput)
      throws IOException {
    super(activity, device, numThreads, metaDataInput);
    labelProbArray = new float[1][getNumLabels()];
  }

  @Override
  public int getImageSizeX() {
    return 100;
  }

  @Override
  public int getImageSizeY() {
    return 75;
  }

  @Override
  protected String getModelPath() {
    return context.getExternalFilesDir(null).getPath()+ File.separator + "kernels/kagglemobilenet/kaggle_kernel.tflite";
  }

  @Override
  protected String getLabelPath() {
    return context.getExternalFilesDir(null).getPath()+ File.separator + "kernels/kagglemobilenet/labels_kaggle_kernel.txt";
  }

  @Override
  protected int getNumBytesPerChannel() {
    return 4; // Float.SIZE / Byte.SIZE;
  }

  @Override
  protected void addPixelValue(int pixelValue, float[] extremeValues) {
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
    tflite.run(imgData, labelProbArray);
  }

  @Override
  public String getDataDetailPath() { return context.getExternalFilesDir(null).getPath()+ File.separator + "kernels/kagglemobilenet/skin-cancer-data-detail.json"; }

  @Override
  protected void processInput() {
    //stub, no extra input needed
  }
}
