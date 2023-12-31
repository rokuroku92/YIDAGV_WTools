package com.yid.agv.backend.agvtask;

import lombok.Data;

@Data
public class AGVQTask {
    private String taskNumber;
    private int agvId;
    private int sequence;
    private Integer startStationId;
    private Integer terminalStationId;
    private int modeId;
    private int status;

}
