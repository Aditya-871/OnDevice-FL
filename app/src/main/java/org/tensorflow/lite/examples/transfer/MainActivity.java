package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.os.Vibrator;
import android.widget.Toast;

enum Mode {
  Data_Collection,
  Training
}

public class MainActivity extends AppCompatActivity implements SensorEventListener  {

  final int NUM_SAMPLES = 100;  // number of data points in a window
  String MODEL_NAME = "ar_model";
//  double VB_THRESHOLD = 0.75;
  public boolean a=false;

  int classAInstanceCount = 0;  // number of windows collected for class A i.e. 'Walking'
  int classBInstanceCount = 0;  // number of windows collected for class B i.e. 'Standing'
  int classCInstanceCount = 0;  // number of windows collected for class C i.e. 'Jogging'
  int classDInstanceCount = 0;  // number of windows collected for class D i.e. 'Sitting'
  int classEInstanceCount = 0;  // number of windows collected for class E i.e. 'Biking'
  int classFInstanceCount = 0;  // number of windows collected for class F i.e. 'Upstairs'
  int classGInstanceCount = 0;  // number of windows collected for class G i.e. 'Downstairs'
  boolean isRunning = false;    // flag to tell whether data collection is on-going or not

  SensorManager mSensorManager;         // sensor manager to handle and register all required sensors
  Sensor mAccelerometer;                // accelerometer sensor
  Sensor mGyroscope;                    // gyroscope sensor
  TransferLearningModelWrapper tlModel; // wrapper object to perform training
  String classId;
  static List<Float> x_accel;           // stores the x component of accelerometer reading in order of their timestamp
  static List<Float> y_accel;           // stores the y component of accelerometer reading in order of their timestamp
  static List<Float> z_accel;           // stores the z component of accelerometer reading in order of their timestamp
  static List<Float> x_gyro;            // stores the x component of gyroscope reading in order of their timestamp
  static List<Float> y_gyro;            // stores the y component of gyroscope reading in order of their timestamp
  static List<Float> z_gyro;            // stores the z component of gyroscope reading in order of their timestamp
  static List<Float> input_signal;      // stores the reading of all sensors consecutively for data processing and training

  Mode mode;        // defines the mode in which app is being used

  Button startButton;           // button to start data collection
  Button stopButton;            // button to stop data collection
  Button inferenceButton;       // button to navigate inference activity
  Button startFlButton;         // button to navigate federated learning activity

  TextView classAInstanceCountTextView;       // textview to show the number of windows collected for class A i.e. 'Walking'
  TextView classBInstanceCountTextView;       // textview to show the number of windows collected for class B i.e. 'Standing'
  TextView classCInstanceCountTextView;       // textview to show the number of windows collected for class C i.e. 'Jogging'
  TextView classDInstanceCountTextView;       // textview to show the number of windows collected for class D i.e. 'Sitting'
  TextView classEInstanceCountTextView;       // textview to show the number of windows collected for class E i.e. 'Biking'
  TextView classFInstanceCountTextView;       // textview to show the number of windows collected for class F i.e. 'Upstairs'
  TextView classGInstanceCountTextView;       // textview to show the number of windows collected for class G i.e. 'Downstairs'

  TextView lossValueTextView;       // textview to show the loss value of model training
  Spinner optionSpinner;            // spinner object to generate dropdown menu to select mode
  Spinner classSpinner;             // spinner object to generate dropdown menu to select class for data collection
  Vibrator vibrator;                // vibrator to alarm at beginning of the activity

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    // link buttons with their corresponding resources in main activity
    startButton = (Button) findViewById(R.id.buttonStart);
    stopButton = (Button) findViewById(R.id.buttonStop);
    inferenceButton = (Button) findViewById(R.id.inferenceButton);
    startFlButton = (Button)findViewById(R.id.startFL);
    stopButton.setEnabled(false);

    // link the textview resources in main activity of app with their corresponding variable
    classAInstanceCountTextView = (TextView)findViewById(R.id.classACountValueTextView);
    classBInstanceCountTextView = (TextView)findViewById(R.id.classBCountValueTextView);
    classCInstanceCountTextView = (TextView)findViewById(R.id.classCCountValueTextView);
    classDInstanceCountTextView = (TextView)findViewById(R.id.classDCountValueTextView);
    classEInstanceCountTextView = (TextView)findViewById(R.id.classECountValueTextView);
    classFInstanceCountTextView = (TextView)findViewById(R.id.classFCountValueTextView);
    classGInstanceCountTextView = (TextView)findViewById(R.id.classGCountValueTextView);

