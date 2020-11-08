package com.enroute.lasttake;

import android.content.Context;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.Exclude;

class Driver {

    public String name,id;
    public DriverLatLng location;
    public DeliveryOrder currentOrder;
    @Exclude
    private MarkerOptions marker;
    private Context context;

    Driver(String name, String id, DriverLatLng location, DeliveryOrder currentOrder){
        this.name=name;
        this.id=id;
        this.location=location;
        this.currentOrder=currentOrder;
        context=Global.getContext();
        MapsInitializer.initialize(context);
        marker=new MarkerOptions()
            .position(location.getLatLng())
            .title(name)
            .draggable(false)
            .anchor(.5f,.5f)
            .flat(true);
    }

    Driver(){
        context=Global.getContext();
        MapsInitializer.initialize(context);
        currentOrder=new DeliveryOrder();
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setLocation(double latitude, double longitude) {
        location.setLocation(latitude, longitude);
        marker.position(location.getLatLng());
    }

    DeliveryOrder getCurrentOrder() {
        return currentOrder;
    }

    void setCurrentOrder(DeliveryOrder currentOrder) {
        this.currentOrder = currentOrder;
    }

    @Exclude
    private void setMapObject(MarkerOptions marker){
        this.marker=marker;
    }

    @Exclude
    MarkerOptions getMapObject(){
        BitmapDescriptor icon;
        icon=BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(context.getResources(),R.drawable.driver_on_order));
        if(marker==null) {
            setMapObject(new MarkerOptions()
                    .position(location.getLatLng())
                    .title(name)
                    .icon(icon)
                    .draggable(false)
                    .snippet(getCurrentOrder() == null ? null : getCurrentOrder().getOrderID())
                    .anchor(.5f, .5f)
                    .flat(true));
        }
        return marker.icon(icon);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(this==obj)
            return true;
        if(obj==null || obj.getClass()!=getClass())
            return false;
        Driver other=(Driver) obj;
        return this.getId().equals(other.getId());
    }
}

class DriverLatLng{
    public double latitude,longitude;

    public DriverLatLng(){}

    DriverLatLng(double latitude,double longitude){
        this.latitude=latitude;
        this.longitude=longitude;
    }

    @Exclude
    LatLng getLatLng(){
        return new LatLng(latitude,longitude);
    }

    void setLocation(double latitude,double longitude){
        this.latitude=latitude;
        this.longitude=longitude;
    }
}
