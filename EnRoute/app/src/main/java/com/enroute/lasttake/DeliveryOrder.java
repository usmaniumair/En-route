package com.enroute.lasttake;

import androidx.annotation.Nullable;

class DeliveryOrder {

    public String destinationName,phoneNumber,orderID;
    public String eta;
    public boolean inProgress;

    DeliveryOrder(String destinationName,String phoneNumber,@Nullable String orderID,boolean inProgress,String eta){
        this.destinationName=destinationName;
        this.phoneNumber=phoneNumber;
        if(orderID.equals(""))
            orderID=null;
        this.orderID=orderID;
        this.inProgress=inProgress;
        this.eta=eta;
    }

    DeliveryOrder(){
        destinationName="None";
        phoneNumber="None";
        orderID="None";
        eta="None";
        inProgress=false;
    }

    String getDestinationName(){
        return destinationName;
    }

    String getPhoneNumber() {
        return phoneNumber;
    }

    String getOrderID() {
        return orderID;
    }

    String getEta() {
        return eta;
    }

    void setInProgress(boolean inProgress){
        this.inProgress= inProgress;
    }

    void setEta(String eta) {
        this.eta = eta;
    }

    boolean isInProgress() {
        return inProgress;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(this==obj)
            return true;
        if(obj==null || obj.getClass()!=getClass())
            return false;
        DeliveryOrder other=(DeliveryOrder)obj;
        return this.getOrderID().equals(other.getOrderID());
    }
}
