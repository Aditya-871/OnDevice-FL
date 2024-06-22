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

package org.tensorflow.lite.examples.transfer;

import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;
import android.util.Pair;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.tensorflow.lite.examples.transfer.api.AssetModelLoader;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel.LossConsumer;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel.Prediction;


// App-layer wrapper for {TransferLearningModel}.
// This wrapper allows to run training continuously, using start/stop API, in contrast to
// run-once API of {TransferLearningModel}.
public class TransferLearningModelWrapper implements Closeable {
  public static final int IMAGE_SIZE = 32;                                              // size of the input data
  private static final int NUM_THREADS =
          Math.max(1, Runtime.getRuntime().availableProcessors() - 1);                  // number of threads to run the transfer learning
  private final TransferLearningModel model;                                            // an instance of transfer learning model defined in transfer api
  private Context context;                                                              // provides a reference to the environment in which your application is currently running
  private final ConditionVariable shouldTrain = new ConditionVariable();                // conditional variable to start training
  private volatile LossConsumer lossConsumer;                                           // loss consumer for the model training
  private volatile boolean isTerminating = false;                                       // flag to tell whether model training is terminating
  private final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);   // interface to simplify asynchronous task execution

  // constructor for the class
  TransferLearningModelWrapper(Context context) {
    model =
        new TransferLearningModel(
            new AssetModelLoader(context, "model"), Arrays.asList("Walking", "Standing", "Jogging", "Sitting", "Biking", "Upstairs", "Downstairs"));
//    model =
//            new TransferLearningModel(
//                    new AssetModelLoader(context, "model"),
//                    Arrays.asList("cat", "dog", "truck", "bird",
//                            "airplane", "ship", "frog", "horse", "deer",
//                            "automobile"));
    this.context = context;

    new Thread(() -> {
      while (!Thread.interrupted()) {
        shouldTrain.block();
        try {
          Log.i("Epoch Started", "Hi");
          model.train(1, lossConsumer).get();
          Log.i("Epoch Ended", "bye");
        } catch (ExecutionException e) {
          throw new RuntimeException("Exception occurred during model training", e.getCause());
        } catch (InterruptedException e) {
          // no-op
        }
      }
    }).start();
  }

  // This method is thread-safe.
  public Future<Void> addSample(float[] input, String className) {
    return model.addSample(input, className);
  }
  public Future<Void> addSample(float[] image, String className, Boolean isTraining) {
    return model.addSample(image, className, isTraining);
  }

  private void checkNotTerminating() {
    if (isTerminating) {
      throw new IllegalStateException("Cannot operate on terminating model");
    }
  }

  // This method is thread-safe, but blocking.
  public Prediction[] predict(float[] input) {
    return model.predict(input);
  }

  // function to get training batch size
  public int getTrainBatchSize() {
    return model.getTrainBatchSize();
  }

   // Start training the model continuously until {@link #disableTraining() disableTraining} is
   // called.
   // @param lossConsumer callback that the loss values will be passed to.
  public void enableTraining(LossConsumer lossConsumer) {
    this.lossConsumer = lossConsumer;
    shouldTrain.open();
  }

   // Stops training the model.
  public void disableTraining() {
    shouldTrain.close();
  }

  // Frees all model resources and shuts down all background threads.
  public void close() {
    isTerminating = true; model.close();
  }

  // function to save the model
  public void saveModel(File file){
    try {
      FileOutputStream out = new FileOutputStream(file);
      GatheringByteChannel gather = out.getChannel();
      model.saveParameters(gather);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // function to load the model file
  public void loadModel(File file){
    try {
      FileInputStream inp = new FileInputStream(file);
      ScatteringByteChannel scatter = inp.getChannel();
      model.loadParameters(scatter);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // function used to evaluate the model
  public Pair<Float, Float> calculateTestStatistics(){
    return model.getTestStatistics();
  }

  // function to get model parameter
  public ByteBuffer[] getParameters()  {
    return model.getParameters();
  }

  // function to update model parameters
  public void updateParameters(ByteBuffer[] newParams) {
    model.updateParameters(newParams);
  }

  // function to start training
  public void train(int epochs){
    new Thread(() -> {
      shouldTrain.block();
      try {
        model.train(epochs, lossConsumer).get();
      } catch (ExecutionException e) {
        throw new RuntimeException("Exception occurred during model training", e.getCause());
      } catch (InterruptedException e) {
        // no-op
      }
    }).start();
  }

  // function to retrieve size of training dataset
  public int getSize_Training() {
    return model.getSize_Training();
  }

  // function to retrieve size of testing dataset
  public int getSize_Testing() { return model.getSize_Testing(); }
}
