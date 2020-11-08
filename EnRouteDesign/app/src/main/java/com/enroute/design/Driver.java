package com.enroute.design;

import android.graphics.drawable.BitmapDrawable;

class Driver {

    private String name;
    private BitmapDrawable image;
    private boolean onDuty;

    Driver(String name, BitmapDrawable image, boolean onDuty){
        this.name=name;
        this.image=image;
        this.onDuty=onDuty;
    }

    String getName(){
        return name;
    }

    BitmapDrawable getImage() {
        return image;
    }

    boolean onDuty() {
        return onDuty;
    }

    void setOnDuty(boolean online) {
        this.onDuty = online;
    }
}
