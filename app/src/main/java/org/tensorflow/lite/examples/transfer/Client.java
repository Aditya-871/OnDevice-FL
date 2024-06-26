package org.tensorflow.lite.examples.transfer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ConditionVariable;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

// this class represents a client in a federated learning system and it implements all necessary functions for federated learning
public class Client {

    private TransferLearningModelWrapper tlModel;                           // a transfer learning model wrapper instance to process requests related to transfer learning
    private static final int LOWER_BYTE_MASK = 0xFF;                        // used for pre-processing the data before training
    private MutableLiveData<Float> lastLoss = new MutableLiveData<>();      // stores the last loss value of model training
    private Context context;                                                // provides a reference to the environment in which the application is currently running
    private final ConditionVariable isTraining = new ConditionVariable();   // flag to tell whether model training is on-going or not
    private static String TAG = "Flower";                                   // TAG used while logging messages
    private int local_epochs = 1;                                           // represents the number of epochs for training in federated learning

    // constructor for the class
    public Client(Context context) {
        this.tlModel = new TransferLearningModelWrapper(context);
        this.context = context;
    }

    // function to get model weights for federated learning
    public ByteBuffer[] getWeights() {
        return tlModel.getParameters();
    }

    // function to train the on device model
    public Pair<ByteBuffer[], Integer> fit(ByteBuffer[] weights, int epochs) {

        this.local_epochs = epochs;
        tlModel.updateParameters(weights);
        isTraining.close();
        tlModel.train(this.local_epochs);
        tlModel.enableTraining((epoch, loss) -> setLastLoss(epoch, loss));
        Log.e(TAG ,  "Training enabled. Local Epochs = " + this.local_epochs);
        isTraining.block();
        return Pair.create(getWeights(), tlModel.getSize_Training());
    }

    // function to evaluate the received global model against local data
    public Pair<Pair<Float, Float>, Integer> evaluate(ByteBuffer[] weights) {
        tlModel.updateParameters(weights);
        tlModel.disableTraining();
        return Pair.create(tlModel.calculateTestStatistics(), tlModel.getSize_Testing());
    }

    // function to set training loss value
    public void setLastLoss(int epoch, float newLoss) {
        if (epoch == this.local_epochs - 1) {
            Log.e(TAG, "Training finished after epoch = " + epoch);
            lastLoss.postValue(newLoss);
            tlModel.disableTraining();
            isTraining.open();
        }
    }

    // function to load data from device memory before on device training starts
    public void loadData(int device_id) {
        try {
            Log.d("FLOWERCLIENT_LOAD", "loadData: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.context.getAssets().open("data/partition_" + (device_id - 1) + "_train.txt")));
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                i++;
//                Log.e(TAG, i + "th training image loaded");
                addSample("data/" + line, true);
            }
            reader.close();

            i = 0;
            reader = new BufferedReader(new InputStreamReader(this.context.getAssets().open("data/partition_" +  (device_id - 1)  + "_test.txt")));
            while ((line = reader.readLine()) != null) {
                i++;
//                Log.e(TAG, i + "th test image loaded");
                addSample("data/" + line, false);
            }
            reader.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // function to add sample for model training
    private void addSample(String photoPath, Boolean isTraining) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap =  BitmapFactory.decodeStream(this.context.getAssets().open(photoPath), null, options);
        String sampleClass = get_class(photoPath);

        // get rgb equivalent and class
        float[] rgbImage = prepareImage(bitmap);

        // add to the list.
        try {
            this.tlModel.addSample(rgbImage, sampleClass, isTraining).get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to add sample to model", e.getCause());
        } catch (InterruptedException e) {
            // no-op
        }
    }

    public String get_class(String path) {
        String label = path.split("/")[2];
        return label;
    }

    /**
     * Normalizes a camera image to [0; 1], cropping it
     * to size expected by the model and adjusting for camera rotation.
     */
    private static float[] prepareImage(Bitmap bitmap)  {
        int modelImageSize = TransferLearningModelWrapper.IMAGE_SIZE;

        float[] normalizedRgb = new float[modelImageSize * modelImageSize * 3];
        int nextIdx = 0;
        for (int y = 0; y < modelImageSize; y++) {
            for (int x = 0; x < modelImageSize; x++) {
                int rgb = bitmap.getPixel(x, y);

                float r = ((rgb >> 16) & LOWER_BYTE_MASK) * (1 / 255.0f);
                float g = ((rgb >> 8) & LOWER_BYTE_MASK) * (1 / 255.0f);
                float b = (rgb & LOWER_BYTE_MASK) * (1 / 255.0f);

                normalizedRgb[nextIdx++] = r;
                normalizedRgb[nextIdx++] = g;
                normalizedRgb[nextIdx++] = b;
            }
        }

        return normalizedRgb;
    }

    // function to write to a file :
    public void writeStringToFile( Context context , String fileName, String content) {
        try {
            // Get the app-specific external storage directory
            File directory = context.getExternalFilesDir(null);

            if (directory != null) {
                File file = new File(directory, fileName);

                // Check if the file exists
                boolean fileExists = file.exists();

                // Open a FileWriter in append mode
                FileWriter writer = new FileWriter(file, true);

                // If the file exists and is not empty, add a new line
                if (fileExists && file.length() > 0) {
                    writer.append("\n");
                }

                // Write the string to the file
                writer.append(content);

                // Close the FileWriter
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception as needed
        }
    }
}
