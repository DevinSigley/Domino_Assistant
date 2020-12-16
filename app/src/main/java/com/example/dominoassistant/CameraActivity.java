/*package com.example.dominoassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
    }
*/
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
/*    public native String stringFromJNI();
}

 */

package com.example.dominoassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1;
    private boolean userStopped = false;
    private String dominoesString = "";
    private String newDominoes;
    private String intentString;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");

                // Load native library after(!) OpenCV initialization
                System.loadLibrary("native-lib");

                Button button = findViewById(R.id.startStopCameraButton);
                if (userStopped){
                    //mOpenCvCameraView.enableView();
                    userStopped = false;
                    startStopCamera();
                }
//                if (button.getText().equals(getResources().getString(R.string.resume_camera))) {
//                    startStopCamera();
//                    //mOpenCvCameraView.enableView();
//                }
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setTitle("Visual Domino Selection");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Permissions for Android 6+
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST
        );

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = findViewById(R.id.main_surface);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        intentString = getIntent().getStringExtra("dominoesString");
        if (intentString != null){
            dominoesString = intentString;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mOpenCvCameraView.setCameraPermissionGranted();
            } else {
                String message = "Camera permission was not granted";
                Log.e(TAG, message);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(TAG, "Unexpected permission request");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null){
            if (!userStopped){
                startStopCamera();
            }
//            Button button = findViewById(R.id.startStopCameraButton);
//            if (button.getText().equals(getResources().getString(R.string.pause_camera))) {
//
//            }
            //mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        // get current camera frame as OpenCV Mat object
        //Mat mat = frame.gray();
        Mat mat = frame.rgba();

        // native call to process current camera frame
        //dominoesString = processImageFromJNI(mat.getNativeObjAddr());
        newDominoes = processImageFromJNI(mat.getNativeObjAddr());

        Log.d(TAG, dominoesString);
        //ArrayList<Domino> dominos = Domino.decodeDominoes(dominoesString);
        // return processed frame for live preview
        return mat;
    }

    private native String processImageFromJNI(long mat);

    public void stopCamera(View view){
        mOpenCvCameraView.disableView();
        //Log.d(TAG, "Pressed stopCamera");
    }
    public void startCamera(View view){
        mOpenCvCameraView.enableView();
        //Log.d(TAG, "Pressed startCamera");
    }

    private void startStopCamera(){
        Button button = findViewById(R.id.startStopCameraButton);
        Button captureDominoesButton = findViewById(R.id.captureDominoesButton);
        if (button.getText().equals(getResources().getString(R.string.pause_camera))){
            mOpenCvCameraView.disableView();
            button.setText(getResources().getString(R.string.resume_camera));
            //button.setBackgroundColor(Color.parseColor("#4CAF50"));
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            captureDominoesButton.setVisibility(View.VISIBLE);
        }
        else {
            mOpenCvCameraView.enableView();
            button.setText(getResources().getString(R.string.pause_camera));
            //button.setBackgroundColor(Color.parseColor("#F44336"));
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            captureDominoesButton.setVisibility(View.INVISIBLE);
        }
    }

    public void startStopCamera(View view){
        Button button = findViewById(R.id.startStopCameraButton);
        if (button.getText().equals(getResources().getString(R.string.pause_camera))){
            userStopped = true;
        }
        else {
            userStopped = false;
        }
        startStopCamera();
    }

    public void captureDominoes(View view) {
        if (intentString != null){
            dominoesString = dominoesString.concat(newDominoes);
        }
        else {
            dominoesString = newDominoes;
        }
        Log.d(TAG, dominoesString);
        Intent intent = new Intent(getBaseContext(), SelectDominoesActivity.class);
        intent.putExtra("dominoesString", dominoesString);
        startActivity(intent);
        finish();
    }
}