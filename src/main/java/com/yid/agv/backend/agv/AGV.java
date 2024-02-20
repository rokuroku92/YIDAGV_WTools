package com.yid.agv.backend.agv;

import com.yid.agv.backend.agvtask.AGVQTask;
import com.yid.agv.repository.NotificationDao;
import lombok.Data;

@Data
public class AGV {
    public enum Status{
        OFFLINE(1), ONLINE(2), MANUAL(3), REBOOT(4),
        STOP(5), DERAIL(6), COLLIDE(7), OBSTACLE(8),
        EXCESSIVE_TURN_ANGLE(9), WRONG_TAG_NUMBER(10), UNKNOWN_TAG_NUMBER(11),
        EXCEPTION_EXCLUSION(12), SENSOR_ERROR(13), CHARGE_ERROR(14), ERROR_AGV_DATA(15);
        private final int value;
        Status(int value) {
            this.value = value;
        }

        public int getValue(){
            return value;
        }

    }

    public enum TaskStatus{
        NO_TASK, PRE_START_STATION, PRE_TERMINAL_STATION, COMPLETED
    }

    private int id;
    private Status status;
    private String place;
    private int battery;
    private int signal;

    private AGVQTask task;
    private TaskStatus taskStatus;
    private NotificationDao.Title title;

    // 以下是程式附帶屬性|補償屬性
    private String lastAgvSystemStatusData;  // 利基系統的狀態參數
    private boolean iLowBattery;  // 電量低於某數持續一段時間後會改變這個狀態
    private int lowBatteryCount;  // 計數器，用於計算低電量持續時間
    private int reDispatchCount;  // 當任務狀態值錯誤位元為1時，要做重新派遣，此變數用來計算次數
    private boolean tagError;  // 卡號錯誤時需要暫停至恢復任務
    private boolean fixAgvTagErrorCompleted;  // 卡號錯誤是否成功消除
    private boolean tagErrorDispatchCompleted;  // 卡號錯誤是否成功派遣回原任務
    private boolean lastTaskBuffer;  // 對於利基系統卡號錯誤時系統補償的緩衝值
    private int obstacleCount;  // 前有障礙時計數器，原time


    public AGV(int id){
        this.id=id;
        taskStatus = TaskStatus.NO_TASK;
        this.title = switch (id) {
            case 1 -> NotificationDao.Title.AMR_1;
            case 2 -> NotificationDao.Title.AMR_2;
            case 3 -> NotificationDao.Title.AMR_3;
            default -> NotificationDao.Title.AGV_SYSTEM;
        };
    }
}
