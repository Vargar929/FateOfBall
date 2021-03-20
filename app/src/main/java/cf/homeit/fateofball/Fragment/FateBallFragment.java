package cf.homeit.fateofball.Fragment;

import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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

import cf.homeit.fateofball.Auxiliary.Constants;
import cf.homeit.fateofball.Interpolator.MyBounceInterpolator;
import cf.homeit.fateofball.R;
import cf.homeit.fateofball.SQLiteHelper.DBHelper;

import static cf.homeit.fateofball.Auxiliary.Constants.FADEIN_DURATION;
import static cf.homeit.fateofball.Auxiliary.Constants.FADEOUT_DURATION;
import static cf.homeit.fateofball.Auxiliary.Constants.RAND_INT_MAX;
import static cf.homeit.fateofball.Auxiliary.Constants.UPSIDE_DOWN_MIN_COUNT;
import static cf.homeit.fateofball.Auxiliary.Constants.VIBRATE_TIME;
import static cf.homeit.fateofball.Auxiliary.Constants.mRotationMatrix;
import static cf.homeit.fateofball.Auxiliary.Constants.mSensorZ;
import static cf.homeit.fateofball.Auxiliary.Constants.mUpsideDownCounter;

public class FateBallFragment extends Fragment implements SensorEventListener, View.OnClickListener {
    public final Random mRandomizer = new Random();
    private final String TAG = FateBallFragment.class.getSimpleName();
    private TextView mPrediction;
    private ConstraintLayout clbg;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Vibrator mVibrator;
    private AdView mAdView;
    private final CountDownTimer mTimer = new CountDownTimer(FADEIN_DURATION + Constants.PREDICTION_DURATION, 1000) {
        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            hidePrediction();
        }
    };
    private DBHelper dbHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        activateSensorListener();
        if (mSensor == null) {
            Log.w(TAG, "No gyroscope sensor is available.");
        }
    }

    private ImageView bAbout, bSettings, bFob;

    @Override
    public void onPause() {
        deactivateSensorListener();
        super.onPause();
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container, false);
        associateElements(view);
//        loadImageFromAsset(mBackground, clbg);
        assocSensors();
        adsViewsEngine();
        dbHelper = new DBHelper(getActivity().getApplicationContext());
        mPrediction.setVisibility(TextView.INVISIBLE);
        bAbout.setOnClickListener(this);
        bFob.setOnClickListener(this);
        bSettings.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        NavController navController;
        navController = Navigation.findNavController(requireActivity(), R.id.first_nav_host);
        switch (v.getId()) {
            case R.id.action_about:
                navController.navigate(R.id.aboutFragment);
                break;
            case R.id.action_settings:
                navController.navigate(R.id.settingsFragment);
                break;
            case R.id.eight_ball:
                onPredictTestBClicked();
                break;
        }

    }

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
                    mUpsideDownCounter > UPSIDE_DOWN_MIN_COUNT) {
                Log.i(TAG, "Device is set upside-down");
                if (mVibrator != null) {
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
            if (mSensorZ < 0) {
                Log.i(TAG, "Device is back");
                // device is returned back
                // -> registered current position
                mSensorZ = z;
                mUpsideDownCounter = 0;
//                return true;
                return false;
            }
        }

        return false;
    }

    public void loadImageFromAsset(String fileName, ConstraintLayout cl) {
//        String filename = "dubi2.png";
        InputStream inputStream = null;
        try {
            inputStream = requireActivity().getApplicationContext().getAssets().open(fileName);
            Drawable drawable = Drawable.createFromStream(inputStream, null);
            cl.setBackground(drawable);
//            imageView.setImageDrawable(drawable);
//            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Method to enable sensor when app is restored
     */
    protected void activateSensorListener() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Method to disable sensor when app is paused
     */
    protected void deactivateSensorListener() {
        mSensorManager.unregisterListener(this);
    }

    /**
     * Testing method responding on button click
     */
    public void onPredictTestBClicked() {
        startPrediction();
    }

    /**
     * Method to display the prediciton with animations
     */
    protected void startPrediction() {
        mPrediction.setText(getNextPrediction());
        showPrediction();
        mTimer.start();
    }

    /**
     * Method to animate prediction appearance
     */
    protected void showPrediction() {
        AlphaAnimation animation = new AlphaAnimation(0, 1);
        animation.setStartOffset(0);
        animation.setDuration(FADEIN_DURATION);
        mPrediction.setVisibility(TextView.VISIBLE);
        if (mVibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                mVibrator.vibrate(VIBRATE_TIME);
            }
        }
        ImageView ball = requireActivity().findViewById(R.id.eight_ball);
        final Animation myAnim = AnimationUtils.loadAnimation(requireActivity().getApplicationContext(), R.anim.bounce);
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 0.6);
        myAnim.setInterpolator(interpolator);
        ball.startAnimation(myAnim);
        mPrediction.startAnimation(animation);
    }

    /**
     * Method to animate prediction disappearance
     */
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

    private void associateElements(View v) {
        mAdView = v.findViewById(R.id.ad_view);
        mPrediction = v.findViewById(R.id.prediction);
        clbg = v.findViewById(R.id.main_cl);
        bFob = v.findViewById(R.id.eight_ball);
        bAbout = v.findViewById(R.id.action_about);
        bSettings = v.findViewById(R.id.action_settings);
    }

    private void assocSensors() {
        mVibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null) {
            Log.w(TAG, "No vibration service exists.");
        }
        mSensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void adsViewsEngine() {
        MobileAds.initialize(requireActivity().getApplicationContext(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(Collections.singletonList("21D4B1EFA1F718FB8D08108FCBCB3114"))
                        .build());

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }


}
