package cf.homeit.fateofball.Activity;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Random;

import cf.homeit.fateofball.Interpolator.MyBounceInterpolator;
import cf.homeit.fateofball.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {


    private static final String mBackground = "wall.jpg";
    private final String TAG = MainActivity.class.getSimpleName();
    private static final int RAND_INT_MAX = 20;

    private TextView mPrediction;
    private final Random mRandomizer = new Random();
    private static final int FADEIN_DURATION = 500;
    private static final int PREDICTION_DURATION = 5000;
    private static final int FADEOUT_DURATION = 500;
    private final float[] mRotationMatrix = new float[16];
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float mSensorZ = -12345;
    /**
     * Countdown timer to count the time needed before hiding the prediction
     */
    private final CountDownTimer mTimer = new CountDownTimer(FADEIN_DURATION + PREDICTION_DURATION, 1000) {
        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            hidePrediction();
        }
    };
    private static final int UPSIDE_DOWN_MIN_COUNT = 15;
    private int mUpsideDownCounter = 0;


    private Vibrator mVibrator;
    private static final int VIBRATE_TIME = 130;
    private ConstraintLayout clbg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        AdSize adSize = new AdSize(300, 250);
        mPrediction = findViewById(R.id.prediction);
        clbg = findViewById(R.id.main_cl);

        mPrediction.setVisibility(TextView.INVISIBLE);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null) {
            Log.w(TAG, "No vibration service exists.");
        }
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        loadImageFromAsset(mBackground, clbg);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdView mAdView = findViewById(R.id.ad_view);


        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(Collections.singletonList("21D4B1EFA1F718FB8D08108FCBCB3114"))
                        .build());

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onResume() {
        super.onResume();
        activateSensorListener();
        if (mSensor == null) {
            Log.w(TAG, "No gyroscope sensor is available.");
        }
    }

    @Override
    public void onPause() {
        desactivateSensorListener();
        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor s, int acc) {
        // Empty
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            if (isReadyToPredict(mRotationMatrix[10])) {
                startPrediction();
            }
        }
    }

    /**
     * Method to test if user is accomplished turn upside-down move of the device
     *  // and turned back
     *  Input is a value of z component of the rotation matrix <-> M(2,2) or M[10]
     * */
    @SuppressLint("LongLogTag")
    protected boolean isReadyToPredict(float z) {

        // if sensorZ is not initialized :
        if (mSensorZ < -1) {
            mSensorZ = z;
            return false;
        }

        // neglect z value less than 0.7
        if (Math.abs(z) < 0.7) {
            return false;
        }

        // register the input sensor value if z is negative
        // (device is upside down) :
        if (z < 0) {
            if (mSensorZ > 0 &&
                    mUpsideDownCounter > UPSIDE_DOWN_MIN_COUNT ) {
                Log.i(TAG, "Device is set upside-down");
                if (mVibrator !=null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mVibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        mVibrator.vibrate(VIBRATE_TIME);
                    }
                }
                mSensorZ = z;
                return true;
            }
            mUpsideDownCounter++;
        }

        if (z > 0) {
            if (mSensorZ < 0){
                Log.i(TAG, "Device is back");
                // device is returned back
                // -> registered current position
                mSensorZ = z;
                mUpsideDownCounter=0;
//                return true;
                return false;
            }
        }

        return false;
    }

    /** Method to enable sensor when app is restored */
    protected void activateSensorListener() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /** Method to disable sensor when app is paused */
    protected void desactivateSensorListener() {
        mSensorManager.unregisterListener(this);
    }

    /** Testing method responding on button click */
    public void onPredictTestBClicked(View view) {
        startPrediction();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        final ImageView bAbout = findViewById(R.id.action_about);
        final ImageView bSettings = findViewById(R.id.action_settings);
        bAbout.setOnClickListener(this);
        bSettings.setOnClickListener(this);
        switch (view.getId()) {
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            case R.id.action_settings:
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Settings", Toast.LENGTH_SHORT);
                toast.show();
                break;
        }
    }


    /** Method to display the prediciton with animations */
    protected void startPrediction() {
        mPrediction.setText(getNextPrediction());
        showPrediction();
        mTimer.start();
    }

    /** Method to animate prediction appearance */
    protected void showPrediction() {
        AlphaAnimation animation = new AlphaAnimation(0, 1);
        animation.setStartOffset(0);
        animation.setDuration(FADEIN_DURATION);
        mPrediction.setVisibility(TextView.VISIBLE);
        if (mVibrator !=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                mVibrator.vibrate(VIBRATE_TIME);
            }
        }
        ImageView ball = findViewById(R.id.eight_ball);
        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 0.6);
        myAnim.setInterpolator(interpolator);
        ball.startAnimation(myAnim);
        mPrediction.startAnimation(animation);
    }

    /** Method to animate prediction disappearance */
    protected void hidePrediction() {
        AlphaAnimation animation = new AlphaAnimation(1, 0);
        animation.setDuration(FADEOUT_DURATION);
        mPrediction.setVisibility(TextView.INVISIBLE);

        mPrediction.startAnimation(animation);
    }

    /**
     * Method to pick randomly the next prediction
     */
    private String getNextPrediction() {
        int i = mRandomizer.nextInt(RAND_INT_MAX);
        return getResources().getStringArray(R.array.responses)[i];
    }

    public void loadImageFromAsset(String fileName, ConstraintLayout cl) {
        try {
            // получаем входной поток
            InputStream ims = getAssets().open(fileName);
            // загружаем как Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // выводим картинку в ImageView
            cl.setBackground(d);
        } catch (IOException ex) {
            return;
        }
    }

}