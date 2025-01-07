package com.android.settings.wifi.qrcode;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.util.Log;
import android.view.View;
import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraMetadata;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.camera2.interop.Camera2CameraControl;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.CaptureRequestOptions;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.NotFoundException;


import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.concurrent.Executor;

public class QrCamera {
    private static final String TAG = "QrCamera";

    private final Context mContext;
    private final ScannerCallback mScannerCallback;
    private final ExecutorService mCameraExecutor;
    private final MultiFormatReader mReader;
    private Camera mCamera;
    private WeakReference<PreviewView> cameraPreview;
    @SuppressLint("UnsafeOptInUsageError")
    private Camera2CameraControl camera2Control;

    public QrCamera(Context context, ScannerCallback callback) {
        mContext = context;
        mScannerCallback = callback;
        mCameraExecutor = Executors.newSingleThreadExecutor();
        mReader = new MultiFormatReader();
    }

    public void start(LifecycleOwner lifecycleOwner, View view) {
        Log.d(TAG, "start QR Camera");
        cameraPreview = new WeakReference<>((PreviewView) view);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        Log.d(TAG, "start QR Camera: get cameraProviderFuture completed: " + (cameraProviderFuture == null));
        cameraProviderFuture.addListener(() -> {
            try {
                Log.d(TAG, "start QR Camera: get cameraProvider");
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Log.d(TAG, "start QR Camera: get cameraProvider completed: " + (cameraProvider == null));
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                Log.d(TAG, "start QR Camera: setSurfaceProvider");
                Size resolution = new Size(1920, 1080);
                Preview preview = new Preview.Builder().setTargetResolution(resolution).build();
                preview.setSurfaceProvider(cameraPreview.get().getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(resolution)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                Log.d(TAG, "start QR Camera: setAnalyzer");
                imageAnalysis.setAnalyzer(mCameraExecutor, image -> {
                    try {
                        Log.d(TAG, "Analyzer");
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);

                        int width = image.getWidth();
                        int height = image.getHeight();
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                                new QrYuvLuminanceSource(data, width, height)));

                        Log.d(TAG, "Analyzer: get bitmap");
                        Result result = mReader.decodeWithState(bitmap);
                        Log.d(TAG, "Analyzer:decodeWithState: " + (result == null));
                        if (result != null && mScannerCallback.isValid(result.getText())) {
                            Log.d(TAG, "Analyzer:decodeWithState result: " + result.getText());
                            mScannerCallback.handleSuccessfulResult(result.getText());
                        }
                    } catch (NotFoundException e) {
                        Log.d(TAG, "No QR code found in the image.");
                    } catch (Exception e) {
                        Log.d(TAG, "Analyzer ex; " + e);
                    }
                    finally {
                        image.close();
                        mReader.reset();
                        Log.d(TAG, "Analyzer close");
                    }
                });

                Log.d(TAG, "start QR Camera: unbindAll");
                cameraProvider.unbindAll();

                mCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );
                Executor executor = Executors.newSingleThreadExecutor();
                camera2Control = Camera2CameraControl.from(mCamera.getCameraControl());
                CaptureRequestOptions options = new CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                        .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, (int) 300)
                        .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) 40000000)
                        .build();
                ListenableFuture<Void> future = camera2Control.addCaptureRequestOptions(options);
                Futures.addCallback(future, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Success addCaptureOptions");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.d(TAG, "Error addCaptureOptions: " + t.getMessage());
                    }
                }, executor);
                Log.d(TAG, "start QR Camera: finished");
            } catch (Exception e) {
                Log.d(TAG, "start QR Camera: handleCameraFailure");
                mScannerCallback.handleCameraFailure();
            }
        }, ContextCompat.getMainExecutor(mContext));
    }

    public void stop() {
        if (mCameraExecutor != null) {
            mCameraExecutor.shutdown();
        }
        if (cameraPreview != null)
            cameraPreview.clear();
        cameraPreview = null;
    }

    public interface ScannerCallback {
        void handleSuccessfulResult(String result);
        void handleCameraFailure();
        Size getViewSize();
        Rect getFramePosition(Size previewSize, int cameraOrientation);
        void setTransform(Matrix transform);
        boolean isValid(String qrCode);
    }

    public boolean isDecodeTaskAlive() {
        return false;
    }
}
