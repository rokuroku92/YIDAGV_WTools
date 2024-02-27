package com.yid.agv.model;

import com.yid.agv.backend.elevator.ElevatorPermission;
import lombok.Data;

@Data
public class ElevatorStatusResponse {
    private ElevatorPermission elevatorPermission;
    private boolean iAlarmObstacle;
    private int elevatorPersonCount;
    private boolean iConnected;
    private boolean iManual;
    private boolean iScan;
    private boolean iBuzzer;
    private boolean iError;
}
