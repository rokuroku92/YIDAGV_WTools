package com.yid.agv.backend.elevator;

import lombok.Data;

@Data
public class ElevatorCaller {
    private int floor;
    public enum IOStatus{
        ON, OFF, TOGGLE, UNKNOWN
    }
    private IOStatus redLight;
    private IOStatus yellowLight;
    private IOStatus greenLight;
    private IOStatus iBuzz;
    private boolean iOpenDoor;
    private boolean iCallButton;
    private boolean doCallElevator;

    private boolean iCaller1Offline;
    private int lastCaller1OutputValue;
    private int lastCaller1ToggleValue;
    private boolean iCaller2Offline;
    private int lastCaller2OutputValue;
    private int lastCaller2ToggleValue;

    public ElevatorCaller(int floor){
        this.floor = floor;
        this.redLight = IOStatus.UNKNOWN;
        this.yellowLight = IOStatus.UNKNOWN;
        this.greenLight = IOStatus.UNKNOWN;
        this.iBuzz = IOStatus.UNKNOWN;
        this.lastCaller1OutputValue = -1;
        this.lastCaller1ToggleValue = -1;
        this.lastCaller2OutputValue = -1;
        this.lastCaller2ToggleValue = -1;
    }
}
