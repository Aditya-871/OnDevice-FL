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
//import objectexplorer.ObjectGraphMeasurer;

public class InferenceActivity extends AppCompatActivity implements SensorEventListener {
    private static final int N_SAMPLES = 100;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mLinearAcceleration;

    private static List<Float> ax;
    private static List<Float> ay;
    private static List<Float> az;
    private static List<Float> gx;
    private static List<Float> gy;
    private static List<Float> gz;
    private static List<Float> lx;
    private static List<Float> ly;
    private static List<Float> lz;
    private static List<Float> ma;
    private static List<Float> ml;
    private static List<Float> mg;
//    private TransferLearningModel.Prediction[] results;
    private float[] results;
    private Classifier classifier;


    Button prevButton;

    private TextView bikingTextView;
    private TextView downstairsTextView;
    private TextView joggingTextView;
    private TextView sittingTextView;
    private TextView standingTextView;
    private TextView upstairsTextView;
    private TextView walkingTextView;
//    TransferLearningModelWrapper tlModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inference);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        classifier = new Classifier(getApplicationContext());
//        tlModel = new TransferLearningModelWrapper(getApplicationContext());

        Log.w("Inference activity","created");

        ax = new ArrayList<>(); ay = new ArrayList<>(); az = new ArrayList<>();
        gx = new ArrayList<>(); gy = new ArrayList<>(); gz = new ArrayList<>();
        lx = new ArrayList<>(); ly = new ArrayList<>(); lz = new ArrayList<>();
        ma = new ArrayList<>(); ml = new ArrayList<>(); mg = new ArrayList<>();

        bikingTextView = (TextView)findViewById(R.id.bikingInferenceValueTextView);
        walkingTextView = (TextView)findViewById(R.id.walkingInferenceValueTextView);
        joggingTextView = (TextView)findViewById(R.id.joggingInferenceValueTextView);
        upstairsTextView = (TextView)findViewById(R.id.upstairsInferenceValueTextView);
        downstairsTextView = (TextView)findViewById(R.id.downstairsInferenceValueTextView);
        sittingTextView = (TextView)findViewById(R.id.sittingInferenceValueTextView);
        standingTextView = (TextView)findViewById(R.id.standingInferenceValueTextView);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,mLinearAcceleration,SensorManager.SENSOR_DELAY_FASTEST);


        prevButton  = (Button) findViewById(R.id.prevButton);

        prevButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), MainActivity.class);
                view.getContext().startActivity(intent);}
        });



    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onDestroy() {
        super.onDestroy();
//        tlModel.close();
//        tlModel = null;
        mSensorManager = null;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.w("Inference activity","sensor changed");

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

    private void setProbabilities() {
//        bikingTextView.setText("hello world!");
//        if(results == null) return;
//        for(TransferLearningModel.Prediction catg : results) {
//            if(catg.getClassName() == "Biking") {
//                bikingTextView.setText(Float.toString(round(catg.getConfidence(), 2)));
//            }
//        }
//        for(TransferLearningModel.Prediction catg : results) {
//            if(catg.getClassName() == "Biking") {
//                downstairsTextView.setText(Float.toString(round(catg.getConfidence(), 2)));
//            }
//        }
//        for(TransferLearningModel.Prediction catg : results) {
//            if(catg.getClassName() == "Biking") {
//                joggingTextView.setText(Float.toString(round(catg.getConfidence(), 2)));
//            }
//        }
//        for(TransferLearningModel.Prediction catg : results) {
//            if(catg.getClassName() == "Biking") {
//                sittingTextView.setText(Float.toString(round(catg.getConfidence(), 2)));
//            }
//        }
//        for(TransferLearningModel.Prediction catg : results) {
//            if(catg.getClassName() == "Biking") {
//                standingTextView.setText(Float.toString(round(catg.getConfidence(), 2)));
//            }
//        }
//        for(TransferLearningModel.Prediction catg : results) {
//            if(catg.getClassName() == "Biking") {
//                upstairsTextView.setText(Float.toString(round(catg.getConfidence(), 2)));
//            }
//        }
//        for(TransferLearningModel.Prediction catg : results) {
//            if(catg.getClassName() == "Biking") {
//                walkingTextView.setText(Float.toString(round(catg.getConfidence(), 2)));
//            }
//        }
        bikingTextView.setText(Float.toString(round(results[0], 2)));
        downstairsTextView.setText(Float.toString(round(results[1], 2)));
        joggingTextView.setText(Float.toString(round(results[2], 2)));
        sittingTextView.setText(Float.toString(round(results[3], 2)));
        standingTextView.setText(Float.toString(round(results[4], 2)));
        upstairsTextView.setText(Float.toString(round(results[5], 2)));
        walkingTextView.setText(Float.toString(round(results[6], 2)));
    }

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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }
}