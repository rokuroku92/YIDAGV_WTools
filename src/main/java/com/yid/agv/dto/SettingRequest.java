package com.yid.agv.dto;

import lombok.Data;

@Data
public class SettingRequest {
    private String dbWToolsUrl;
    private String dbWToolsUsername;
    private String dbWToolsPassword;
    private String agvControlUrl;
    private Integer agvLowBattery;
    private Integer agvLowBatteryDuration;
    private Integer agvObstacleDuration;
    private Integer agvTaskExceptionOption;
    private String elevatorIp;
    private Integer elevatorPort;
    private Integer elevatorTimeout;
    private Integer elevatorFailSocket;
    private Integer elevatorPrePersonOpenDoorDuration;
    private Integer httpTimeout;
    private Integer httpMaxRetry;
}
