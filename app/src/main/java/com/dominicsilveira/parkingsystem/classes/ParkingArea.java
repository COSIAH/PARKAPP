package com.dominicsilveira.parkingsystem.classes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ParkingArea implements Serializable{
    public String name,upiId,upiName,userID;
    public double latitude,longitude;
    public int totalSlots,occupiedSlots,availableSlots,amount2,amount3,amount4;
    public List<SlotNoInfo> slotNos = new ArrayList<>();

    public ParkingArea(){}

    public ParkingArea(String name, double latitude,double longitude,
                       String upiId,String upiName, String userID,
                       int totalSlots,int occupiedSlots,
                       int amount2,int amount3,int amount4,List<SlotNoInfo> slotNos){
        this.name=name;
        this.latitude=latitude;
        this.longitude=longitude;
        this.upiId=upiId;
        this.upiName=upiName;
        this.userID=userID;
        this.totalSlots=totalSlots;
        this.occupiedSlots=occupiedSlots;
        this.availableSlots=totalSlots-occupiedSlots;
        this.amount2=amount2;
        this.amount3=amount3;
        this.amount4=amount4;
        this.slotNos=slotNos;
    }

    public void setData(String varName,int varData){
        if(varName.equals("availableSlots"))
            this.availableSlots=varData;
        if(varName.equals("occupiedSlots"))
            this.occupiedSlots=varData;
        if(varName.equals("totalSlots"))
            this.totalSlots=varData;
    }
}



