package com.example.speedometer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {
    TextView speedText,distanceText,timeText;
    SwitchCompat switchMetricUnit;
    MaterialButton startPauseBtn;
    protected static final int REQUEST_CHECK_SETTINGS = 99;
//    LocationManager locationManager;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location startLocation,endLocation;
    ProgressDialog progressDialog;
    long startTime,endTime;
    float distanceCovered;
    FusedLocationProviderClient mProviderClient;
    com.google.android.gms.location.LocationCallback locationCallback=new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            onLocationChanged(locationResult.getLastLocation());
        }
    };
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedText=findViewById(R.id.speed);
        distanceText=findViewById(R.id.distance);
        timeText=findViewById(R.id.time);
        switchMetricUnit=findViewById(R.id.switch_metric_unit);
        startPauseBtn=findViewById(R.id.start_pause_btn);

        startPauseBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if(startPauseBtn.getText().equals("START")){
                    //Checking operating system
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        checkLocationPermission();
                    else
                        buildGoogleApiClient();
                    startPauseBtn.setText("STOP");
                }else{
                    disConnect();
                    speedText.setText("Speed: ");
                    distanceText.setText("Distance: ");
                    timeText.setText("Time: ");
                    startPauseBtn.setText("START");
                }
            }
        });
    }
//    @SuppressLint("MissingPermission")
//    private void doStuff() {
//        locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
//        if(locationManager!=null){
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,
//                    (android.location.LocationListener) this);
//        }
//        Toast.makeText(this, "Waiting for GPS Connection!", Toast.LENGTH_SHORT).show();
//    }
    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationChanged(Location location) {
        if(location!=null){
            progressDialog.dismiss();
            if(startLocation==null) {
                startLocation = endLocation = location;
                startTime=System.currentTimeMillis();
                endTime=0;
            }
            else
                endLocation=location;
            endTime=System.currentTimeMillis();
            long diffTime=endTime-startTime;
            Calendar c= Calendar.getInstance();
            c.set(Calendar.HOUR,0);
            c.set(Calendar.MINUTE,0);
            c.set(Calendar.SECOND,0);
            c.set(Calendar.MILLISECOND, (int) diffTime);
            distanceCovered=startLocation.distanceTo(endLocation);

            float speed=location.getSpeed();
            if(switchMetricUnit.isChecked()) {
                speedText.setText("Speed: "+new DecimalFormat("#.###").format(speed)+" m/s");
                distanceText.setText("Distance: "+new DecimalFormat("#.###").format(distanceCovered) +" m");
            }
            else{
                speed*=3.6f;
                distanceCovered/=1000.00f;
                speedText.setText("Speed: "+new DecimalFormat("#.###").format(speed)+" km/h");
                distanceText.setText("Distance: "+new DecimalFormat("#.###").format(distanceCovered) +" km");
            }

            timeText.setText("Time: "+String.format(Locale.getDefault(),"%02d:%02d:%02d",c.get(Calendar.HOUR),
                    c.get(Calendar.MINUTE),c.get(Calendar.SECOND)));
            Toast.makeText(this, "Latitude: "+endLocation.getLatitude()+", Longitude: "+endLocation.getLongitude(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient =new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @SuppressLint("VisibleForTests")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(500);  //After how much seconds u want to update location
        mLocationRequest.setFastestInterval(500); //update location in background.
        gpsDialog();
        showProgressDialog();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            //start updating user current location
             mProviderClient = new FusedLocationProviderClient(this);
            mProviderClient.requestLocationUpdates(mLocationRequest, locationCallback,getMainLooper());
        }
    }

    private void showProgressDialog() {
        progressDialog =new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Getting Location");
        progressDialog.show();
    }

    private void gpsDialog() {
        LocationSettingsRequest.Builder builder=new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        Task<LocationSettingsResponse> responseTask=LocationServices
                .getSettingsClient(getApplicationContext()).checkLocationSettings(builder.build());
        responseTask.addOnCompleteListener(task -> {
            try{
                LocationSettingsResponse settingsResponse=task.getResult(ApiException.class);
            } catch (ApiException e) {
                if(e.getStatusCode()== LocationSettingsStatusCodes.RESOLUTION_REQUIRED){
                    ResolvableApiException apiException =(ResolvableApiException) e;
                    try{
                        apiException.startResolutionForResult(MainActivity.this,REQUEST_CHECK_SETTINGS);
                    }catch (IntentSender.SendIntentException intentException){
                        Log.i("tag","PendingIntent unable to execute request.");
                    }
                }
                if(e.getStatusCode()==LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE){
                    Toast.makeText(MainActivity.this, "Settings not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private boolean useMetricUnits() {
        return switchMetricUnit.isChecked();
    }

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            //Asking user when explanation is needed
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                //Show an explanation to the user
                //this thread wait for the user's response! after the user sees the explanation, try again to request the permission
                //Prompt the user once the explanation has been shown.
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }else{
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }else if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            buildGoogleApiClient();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_LOCATION:{
                //if request is empty, the result arrays are empty.
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //Permission was granted. Do the contacts related task you need to do
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        buildGoogleApiClient();
                    }
                }else{
//                    Permission denied, disable the functionality that depends on this permission
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        disConnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disConnect();
    }

    private void disConnect(){
        if(mGoogleApiClient!=null)
            mGoogleApiClient.disconnect();
        mProviderClient.removeLocationUpdates(locationCallback);
        startLocation=null;
        distanceCovered=0;
        startTime=0;
        endTime=0;
    }
}