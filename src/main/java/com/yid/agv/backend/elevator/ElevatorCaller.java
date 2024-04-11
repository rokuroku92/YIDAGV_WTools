package com.yid.agv.backend.elevator;

import lombok.Data;

/**
 * 表示電梯呼叫器（ElevatorCaller）的類。
 */
@Data
public class ElevatorCaller {
    private int floor; // 樓層，在特典案子中 floor=1 為 B1
    public enum IOStatus{
        ON, OFF, TOGGLE, UNKNOWN
    }
    private IOStatus redLight; // 紅燈狀態
    private IOStatus yellowLight; // 黃燈狀態
    private IOStatus greenLight; // 綠燈狀態
    private IOStatus iBuzz; // 蜂鳴器狀態
    private int instantCaller1Value; // 即時呼叫器1的值
    private int instantCaller2Value; // 即時呼叫器2的值
    private boolean iOpenDoor; // 門是否打開
    private boolean iCallButton; // 是否按下呼叫按鈕
    private boolean doCallElevator; // 是否呼叫電梯

    private boolean iCaller1Offline; // 呼叫器1是否離線
    private int lastCaller1OutputValue; // 上一次呼叫器1的 Output 值
    private int lastCaller1ToggleValue; // 上一次呼叫器1的 Toggle 值
    private boolean iCaller2Offline; // 呼叫器2是否離線
    private int lastCaller2OutputValue; // 上一次呼叫器2的 Output 值
    private int lastCaller2ToggleValue; // 上一次呼叫器2的 Toggle 值
    private int clrCallCount;  // 補償數，彌補 clrcall 延遲造成二次誤觸發。

    /**
     * ElevatorCaller 的構造函數。
     * @param floor 樓層。
     */
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
        clrCallCount = 0;
    }
}
