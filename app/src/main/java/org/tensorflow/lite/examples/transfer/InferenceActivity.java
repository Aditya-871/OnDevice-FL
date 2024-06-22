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
import android.widget.Button;
import android.widget.TextView;

import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// activity to perform on device inference and show each output classes with their corresponding confidence value
public class InferenceActivity extends AppCompatActivity implements SensorEventListener {
    private static final int N_SAMPLES = 100;   // window size used in model training
    private SensorManager mSensorManager;       // an instance of sensor manager to handle required sensors
    private Sensor mAccelerometer;              // an instance of accelerometer sensor
    private Sensor mGyroscope;                  // an instance of gyroscope sensor
    private Sensor mLinearAcceleration;         // an instance of linear acceleration sensor
    private static List<Float> ax;              // list to store x-component of accelerometer readings in order of their timestamp
    private static List<Float> ay;              // list to store y-component of accelerometer readings in order of their timestamp
    private static List<Float> az;              // list to store z-component of accelerometer readings in order of their timestamp
    private static List<Float> gx;              // list to store x-component of gyroscope readings in order of their timestamp
    private static List<Float> gy;              // list to store y-component of gyroscope readings in order of their timestamp
    private static List<Float> gz;              // list to store z-component of gyroscope readings in order of their timestamp
    private static List<Float> lx;              // list to store x-component of linear acceleration readings in order of their timestamp
    private static List<Float> ly;              // list to store y-component of linear acceleration readings in order of their timestamp
    private static List<Float> lz;              // list to store z-component of linear acceleration readings in order of their timestamp
    private static List<Float> ma;              // list to store magnitude of readings of accelerometer in order of their timestamp
    private static List<Float> ml;              // list to store magnitude of readings of linear acceleration in order of their timestamp
    private static List<Float> mg;              // list to store magnitude of readings of gyroscope in order of their timestamp
    private float[] results;                    // float array to store the output result i.e. confidence value for each class
    private Classifier classifier;              // an instance of classifier class to handle inference requests
    private Button prevButton;                  // button to navigate to main activity
    private TextView bikingTextView;            // textview to show the confidence value of biking class
    private TextView downstairsTextView;        // textview to show the confidence value of downstairs class
    private TextView joggingTextView;           // textview to show the confidence value of jogging class
    private TextView sittingTextView;           // textview to show the confidence value of sitting class
    private TextView standingTextView;          // textview to show the confidence value of standing class
    private TextView upstairsTextView;          // textview to show the confidence value of upstairs class
    private TextView walkingTextView;           // textview to show the confidence value of walking class

