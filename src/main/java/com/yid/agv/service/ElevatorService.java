package com.yid.agv.service;

import com.yid.agv.backend.elevator.ElevatorManager;
import com.yid.agv.backend.elevator.ElevatorSocketBox;
import com.yid.agv.model.ElevatorStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ElevatorService {

    @Autowired
    private ElevatorSocketBox elevatorSocketBox;
    @Autowired
    private ElevatorManager elevatorManager;

    public ElevatorStatusResponse getElevatorStatus(){
        ElevatorStatusResponse elevatorStatusResponse = new ElevatorStatusResponse();
        elevatorStatusResponse.setElevatorPermission(elevatorManager.getElevatorPermission());
        elevatorStatusResponse.setIAlarmObstacle(elevatorManager.getIAlarmObstacle());  // AGV 請求電梯權限時，電梯內有東西
        elevatorStatusResponse.setElevatorPersonCount(elevatorManager.getElevatorPersonCount());
        elevatorStatusResponse.setIConnected(elevatorSocketBox.isElevatorBoxConnected());
        if (elevatorSocketBox.isElevatorBoxManual() != ElevatorSocketBox.ElevatorBoxStatus.UNKNOWN)
            elevatorStatusResponse.setIManual(elevatorSocketBox.isElevatorBoxManual() == ElevatorSocketBox.ElevatorBoxStatus.TRUE);
        if (elevatorSocketBox.isElevatorBoxScan() != ElevatorSocketBox.ElevatorBoxStatus.UNKNOWN)
            elevatorStatusResponse.setIScan(elevatorSocketBox.isElevatorBoxScan() == ElevatorSocketBox.ElevatorBoxStatus.TRUE);
        if (elevatorSocketBox.isElevatorBoxBuzzer() != ElevatorSocketBox.ElevatorBoxStatus.UNKNOWN)
            elevatorStatusResponse.setIBuzzer(elevatorSocketBox.isElevatorBoxBuzzer() == ElevatorSocketBox.ElevatorBoxStatus.TRUE);
        if (elevatorSocketBox.isElevatorBoxError() != ElevatorSocketBox.ElevatorBoxStatus.UNKNOWN)
            elevatorStatusResponse.setIError(elevatorSocketBox.isElevatorBoxError() == ElevatorSocketBox.ElevatorBoxStatus.TRUE);
        return elevatorStatusResponse;
    }

    public boolean cancelPersonOccupied(){
        if (elevatorSocketBox.isElevatorBoxConnected()){
            elevatorSocketBox.sendCommandToElevatorBox(ElevatorSocketBox.ElevatorBoxCommand.CLOSE_BUTTON);
            return true;
        } else {
            return false;
        }
    }

}
