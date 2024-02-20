package com.yid.agv.service;

import com.yid.agv.backend.elevator.ElevatorManager;
import com.yid.agv.backend.elevator.ElevatorSocketBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ElevatorService {

    @Autowired
    private ElevatorSocketBox elevatorSocketBox;
    @Autowired
    private ElevatorManager elevatorManager;

    public boolean getElevatorObstacleAlarm() {
        return elevatorManager.getIAlarmObstacle();
    }
    public int getElevatorPersonCount() {
        return elevatorManager.getElevatorPersonCount();
    }
    public boolean getIElevatorBoxManual() {
        return elevatorSocketBox.isElevatorBoxManual();
    }
    public boolean getIElevatorBoxBuzzer() {
        return elevatorSocketBox.isElevatorBoxBuzzer();
    }
    public boolean getIElevatorBoxConnected() {
        return elevatorSocketBox.isElevatorBoxConnected();
    }
    public boolean getIElevatorBoxScan() {
        return elevatorSocketBox.isElevatorBoxScan();
    }
    public boolean getIElevatorBoxError() {
        return elevatorSocketBox.isElevatorBoxError();
    }

}
