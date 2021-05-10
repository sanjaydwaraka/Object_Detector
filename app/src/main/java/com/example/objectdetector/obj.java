package com.example.objectdetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class obj extends AppCompatActivity {
    private static final int MY_CAMERA_PERMISSION_CODE = 100;//permission
    PreviewView cam;//variable for view element
    ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture; //for listening continuously
    ObjectDetector brain;
    private LocalModel localModel;
    private CustomObjectDetectorOptions optionss;
    PreviewView previewView;
    TextView objectIdentified;
    //drawing to layout declaration
    RelativeLayout parent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obj);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        mapview();
        checkCameraPermission();

        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(() -> {
            //link camera provider to cam id
            try {
                bindcamcamera(cameraProviderListenableFuture.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this)); //where to execute
        //load model
        localModel = new LocalModel.Builder()
                .setAssetFilePath("objdetect.tflite")
                .build();
        //configure model
        optionss = new CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.6f)
                .build();
        //combine and create model
        brain = ObjectDetection.getClient(optionss);
        previewView = findViewById(R.id.cam);
        parent = findViewById(R.id.parentLayout);
        objectIdentified = findViewById(R.id.objectsResult);
    }


    //FUNCTIONS

    private void bindcamcamera(ProcessCameraProvider CameraProvider) {
        Preview bulider = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        bulider.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        //actions
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void analyze(@NonNull ImageProxy image) {
                int rotdigree = image.getImageInfo().getRotationDegrees();
                Image fimage = image.getImage();
                if (fimage != null) {
                    InputImage preprocessedimage = InputImage.fromMediaImage(fimage, rotdigree);
                    //on Sucess AND on failure
                    brain.process(preprocessedimage)
                            .addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
                                @Override
                                public void onSuccess(List<DetectedObject> detectedObjects) {
                                    while (parent.getChildCount() > 6){
                                        objectIdentified.setText("");
                                        parent.removeViewAt(6);
                                    }
                                    //plot result
                                    for (DetectedObject detectedObject : detectedObjects) {
                                        String nameofobj = detectedObject.getLabels().get(0).getText();
                                        Rect rect = scaleBoundingBox(detectedObject.getBoundingBox(), fimage);
                                        objectIdentified.setText(objectIdentified.getText() + nameofobj + ", ");
                                        imagetodraw imtodraw = new imagetodraw(getApplicationContext(), nameofobj, rect);
                                        parent.addView(imtodraw);
                                    }
                                    image.close();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("ObjectDetectionError", e.toString());
                                    image.close();
                                }
                            });

                }

            }
        });
        CameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, bulider);

    }


    private void mapview() {
        cam = findViewById(R.id.cam);
        parent = findViewById(R.id.parentLayout);
    }

    //permission
    boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(obj.this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        } else {
            return true;
        }
        return false;
    }
    private Rect scaleBoundingBox(Rect boundingBox, Image fimage) {
        float scaleY = previewView.getHeight() / fimage.getHeight();
        float scaleX = previewView.getWidth() / fimage.getWidth();
        float scale = Math.max(scaleY, scaleX);
        Size scaledSize = new Size((int) Math.ceil(fimage.getWidth() * scale), (int) Math.ceil(fimage.getHeight() * scale));

        float offsetX = previewView.getWidth() - scaledSize.getWidth() / 2;
        float offsetY = previewView.getHeight() - scaledSize.getHeight() / 2;

        boundingBox.left = (int) (boundingBox.left * scale + offsetX);
        boundingBox.top = (int) (boundingBox.top * scale + offsetY);
        boundingBox.right = (int) (boundingBox.right * scale + offsetX);
        boundingBox.bottom = (int) (boundingBox.bottom * scale + offsetY);
        return boundingBox;
    }
}