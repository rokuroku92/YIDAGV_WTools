package com.yid.agv.backend.agv;

import com.yid.agv.backend.agvtask.AGVQTask;
import com.yid.agv.repository.NotificationDao;
import lombok.Data;

/**
 * AGV（Automated Guided Vehicle，自動導引車）的類。
 * 屬性皆來自於 TrafficControl 提供的資料。
 */
@Data
public class AGV {
    /**
     * 表示 AGV 的狀態。
     */
    public enum Status{
        OFFLINE, ONLINE, MANUAL, REBOOT,
        STOP, DERAIL, COLLIDE, OBSTACLE,
        EXCESSIVE_TURN_ANGLE, WRONG_TAG_NUMBER, UNKNOWN_TAG_NUMBER,
        EXCEPTION_EXCLUSION, SENSOR_ERROR, CHARGE_ERROR, NAVIGATION_LOST, ERROR_AGV_DATA

    }

    /**
     * 表示 AGV 任務的狀態。
     */
    public enum TaskStatus{
        NO_TASK, PRE_START_STATION, PRE_TERMINAL_STATION, COMPLETED
    }

    private int id; // AGV 的唯一標識符
    private Status status; // AGV 的狀態
    private String place; // AGV 的位置(RFID Tag)
    private int battery; // AGV 的電池電量(電壓、電流經庫侖計轉換)
    private int signal; // AGV 的信號強度
    private boolean iScan; // 是否有棧板

    private AGVQTask task; // AGV 的當前任務
    private TaskStatus taskStatus; // AGV 的當前任務狀態
    private NotificationDao.Title title; // AGV 的資料庫通知表標題

    // 以下是程式附帶屬性|補償屬性
    private String lastAgvSystemStatusData;  // 利基系統的狀態參數
    private boolean iLowBattery;  // 電量低於某數持續一段時間後會改變這個狀態
    private int lowBatteryCount;  // 計數器，用於計算低電量持續時間
    private int reDispatchCount;  // 當任務狀態值錯誤位元為1時，要做重新派遣，此變數用來計算次數
    private boolean tagError;  // 卡號錯誤時需要暫停至恢復任務
    private boolean fixAgvTagErrorCompleted;  // 卡號錯誤是否成功消除
    private boolean tagErrorDispatchCompleted;  // 卡號錯誤是否成功派遣回原任務
    private boolean lastTaskBuffer;  // 對於利基系統卡號錯誤時系統補償的緩衝值
    private int obstacleCount;  // 前有障礙時計數器
    private int scanCountForHandleNotExecutingTaskRedispatch; // 是否有棧板持續時間計數器
    private boolean iAlarm;  // 是否需要前端發出警報

    /**
     * 建構一個新的 AGV 對象。
     * @param id AGV 的唯一標識符
     */
    public AGV(int id) {
        this.id=id;
        this.status = Status.OFFLINE;
        this.taskStatus = TaskStatus.NO_TASK;
        this.title = switch (id) {
            case 1 -> NotificationDao.Title.AMR_1;
            case 2 -> NotificationDao.Title.AMR_2;
            case 3 -> NotificationDao.Title.AMR_3;
            default -> NotificationDao.Title.AGV_SYSTEM;
        };
    }
}