    // defines what to do when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inference);                                                // set layout for the activity
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);                  // allocated the sensor manager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);                // allocated the accelerometer sensor
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);                        // allocated the gyroscope sensor
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);     // allocated the linear acceleration sensor

        classifier = new Classifier(getApplicationContext());                                       // allocated the classifier object

        ax = new ArrayList<>(); ay = new ArrayList<>(); az = new ArrayList<>();                     // allocated accelerometer readings list
        gx = new ArrayList<>(); gy = new ArrayList<>(); gz = new ArrayList<>();                     // allocated gyroscope readings list
        lx = new ArrayList<>(); ly = new ArrayList<>(); lz = new ArrayList<>();                     // allocated linear acceleration readings list
        ma = new ArrayList<>(); ml = new ArrayList<>(); mg = new ArrayList<>();                     // allocated magnitude of readings list

        bikingTextView = (TextView)findViewById(R.id.bikingInferenceValueTextView);                 // set the biking textview with its corresponding resource
        walkingTextView = (TextView)findViewById(R.id.walkingInferenceValueTextView);               // set the walking textview with its corresponding resource
        joggingTextView = (TextView)findViewById(R.id.joggingInferenceValueTextView);               // set the jogging textview with its corresponding resource
        upstairsTextView = (TextView)findViewById(R.id.upstairsInferenceValueTextView);             // set the upstairs textview with its corresponding resource
        downstairsTextView = (TextView)findViewById(R.id.downstairsInferenceValueTextView);         // set the downstairs textview with its corresponding resource
        sittingTextView = (TextView)findViewById(R.id.sittingInferenceValueTextView);               // set the sitting textview with its corresponding resource
        standingTextView = (TextView)findViewById(R.id.standingInferenceValueTextView);             // set the standing textview with its corresponding resource

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);      // register the accelerometer sensor with sensor manager
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);          // register the gyroscope sensor with sensor manager
        mSensorManager.registerListener(this,mLinearAcceleration,SensorManager.SENSOR_DELAY_FASTEST);   // register the linear acceleration sensor with sensor manager


        prevButton  = (Button) findViewById(R.id.prevButton);

        prevButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), MainActivity.class);
                view.getContext().startActivity(intent);}
        });
    }

    // defines what to do when activity is paused
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // defines what to do when activity is resumed
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
    }

    // defines what to do when activity is destroyed
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager = null;
    }

    // defines what to do when sensor reading changes
    @Override
    public void onSensorChanged(SensorEvent event) {

        activityPrediction();

        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if(event.values[0]>0.0) Log.w("Inference activity","accelerometer "+event.values[0]);

            ax.add(event.values[0]);
            ay.add(event.values[1]);
            az.add(event.values[2]);

        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx.add(event.values[0]);
            gy.add(event.values[1]);
            gz.add(event.values[2]);
        } else if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            lx.add(event.values[0]);
            ly.add(event.values[1]);
            lz.add(event.values[2]);
        }
    }

    // function to set the confidence value corresponding to each class
    private void setProbabilities() {
        bikingTextView.setText(Float.toString(round(results[0], 2)));
        downstairsTextView.setText(Float.toString(round(results[1], 2)));
        joggingTextView.setText(Float.toString(round(results[2], 2)));
        sittingTextView.setText(Float.toString(round(results[3], 2)));
        standingTextView.setText(Float.toString(round(results[4], 2)));
        upstairsTextView.setText(Float.toString(round(results[5], 2)));
        walkingTextView.setText(Float.toString(round(results[6], 2)));
    }

    // function to do prediction
    private void activityPrediction() {
        List<Float> data = new ArrayList<>();

        if (ax.size() >= N_SAMPLES && ay.size() >= N_SAMPLES && az.size() >= N_SAMPLES
                && gx.size() >= N_SAMPLES && gy.size() >= N_SAMPLES && gz.size() >= N_SAMPLES
                && lx.size() >= N_SAMPLES && ly.size() >= N_SAMPLES && lz.size() >= N_SAMPLES
        ) {
            double maValue; double mgValue; double mlValue;

            for( int i = 0; i < N_SAMPLES ; i++ ) {
                maValue = Math.sqrt(Math.pow(ax.get(i), 2) + Math.pow(ay.get(i), 2) + Math.pow(az.get(i), 2));
                mlValue = Math.sqrt(Math.pow(lx.get(i), 2) + Math.pow(ly.get(i), 2) + Math.pow(lz.get(i), 2));
                mgValue = Math.sqrt(Math.pow(gx.get(i), 2) + Math.pow(gy.get(i), 2) + Math.pow(gz.get(i), 2));

                ma.add((float)maValue);
                ml.add((float)mlValue);
                mg.add((float)mgValue);
            }

            data.addAll(ax.subList(0, N_SAMPLES));
            data.addAll(ay.subList(0, N_SAMPLES));
            data.addAll(az.subList(0, N_SAMPLES));

            data.addAll(lx.subList(0, N_SAMPLES));
            data.addAll(ly.subList(0, N_SAMPLES));
            data.addAll(lz.subList(0, N_SAMPLES));

            data.addAll(gx.subList(0, N_SAMPLES));
            data.addAll(gy.subList(0, N_SAMPLES));
            data.addAll(gz.subList(0, N_SAMPLES));

            data.addAll(ma.subList(0, N_SAMPLES));
            data.addAll(ml.subList(0, N_SAMPLES));
            data.addAll(mg.subList(0, N_SAMPLES));

            results = classifier.predictProbabilities(toFloatArray(data));

            setProbabilities();

            ax.clear(); ay.clear(); az.clear();
            gx.clear(); gy.clear(); gz.clear();
            lx.clear(); ly.clear(); lz.clear();
        }
    }

    // defines what to do when accuracy changes
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // function to round up floating numbers
    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    // function to create array of 'float' from list of 'Float'
    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }
}