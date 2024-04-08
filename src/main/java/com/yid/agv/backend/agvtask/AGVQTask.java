package com.yid.agv.backend.agvtask;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示 AGV 任務（AGVQTask）的類。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AGVQTask {
    private String taskNumber; // 任務編號 ex: #NE20240320001。前兩碼為任務種類，暫定：YE經過電梯移轉、NE未使用電梯(普通任務)、RE經過電梯移轉(回送)、SB回待命點、LB低電量回待命點
    private int agvId; // AGV 的 ID
    private int sequence; // 任務步驟
    private String startStation; // 起始站點 ex: 1-T-1
    private String terminalStation; // 目標站點
    private Integer startStationId; // 起始站點資料庫id
    private Integer terminalStationId; // 目標站點資料庫id
    private int modeId; // 模式資料庫id
    private int status; // 狀態 0待執行、-1取消、1~99執行中、100完成

}
