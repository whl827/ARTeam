/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.helloar.parsing.GeoJsonParser;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer.BlendMode;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


import java.lang.Object;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer, SensorEventListener {
    private static final String TAG = HelloArActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private float scaleFactor;

    private boolean installRequested;

    private Session session;
    private GestureDetector gestureDetector;
    private Snackbar messageSnackbar;
    private DisplayRotationHelper displayRotationHelper;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();

    private final ObjectRenderer pipeObject = new ObjectRenderer();
    private final ObjectRenderer pipObjectShadow = new ObjectRenderer();

    private final ObjectRenderer substationObject = new ObjectRenderer();
    private final ObjectRenderer substationObjectShadow = new ObjectRenderer();

    // Have multiple Objects
    private ArrayList<ObjectRenderer> allObjects = new ArrayList<>();
    private ArrayList<ObjectRenderer> allObjectsShadow = new ArrayList<>();
    private ArrayList<Integer> allObjectModes = new ArrayList<>();

    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloud = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];

    // Tap handling and UI.
    private final ArrayBlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayList<Anchor> anchors = new ArrayList<>();
    private Anchor referenceAnchor;

    // For switching between two objects
    private int mode = 0;

    //counter for creating predefined objects;
    int createdCounter = 0;

    private Button button;
    private TextView locationText;
    private TextView deviceLocationText;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentLocation;
    private double latitude = 0;
    private double longitude = 0;
    private double altitude;

    private File internal;
    private FeatureCollection geoObject = null;
    private GeoJSON geoJSON = null;
    private File geoJsonFile = null;
    private GeoJsonParser geoParsed = null;
    private ArrayList<ArrayList<Float>> allPoints = null;

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float bearing;
    private float azimuth;
    private float direction;
    private float declination;

    float[] mGravity = null;
    float[] mGeomagnetic = null;

    public static final String EXTRA_MESSAGE = "com.helloar.edward.han.MESSAGE";

    private static final String substationStr = "Electric Substation Info\n" +
            "\n" +
            "ELP_ID: 3\n" +
            "ELP_Code 7110\n" +
            "DXF_LAYER: SUBSTATION\n" +
            "DESC: Substation\n" +
            "Shape_Length: 113.03085722\n" +
            "Shape_Area: 157.48189998734222\n";
    private static final String pipeStr = "Replacement Instruction:\n" +
            "\n" +
            "Pressure: 0.6 BAR\n" +
            "Temperature: 59 Celsius\n" +
            "Flow: STRATIFIED\n" +
            "Last Check: 16/05/2017\n" +
            "\n" +
            "1. Locate Pipe\n" +
            "2. Locate Valve\n" +
            "3. Close the valve by turning it clockwise\n" +
            "4. Locate Bleed Valve\n" +
            "5. Empty the pipe by opening the bleed valve\n" +
            "6. Locate the pressure indicating transmitter\n" +
            "7. Remove the malfunction instrument\n" +
            "8. Put the new instrument in place\n" +
            "9. Close the bleed valve\n" +
            "10. Reopen the valve";

    private ArrayList<String> infoArray;

    // added on 4/4/18
    //private final Pose mCameraRelativePose = Pose.makeTranslation(0.01f, -0.01f, -0.6f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up tap listener.
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                onSingleTap(e);
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });

        surfaceView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return gestureDetector.onTouchEvent(event);
                    }
                });

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        //instantiate allpoints
        allPoints = new ArrayList<ArrayList<Float>>();

        installRequested = false;

        button = findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeObjects();
            }
        });

        locationText = findViewById(R.id.locationText);
        deviceLocationText = findViewById(R.id.deviceLocationText);

        currentLocation = new Location("");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                altitude = location.getAltitude();
                String msg = "Latitude: " + latitude + "\n" +
                             "Longitude: " + longitude;
                deviceLocationText.setText(msg);
                currentLocation.setLatitude(latitude);
                currentLocation.setLongitude(longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider)
            {

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]
                    {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET
                    }, 10);
           // locationManager.requestLocationUpdates("gps", 1, 0, locationListener);

            return;
        }
        locationManager.requestLocationUpdates("gps", 1, 0, locationListener);

        try {
            internal = Environment.getExternalStorageDirectory();
            geoJsonFile = new File(internal, "michelson.geojson");
            geoParsed = new GeoJsonParser(this, "michelson.geojson");
            Log.d(TAG, "onCreate: file success " + geoParsed.getGeoJson().getType().toString());
            geoObject = (FeatureCollection) geoParsed.getGeoJson();
            Log.d(TAG, "geoJson to Feature -- " + geoObject.getFeatures().get(0).getGeometry().toJSON().getString("coordiantes"));
        } catch (Exception e) {

        } finally {
            allPoints = geoParsed.getCoordinates(0);
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//            @Override
//            public void onAccuracyChanged(Sensor sensor, int accuracy) {
//                // TODO Auto-generated method stub
//
//            }
//        };

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    //Sensor Listener Function
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = event.values;
            Log.d(TAG, "bearing:! sensor type: accelerometer");
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mGeomagnetic = event.values;
            Log.d(TAG, "bearing:! On sensor type is magnetic field");
        }
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientationData[] = new float[3];
                SensorManager.getOrientation(R, orientationData);
                azimuth = orientationData[0];
                // now how to use previous 3 values to calculate orientation
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                showSnackbarMessage(message, true);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.
            Config config = new Config(session);
            if (!session.isSupported(config)) {
                showSnackbarMessage("This device does not support AR", true);
            }
            session.configure(config);
        }

        showLoadingMessage();
        // Note that order matters - see the note in onPause(), the reverse applies here.
        session.resume();
        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
        switch (requestCode)
        {
            case 10:
                if(results.length>0 && results[0] == PackageManager.PERMISSION_GRANTED)
                {
//                    locationManager.requestLocationUpdates("gps", 1000, 0, locationListener);
                }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        queuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        backgroundRenderer.createOnGlThread(/*context=*/ this);

        // Prepare the other rendering objects.
        try {
            virtualObject.createOnGlThread(/*context=*/ this, "SubstationBig.obj", "SubstationBig.png");
            virtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            virtualObjectShadow.createOnGlThread(/*context=*/ this, "andy_shadow.obj", "andy_shadow.png");
            virtualObjectShadow.setBlendMode(BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            allObjects.add(virtualObject);
            allObjectsShadow.add((virtualObjectShadow));

            //pipe
            pipeObject.createOnGlThread(/*context=*/ this, "Tee.obj", "Tee.png");
            pipeObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            pipObjectShadow.createOnGlThread(/*context=*/ this, "andy_shadow.obj", "andy_shadow.png");
            pipObjectShadow.setBlendMode(BlendMode.Shadow);
            pipObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            allObjects.add(pipeObject);
            allObjectsShadow.add(pipObjectShadow);

//            //substation
//            substationObject.createOnGlThread(/*context=*/ this, "SubstationBig.obj", "SubstationBig.png");
//            substationObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
//
//            substationObjectShadow.createOnGlThread(/*context=*/ this, "andy_shadow.obj", "andy_shadow.png");
//            substationObjectShadow.setBlendMode(BlendMode.Shadow);
//            substationObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);
//
//            allObjects.add(substationObject);
//            allObjectsShadow.add(substationObjectShadow);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            planeRenderer.createOnGlThread(/*context=*/ this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        pointCloud.createOnGlThread(/*context=*/ this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.

            MotionEvent tap = queuedSingleTaps.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {

                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();
                    // Creates an anchor if a plane or an oriented point was hit.
                    if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                            || (trackable instanceof Point
                            && ((Point) trackable).getOrientationMode()
                            == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {

                        // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.

                        //check if object exists on screen
                        double cur_tx = hit.getHitPose().tx();
                        double cur_ty = hit.getHitPose().ty();
                        double cur_tz = hit.getHitPose().tz();
                        boolean exist = false;
                        for(int i=0; i<anchors.size(); i++)
                        {

                            Anchor anc = anchors.get(i);

                            double temp_tx = Math.round(anc.getPose().tx() * 1000.0) / 1000.0;
                            double temp_ty = Math.round(anc.getPose().ty() * 1000.0) / 1000.0;
                            double temp_tz = Math.round(anc.getPose().tz() * 1000.0) / 1000.0;

                            //TODO: change comparison value

                            Log.d(TAG, "!CHECK: OBJECT POSE: " + temp_ty + ", " + temp_tz + "\n");
                            Log.d(TAG, "!CHECK: TAPPED POSE: " + cur_ty + ", " + cur_tz + "\n");

                            // 0.09 <= value <= 1.0 ; bigger values make "hit-frame" bigger
                            if( (Math.abs(cur_tx - temp_tx) < 0.25) &&
                            (Math.abs(cur_ty - temp_ty) < 0.25) &&
                                    (Math.abs(cur_tz - temp_tz) < 0.25)) {
                                Log.d(TAG, "!CHECK: OBJECT EXISTS");
                                exist = true;
                                displayInfo(i);
                            }
                        }
                        if(exist) { break; }

                        // add an anchor at the tapped position
                        if (anchors.size() >= 20) {
                            anchors.get(0).detach();
                            anchors.remove(0);
                            allObjectModes.remove(0);
                        }
                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor is created on the Plane to place the 3D model
                        // in the correct position relative both to the world and to the plane.
                        anchors.add(hit.createAnchor());
                        allObjectModes.add(mode);

                        break;
                    }
                }
            }

            // Draw background.
            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            this.pointCloud.update(pointCloud);
            this.pointCloud.draw(viewmtx, projmtx);

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (messageSnackbar != null) {
                for (Plane plane : session.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                        break;
                    }
                }
            }

            // Visualize planes.
            planeRenderer.drawPlanes(
                    session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            // create anchors not by touch (from file)
            double lat0 = 0, lon0 = 0, altitude = 0;
//            if (referenceAnchor == null && latitude != 0 && longitude != 0) {
//                // get coordinates from file
//                lat0 = allPoints.get(0).get(1); //geojson long/lat
//                lon0 = allPoints.get(0).get(0);
//
//                Log.d(TAG, "!!!LAT: " + lat0 + "LONG: " +lon0);
//
//                referenceAnchor = session.createAnchor(getPoseFromCoordinates(frame, lat0, lon0, altitude));
//
//                //placing predefined object
//                if (referenceAnchor != null) {
//                    anchors.add(referenceAnchor);
//                    allObjectModes.add(mode);
//                }
//            }

            if(createdCounter < allPoints.size() && latitude != 0 && longitude != 0)
            {
                for(int index=0; index<allPoints.size(); index++)
                {
                    Anchor current_anchor;
                    double cur_lat = allPoints.get(index).get(1);
                    double cur_lon = allPoints.get(index).get(0);
                    current_anchor = session.createAnchor(getPoseFromCoordinates(frame, cur_lat, cur_lon, altitude-1));
                    if (current_anchor != null)
                    {
                        anchors.add(current_anchor);
                        allObjectModes.add(mode); //change mode
                        createdCounter++;
                    }
                }
            }
            Log.d(TAG, "ANCHOR SIZE: " + anchors.size());

            // Display orientation from true north to let the user calibrate.
            // convert radians to degrees
            azimuth = (float)Math.toDegrees(azimuth);
            GeomagneticField geoField = new GeomagneticField(
                    (float) latitude,
                    (float) longitude,
                    (float) altitude,
                    System.currentTimeMillis());
            //azimuth += geoField.getDeclination(); // converts magnetic north to true north
            declination = geoField.getDeclination(); // the difference angle of magnetic north from true north
//            if(azimuth < 0){
//                azimuth += 360;
//            }

            Location targetLoc = new Location("");
            targetLoc.setLatitude(lat0);
            targetLoc.setLongitude(lon0);

            bearing = currentLocation.bearingTo(targetLoc); // (it's already in degrees)

            Log.d(TAG, "bearing: " + bearing + "azimuth: " + azimuth);

            direction = azimuth - bearing;

            // Visualize anchors.
            scaleFactor = 1.0f;
            for (int k = 0; k < anchors.size(); k++) {
                if (anchors.get(k).getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                anchors.get(k).getPose().toMatrix(anchorMatrix, 0);

//                Matrix.rotateM(anchorMatrix, 0, 90, 0f, 1f, 0f);

//                anchorMatrix[13] = anchorMatrix[13]-(0.5f);

                // Update and draw the model and its shadow.
                if (allObjectModes.get(k).intValue() == 1) { //pipe
                    scaleFactor = 0.0125f;
                    allObjects.get(1).updateModelMatrix(anchorMatrix, scaleFactor);
                    allObjectsShadow.get(1).updateModelMatrix(anchorMatrix, scaleFactor);
                    allObjects.get(1).draw(viewmtx, projmtx, lightIntensity);
                    allObjectsShadow.get(1).draw(viewmtx, projmtx, lightIntensity);
                } else {
                    scaleFactor = 0.125f; //substation
                    allObjects.get(0).updateModelMatrix(anchorMatrix, scaleFactor);
                    allObjectsShadow.get(0).updateModelMatrix(anchorMatrix, scaleFactor);
                    allObjects.get(0).draw(viewmtx, projmtx, lightIntensity);
                    allObjectsShadow.get(0).draw(viewmtx, projmtx, lightIntensity);
                }
            }

            // ***** GETTING THE LATITUDE/LONGITUDE OF AN OBJECT PLACED ON THE AR PLANE *****

            if (anchors.size() > 0) {
                Location loc1 = new Location("");
                loc1.setLatitude(latitude);
                loc1.setLongitude(longitude);

                Location loc2 = getCoordinatesFromPose(anchors.get(anchors.size() - 1).getPose());

                float distanceInMeters = loc1.distanceTo(loc2);

                // Commented below to show functionality for pre-defined/loaded objects
                // Uncomment below, and comment code about reference anchors above (lines 685-700)
                //      to show object placement functionality

//                String msg = "Object Latitude: " + loc1.getLatitude() + "\nObject Longitude: " + loc1.getLongitude();
//                msg += "\n\nDistance from Device to Object: " + distanceInMeters + " meters";

//                locationText.setText(msg);

                Log.d(TAG, "TESTING : ANCHOR SIZE: " + anchors.size());

                Log.d(TAG, "RESULT: \n" + distanceInMeters);

            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        messageSnackbar =
                Snackbar.make(
                        HelloArActivity.this.findViewById(android.R.id.content),
                        message,
                        Snackbar.LENGTH_INDEFINITE);
        messageSnackbar.getView().setBackgroundColor(0xbf323232);
        if (finishOnDismiss) {
            messageSnackbar.setAction(
                    "Dismiss",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            messageSnackbar.dismiss();
                        }
                    });
            messageSnackbar.addCallback(
                    new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            finish();
                        }
                    });
        }
        messageSnackbar.show();
    }

    private Location getCoordinatesFromPose(Pose pose) {
        double dn, de, dLat, dLon, lat0, lon0;
        double radius = 6378137; // for calculating coordinates    
        Location loc;

        // calculate offset
        dn = pose.tx();
        de = pose.tz();

        Log.d(TAG, "TESTING2: " + dn + ", " + de);

        // Coordinate offsets in radians
        dLat = dn / radius;
        dLon = de / (radius * Math.cos(Math.PI * latitude / 180));

        // Offset Position, decimal degrees
        lat0 = latitude + dLat * 180 / Math.PI;
        lon0 = longitude + dLon * 180 / Math.PI;

        Log.d(TAG, "ANCHOR HIT POSE: Lat: " + lat0 + " Lon: " + lon0);

        loc = new Location("");
        loc.setLatitude(lat0);
        loc.setLongitude(lon0);

        return loc;
    }

    private Pose getPoseFromCoordinates(Frame frame, double lat0, double lon0, double altitude) {
        double latDif, lonDif, latDifRad, lonDifRad, offN, offE;
        double radius = 6378137; // for calculating coordinates
        Pose mPose;

        if (latitude == 0 || longitude == 0) return null;

        // calculate where to place the anchor
        latDif = lat0 - latitude;
        lonDif = lon0 - longitude;

        Log.d(TAG, "TESTING : latDif: " + latDif + "lonDif: " + lonDif);

        latDifRad = latDif * Math.PI/180;
        lonDifRad = lonDif * Math.PI/180;

        // offsets in meters
        offN = latDifRad * radius;
        offE = lonDifRad * (radius * Math.cos(Math.PI * latitude / 180));

        Log.d(TAG, "TESTING : " + offN + ", " + offE);

        // create anchor
        mPose = Pose.makeTranslation(
                (float)offN + frame.getCamera().getPose().tx(),
                (float)altitude,
                (float)offE + frame.getCamera().getPose().tz());

        // TODO re-orient anchor wrt true north

        Log.d(TAG, "TESTING REFERENCHE: " + mPose.toString());

        return mPose;
    }

    private void showLoadingMessage() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showSnackbarMessage("Searching for surfaces...", false);

                    }
                });
    }

    private void displayInfo(int i) {

        final int info_index=  i;
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(HelloArActivity.this, InfoActivity.class);
                        String message = null;
                        if(info_index >= allObjectModes.size())
                        {
                            message = "NO INFO AVAILABLE";
                            Log.d(TAG, "!@ size: " + allObjectModes.size() + " Index: " + info_index);
                            Log.d(TAG, "!@ anchor size: " + anchors.size());
                        }
                        else
                        {
                            switch(allObjectModes.get(info_index)) {
                                case 0:
                                    message = substationStr;
                                    break;
                                case 1:
                                    message = pipeStr;
                                    break;
                            }
                        }

                        intent.putExtra(EXTRA_MESSAGE, message);
                        startActivityForResult(intent,1);
                        Toast.makeText(HelloArActivity.this, "testing toast", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void hideLoadingMessage() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (messageSnackbar != null) {
                            messageSnackbar.dismiss();
                        }
                        messageSnackbar = null;
                    }
                });
    }

    private void changeObjects() {
        if (mode == 0) {
            button.setText("Pipe");
            mode = 1;
        } else {
            button.setText("Substation");
            mode = 0;
        }
    }

    private int getMode() {
        return mode;
    }

}
