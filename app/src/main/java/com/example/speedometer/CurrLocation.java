package com.example.speedometer;

import android.location.Location;

public class CurrLocation extends Location {
    private boolean useMetricUnit =false;
    public CurrLocation(Location l) {
        this(l,true);
    }
    public CurrLocation(Location l,boolean useMetricUnit) {
        super(l);
        this.useMetricUnit = useMetricUnit;
    }
    public void setUseMetricUnit(boolean useMetricUnit){
        this.useMetricUnit=useMetricUnit;
    }

    public boolean isUseMetricUnit() {
        return useMetricUnit;
    }

    @Override
    public float distanceTo(Location dest) {
        float mDistance= super.distanceTo(dest);
        if(!isUseMetricUnit()){
//            convert meters to feet
            mDistance*=3.28084f;
        }
        return mDistance;
    }
    @Override
    public double getAltitude() {
        double mAltitude= super.getAltitude();
        if(!this.isUseMetricUnit()){
//            convert meters to feet
            mAltitude*=3.28084d;
        }
        return mAltitude;
    }

    @Override
    public float getSpeed() {
        float mSpeed= super.getSpeed();
        if(!this.isUseMetricUnit()){
//            convert meters/second to miles/hour
            mSpeed*=2.23694;
        }
        return mSpeed;
    }

    @Override
    public float getAccuracy() {
        float mAccuracy= super.getAccuracy();
        if(!this.isUseMetricUnit()){
//            convert meters to feets
            mAccuracy*=3.28084;
        }
        return mAccuracy;
    }
}