    lossValueTextView = (TextView)findViewById(R.id.lossValueTextView);

    // link spinners to their corresponding resources
    optionSpinner = (Spinner) findViewById(R.id.optionSpinner);
    classSpinner = (Spinner) findViewById(R.id.classSpinner);

    ArrayAdapter<CharSequence> optionAdapter = ArrayAdapter
            .createFromResource(this, R.array.options_array,
                    R.layout.spinner_item);
    optionAdapter
            .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    optionSpinner.setAdapter(optionAdapter);

    ArrayAdapter<CharSequence> classAdapter = ArrayAdapter
            .createFromResource(this, R.array.class_array,
                    R.layout.spinner_item);
    classAdapter
            .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    classSpinner.setAdapter(classAdapter);

    // allocate memory to list of sensor readings and other arrays
    x_accel = new ArrayList<Float>();
    y_accel = new ArrayList<Float>();
    z_accel = new ArrayList<Float>();
    x_gyro= new ArrayList<Float>();
    y_gyro = new ArrayList<Float>();
    z_gyro = new ArrayList<Float>();
    input_signal = new ArrayList<Float>();

    // create instances of required sensor
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    if(mGyroscope != null){
      Log.w("myApp", "gyro is available");
    }
    else
    {
      Log.w("myApp", "gyro is not available");
    }

    if(mAccelerometer != null){
      Log.w("myApp", "accel is available");
    }
    else
    {
      Log.w("myApp", "accel is not available");
    }

    // register sensors listener to start reading sensor values
    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

    // create an instance of transfer learning model wrapper
    tlModel = new TransferLearningModelWrapper(getApplicationContext());

