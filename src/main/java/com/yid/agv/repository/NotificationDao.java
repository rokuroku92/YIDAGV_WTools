package com.yid.agv.repository;


import com.yid.agv.model.Notification;

import java.util.List;

public interface NotificationDao {

    enum Title{
        AGV_SYSTEM(1), AMR_1(2), AMR_2(3), AMR_3(4),
        CALLER_1(5), CALLER_2(6);
        private final int value;
        Title(int value) {
            this.value = value;
        }
        public int getValue(){
            return value;
        }
    }
    enum Status{
        OFFLINE(1, "離線"), ONLINE(1, "連線"), MANUAL(2, "手動模式"), REBOOT(1, "重新啟動"),
        STOP(3, "緊急停止"), DERAIL(3, "出軌"), COLLIDE(3, "發生碰撞"), OBSTACLE(2, "前有障礙"),
        EXCESSIVE_TURN_ANGLE(3, "轉向角度過大"), WRONG_TAG_NUMBER(3, "卡號錯誤"), UNKNOWN_TAG_NUMBER(3, "未知卡號"),
        EXCEPTION_EXCLUSION(3, "異常排除"), SENSOR_ERROR(3, "感知器偵測異常"), CHARGE_ERROR(3, "充電異常"),
        ERROR_AGV_DATA(3, "讀取狀態資料錯誤"), FAILED_EXECUTION_TASK(3, "任務執行失敗"), FAILED_EXECUTION_TASK_THREE_TIMES(3, "任務執行三次皆失敗"),
        FAILED_SEND_TASK(3, "發送任務失敗"), FAILED_SEND_TASK_THREE_TIMES(3, "發送任務三次皆失敗"), BATTERY_TOO_LOW(3, "電池電量過低"),
        CALLER_LONG_OFFLINE(3, "caller離線超過20秒，請至現場排除問題");
        private final int level;
        private final String content;
        Status(int level, String content) {
            this.level = level;
            this.content = content;
        }

        public int getLevel(){
            return level;
        }
        public String getContent(){
            return content;
        }

    }

    List<Notification> queryTodayNotifications();
    
    List<Notification> queryNotificationsByDate(String date);
    
    List<Notification> queryAllNotifications();
    List<Notification> queryNotifications();
    List<Notification> queryNotificationsL();

    void insertMessage(Title titleId, Status messageId);
    void insertMessage(Title titleId, String content);

}
