package com.enroute.lasttake;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

public class Global extends Application {

    private String userID,businessID,accountType,name,colorBlind;

    private int[] ids={R.drawable.ic_accept_order_background,R.drawable.ic_cancel_order_background,R.drawable.ic_blue_button_bg};

    private GlobalDataChangedListener globalDataChangedListener;

    private static Global global;

    public static Context getContext(){
        return global.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        global=this;
    }

    public void clear(){
        userID=null;
        businessID=null;
        accountType=null;
        name=null;
    }

    public void setColorBlind(String colorBlind) {
        this.colorBlind = colorBlind;
        int greenTint,redTint,blueTint;
        if(colorBlind.equals("normal")){
            greenTint=getContext().getResources().getColor(R.color.greenTextNormal);
            redTint=getContext().getResources().getColor(R.color.redTextNormal);
            blueTint=getContext().getResources().getColor(R.color.blueBgNormal);
        }else if(colorBlind.equals("prot")){
            greenTint=getContext().getResources().getColor(R.color.greenTextProt);
            redTint=getContext().getResources().getColor(R.color.redTextProt);
            blueTint=getContext().getResources().getColor(R.color.blueBgProt);
        }else{
            greenTint=getContext().getResources().getColor(R.color.greenTextDeut);
            redTint=getContext().getResources().getColor(R.color.redTextDeut);
            blueTint=getContext().getResources().getColor(R.color.blueBgDeut);
        }
        DrawableCompat.setTint(DrawableCompat.wrap(AppCompatResources.getDrawable(getContext(),ids[0])),greenTint);
        DrawableCompat.setTint(DrawableCompat.wrap(AppCompatResources.getDrawable(getContext(),ids[1])),redTint);
        DrawableCompat.setTint(DrawableCompat.wrap(AppCompatResources.getDrawable(getContext(),ids[2])),blueTint);
    }

    public String getColorBlind() {
        return colorBlind;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
        if(globalDataChangedListener!=null)
            globalDataChangedListener.DataChanged(userID,businessID,accountType,name);
    }

    public String getBusinessID() {
        return businessID;
    }

    public void setBusinessID(String businessID) {
        this.businessID = businessID;
        if(globalDataChangedListener!=null)
            globalDataChangedListener.DataChanged(userID,businessID,accountType,name);
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
        if(globalDataChangedListener!=null)
            globalDataChangedListener.DataChanged(userID,businessID,accountType,name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if(globalDataChangedListener!=null)
            globalDataChangedListener.DataChanged(userID,businessID,accountType,name);
    }

    public void setGlobalDataChangedListener(GlobalDataChangedListener globalDataChangedListener) {
        this.globalDataChangedListener = globalDataChangedListener;
    }
}

interface GlobalDataChangedListener {
    void DataChanged(String userID,String businessID,String accountType,String name);
}