    // set the spinner to select mode of data collection or training
    optionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view,
                                 int position, long id) {
        String option = (String) parent.getItemAtPosition(position);
        switch (option){
          case "Data Collection":
            mode = Mode.Data_Collection;
            break;
          case "Training":
            mode = Mode.Training;
            break;
//
          default:
            throw new IllegalArgumentException("Invalid app mode.");
        }
        a=true;
        //Log.i("myapp100", mode.toString());
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
      }
    });

    // set the spinner to select class for data collection
    classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view,
                                 int position, long id) {
        classId = (String) parent.getItemAtPosition(position);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
      }
    });

    // set on click listener for start button
    startButton.setOnClickListener( new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        optionSpinner.setEnabled(false);
        isRunning = true;

      }
    });

    // set on click listener for stop button
    stopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        optionSpinner.setEnabled(true);
        isRunning = false;
        if(mode == Mode.Training) {
          tlModel.disableTraining();

          File modelPath = getApplicationContext().getFilesDir();
          File modelFile = new File(modelPath, MODEL_NAME);
          tlModel.saveModel(modelFile);


          System.out.println(modelFile);
          Toast.makeText(getApplicationContext(), "Model saved.", Toast.LENGTH_SHORT).show();
        }
      }
    });

    // set on click listener for inference button
    inferenceButton.setOnClickListener( new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), InferenceActivity.class);
        view.getContext().startActivity(intent);}
    });

    // set on click listener for federated learning button
    startFlButton.setOnClickListener( new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), FLActivity.class);
        view.getContext().startActivity(intent);}
    });

  }

  // function defines what to do when activity pauses
  protected void onPause() {
    super.onPause();
    mSensorManager.unregisterListener(this);
  }

  // function defines what to do when activity is resumed
  protected void onResume() {
    super.onResume();
    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
  }

  // function defines what to do when activity is destroyed
  protected void onDestroy() {
    super.onDestroy();
    tlModel.close();
    tlModel = null;
    mSensorManager = null;
  }

  // defines the tasks to perform when value of any sensor reading changes
  @Override
  public void onSensorChanged(SensorEvent event) {
    switch (event.sensor.getType()) {
      case Sensor.TYPE_ACCELEROMETER:
      { x_accel.add(event.values[0]); y_accel.add(event.values[1]); z_accel.add(event.values[2]);
      }
        break;
      case Sensor.TYPE_GYROSCOPE:
      {x_gyro.add(event.values[0]); y_gyro.add(event.values[1]); z_gyro.add(event.values[2]);
      }
        break;
    }

    //Check if we have desired number of samples for sensors, if yes, the process input.
    //Log.i("size_of_accel", String.valueOf(x_accel.size()));
    //Log.i("size_of_gyro", String.valueOf(x_gyro.size()));
    int batchSize = tlModel.getTrainBatchSize();
   // Log.i("batchsize", String.valueOf(batchSize));

    if(x_accel.size() >= NUM_SAMPLES && y_accel.size() >= NUM_SAMPLES &&
            z_accel.size() >= NUM_SAMPLES && x_gyro.size() >= NUM_SAMPLES &&
            y_gyro.size() >= NUM_SAMPLES && z_gyro.size() >= NUM_SAMPLES)
      processInput();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }

  // function to process input i.e. arrays of sensor readings
  private void processInput()
  {
      int i = 0;
      // create a window with NUM_SAMPLES data points
      while (i < NUM_SAMPLES) {
        input_signal.add(x_accel.get(i));
        input_signal.add(y_accel.get(i));
        input_signal.add(z_accel.get(i));
        input_signal.add(x_gyro.get(i));
        input_signal.add(y_gyro.get(i));
        input_signal.add(z_gyro.get(i));
        i++;
      }

      float[] input = toFloatArray(input_signal);

      if (isRunning){
        if(mode == Mode.Training){
          int batchSize = tlModel.getTrainBatchSize();

          // tryyyyyyyyyyyyyyyyyy
          Log.i("batchsize", String.valueOf(batchSize));

          if(classAInstanceCount >= batchSize && classBInstanceCount >= batchSize && classCInstanceCount >= batchSize && classDInstanceCount >= batchSize && classEInstanceCount >= batchSize && classFInstanceCount >= batchSize && classGInstanceCount >= batchSize){
            //System.console()
            Log.i("training_check", "training started #####################################");
            tlModel.enableTraining((epoch, loss) -> runOnUiThread(new Runnable() {
              @Override
              public void run() {
                lossValueTextView.setText(Float.toString(loss));
              }
            }));
            Log.i("training_check", "training stopped #####################################");
          }
          else{
            String message = batchSize + " instances per class are required for training.";
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            stopButton.callOnClick();
          }

        }
        else if (mode == Mode.Data_Collection){

          Log.w("data_collection_check", "data collection started");
          Log.w("data_collection_check", "data collection started");
          //Log.i("classid", classId);
//          tlModel.addSample(input, classId);

          // update the instance count for selected class
          if (classId.equals("Walking")) classAInstanceCount += 1;
          else if(classId.equals("Standing")) classBInstanceCount += 1;
          else if(classId.equals("Jogging")) classCInstanceCount += 1;
          else if(classId.equals("Sitting")) classDInstanceCount += 1;
          else if(classId.equals("Biking")) classEInstanceCount += 1;
          else if(classId.equals("Upstairs")) classFInstanceCount += 1;
          else if(classId.equals("Downstairs")) classGInstanceCount += 1;

          // display the instance count in textview of activity
          classAInstanceCountTextView.setText(Integer.toString(classAInstanceCount));
          classBInstanceCountTextView.setText(Integer.toString(classBInstanceCount));
          classCInstanceCountTextView.setText(Integer.toString(classCInstanceCount));
          classDInstanceCountTextView.setText(Integer.toString(classDInstanceCount));
          classEInstanceCountTextView.setText(Integer.toString(classEInstanceCount));
          classFInstanceCountTextView.setText(Integer.toString(classFInstanceCount));
          classGInstanceCountTextView.setText(Integer.toString(classGInstanceCount));

        }

      }

      // Clear all the values
      x_accel.clear(); y_accel.clear(); z_accel.clear();
      x_gyro.clear(); y_gyro.clear(); z_gyro.clear();
      input_signal.clear();
  }

  // auxiliary function to type cast list of 'Float' to array of 'float'
  private float[] toFloatArray(List<Float> list)
  {
    int i = 0;
    float[] array = new float[list.size()];

    for (Float f : list) {
      array[i++] = (f != null ? f : Float.NaN);
    }
    return array;
  }

  // auxiliary function to round up a floating value
  private static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }
}
