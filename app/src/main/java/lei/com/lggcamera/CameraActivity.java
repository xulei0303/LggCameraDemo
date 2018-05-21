package lei.com.lggcamera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;

import lei.com.lggcamera.ListenerUtils.CameraOpendListener;
import lei.com.lggcamera.ListenerUtils.SurfaceCreatedListener;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener, CameraOpendListener, SurfaceCreatedListener {
    private final String TAG = "CameraActivity";

    private MySurfaceView mSurfaceView;
    private Handler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private CameraDevice mCameraDevice;
    private int REQUEST_CAMERA_PERMISSION = 1;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        findViewById(R.id.takePhoto).setOnClickListener(this);
        mSurfaceView = (MySurfaceView) findViewById(R.id.surfaceview);
        startBackgroundThread();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSurfaceView.isAvailable()) {
            openCamera();
        } else {
            mSurfaceView.setSurfaceAvailableListener(this);
        }

    }

    @Override
    public void onSurfaceAvailable() {
        openCamera();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.takePhoto) {
            // TODO

        }
    }

    private void startBackgroundThread() {
        mHandlerThread = new HandlerThread("background-thread");
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());

    }

    private void openCamera() {
        // 检查权限，稍后添加
        if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // no permission!, request permission
            requestCameraPermission();
        }

        CameraManager mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = mCameraManager.getCameraIdList()[0];
            mCameraManager.openCamera(cameraId, mStateCallback, mBackgroundHandler); // 报错是没有检查权限
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestCameraPermission(){
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private MyStateCallback mStateCallback = new MyStateCallback(this);



    private class MyStateCallback extends CameraDevice.StateCallback {
        private CameraOpendListener mListener;

        public MyStateCallback(CameraOpendListener listener) {
            mListener = listener;
        }

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            // 这里可以开始尝试去createCameraSession；
            mListener.isOpend();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            Log.d(TAG, "open camera error = " + error);
            CameraActivity.this.finish();
        }
    }

    @Override
    public void isOpend() {
        // get mCameraDevice!
        CreateCameraSession();
    }

    private void CreateCameraSession() {
        //mSurfaceView.getHolder().setFixedSize(mSurfaceView.getWidth(), mSurfaceView.getHeight());
        Surface surface = mSurfaceView.getHolder().getSurface();
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface), mSessionCallback, mBackgroundHandler);
        } catch (Exception e) {
            Log.d(TAG, "createCaptureSession failed");
            e.printStackTrace();
        }
    }

    private StateCallback mSessionCallback = new StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            // set preview
            if(null == mCameraDevice){
                return;
            }
            mCameraCaptureSession = session;
            setPreView(mCameraCaptureSession);
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            showToast("failed!");
        }
    };

    /**
     * Shows an error message dialog.
     */
    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
        builder.setCancelable(false)
                .setTitle("permission error")
                .setMessage("Need camera permission!")
                .setPositiveButton(null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        CameraActivity.this.finish();
                    }
                })
                .show();
    }

    private void setPreView(CameraCaptureSession session) {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        CaptureRequest captureRequest = mCaptureRequestBuilder.build();
        try {
            session.setRepeatingRequest(captureRequest, mCaptureCallback, mBackgroundHandler);
        } catch (Exception e) {
            showToast("setPreView failed !");
            e.printStackTrace();
        }
    }


    private CaptureCallback mCaptureCallback = new CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };


    private void showToast(final String text) {
        final Activity activity = CameraActivity.this;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
