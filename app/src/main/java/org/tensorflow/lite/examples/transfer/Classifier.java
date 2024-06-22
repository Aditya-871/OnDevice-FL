package org.tensorflow.lite.examples.transfer;

import android.content.Context;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


// Class to perform on device inference using saved model
public class Classifier {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TensorFlowInferenceInterface inferenceInterface;                            // an instance of inference interface provided by tensorflow for doing on-device inference
    private static final String MODEL_FILE = "file:///android_asset/frozen_HAR.pb";     // represents the path to saved frozen model
    private static final String INPUT_NODE = "LSTM_1_input";                            // represents the first input layer of our tensorflow model
    private static final String[] OUTPUT_NODES = {"Dense_2/Softmax"};                   // array to store all output nodes name of our tensorflow model
    private static final String OUTPUT_NODE = "Dense_2/Softmax";                        // represents the output node of our tensorflow model
    private static final long[] INPUT_SIZE = {1, 100, 12};                              // represents the shape of input to our tensorflow model
    private static final int OUTPUT_SIZE = 7;                                           // represent the number of output classes of our tensorflow model

    // constructor for the class
    public Classifier(final Context context) {
        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    // function which returns the confidence value for each output class
    public float[] predictProbabilities(float[] data) {
        float[] result = new float[OUTPUT_SIZE];
        inferenceInterface.feed(INPUT_NODE, data, INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE, result);

        //Biking   Downstairs	 Jogging	  Sitting	Standing	Upstairs	Walking
        return result;
    }
}
