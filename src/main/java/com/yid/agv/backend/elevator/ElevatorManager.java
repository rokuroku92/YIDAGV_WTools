package com.yid.agv.backend.elevator;

import com.yid.agv.repository.NotificationDao;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用於管理電梯的類。
 */
@Component
public class ElevatorManager {
    private static final Logger log = LoggerFactory.getLogger(ElevatorManager.class);
    @Value("${http.timeout}")
    private int HTTP_TIMEOUT;
    @Value("${agvControl.url}")
    private String agvUrl;
    @Value("${elevator.pre_person_open_door_duration}")
    private int prePersonOpenDoorDuration;
    private final ElevatorSocketBox elevatorSocketBox;
    private final NotificationDao notificationDao;

    @Getter
    private ElevatorPermission elevatorPermission;
    private boolean hasOpenedDoorBefore;
    private int prePersonOpenDoorCount;
    @Getter
    private int elevatorPersonCount;
    private boolean iAlarmObstacle;
    private final Map<Integer, ElevatorCaller> elevatorCallerMap;
    private final Queue<Integer> callQueue;

    /**
     * ElevatorManager 的構造函數。
     */
    public ElevatorManager(ElevatorSocketBox elevatorSocketBox,
                           NotificationDao notificationDao) {
        this.elevatorSocketBox = elevatorSocketBox;
        this.notificationDao = notificationDao;
        elevatorCallerMap = new HashMap<>();
        callQueue = new ConcurrentLinkedDeque<>();
    }

    /**
     * 初始化 ElevatorManager。
     */
    @PostConstruct
    public void initialize() {
        prePersonOpenDoorCount = 0;
        elevatorPersonCount = 0;
        for (int i = 2; i <= 5; i++) {  // TODO: 改為 int i = 1; i <= 5; i++
            ElevatorCaller elevatorCaller = new ElevatorCaller(i);
            elevatorCallerMap.put(i, elevatorCaller);
            updateCaller1OfflineStatus(elevatorCaller);
            updateCaller2OfflineStatus(elevatorCaller);
        }
        elevatorPermission = ElevatorPermission.FREE;
    }

    /**
     * 用於定時處理電梯任務的方法。
     * 此方法根據一定的時間間隔執行，處理電梯的狀態和任務。
     */
    @Scheduled(fixedRate = 1000)
    public void elevatorProcess() {
        String[] allCallerInstantStatuses = crawlStatus().orElse(new String[0]);
        if (allCallerInstantStatuses.length == 0) return;

        elevatorCallerMap.values().forEach(elevatorCaller -> {
            String callerId1 = Integer.toString(elevatorCaller.getFloor()*2-1);
            String callerId2 = Integer.toString(elevatorCaller.getFloor()*2);
            String[] identifiedCaller1Data = null;
            String[] identifiedCaller2Data = null;
            // 比對資料，取出正確 AGV 資料
            for (String callerInstantStatus : allCallerInstantStatuses) {
                String[] unidentifiedCallerData = callerInstantStatus.split(",");  // 分隔 Traffic Control 資料
                String dataCallerId = unidentifiedCallerData[0].trim();
                if (dataCallerId.equals(callerId1)) {
                    identifiedCaller1Data = unidentifiedCallerData;
                } else if (dataCallerId.equals(callerId2)){
                    identifiedCaller2Data = unidentifiedCallerData;
                }
            }

            if (identifiedCaller1Data == null || Integer.parseInt(identifiedCaller1Data[1]) < 0){
                updateCaller1OfflineStatus(elevatorCaller);
            } else {
                updateCaller1OnlineStatus(elevatorCaller, identifiedCaller1Data);
            }
            if (identifiedCaller2Data == null || Integer.parseInt(identifiedCaller2Data[1]) < 0) {
                updateCaller2OfflineStatus(elevatorCaller);
            } else {
                updateCaller2OnlineStatus(elevatorCaller, identifiedCaller2Data);
            }
        });

        elevatorCallerMap.values().forEach(elevatorCaller -> {
            elevatorCaller.setGreenLight(ElevatorCaller.IOStatus.ON);
            elevatorCaller.setRedLight(ElevatorCaller.IOStatus.OFF);
        });
        switch (elevatorPermission) {
            case SYSTEM -> {
                elevatorPersonCount = 0;
                elevatorCallerMap.values().forEach(elevatorCaller -> {
                    elevatorCaller.setGreenLight(ElevatorCaller.IOStatus.OFF);
                    elevatorCaller.setRedLight(ElevatorCaller.IOStatus.ON);
                });
                if (elevatorSocketBox.isElevatorBoxManual() == ElevatorSocketBox.ElevatorBoxStatus.TRUE) {
                    elevatorSocketBox.sendCommandToElevatorBox(ElevatorSocketBox.ElevatorBoxCommand.OPEN_BUZZER);
                } else if (elevatorSocketBox.isElevatorBoxManual() == ElevatorSocketBox.ElevatorBoxStatus.FALSE) {
                    if (elevatorSocketBox.isElevatorBoxBuzzer() == ElevatorSocketBox.ElevatorBoxStatus.TRUE)
                        elevatorSocketBox.sendCommandToElevatorBox(ElevatorSocketBox.ElevatorBoxCommand.CLOSE_BUZZER);
                }
            }
            case PRE_PERSON -> {
                Integer floor = callQueue.peek();
                ElevatorCaller elevatorCaller = elevatorCallerMap.get(floor);
                controlElevatorTO(floor);
                if (elevatorCaller.isIOpenDoor() || hasOpenedDoorBefore) {
                    hasOpenedDoorBefore = true;
                    elevatorCaller.setICallButton(false);
                    controlElevatorTO(null);
                    if (elevatorSocketBox.isElevatorBoxManual() == ElevatorSocketBox.ElevatorBoxStatus.FALSE) {
                        prePersonOpenDoorCount++;
                        log.info("prePersonOpenDoorCount: " + prePersonOpenDoorCount);
//                        if(elevatorSocketBox.isElevatorBoxBuzzer()  == ElevatorSocketBox.ElevatorBoxStatus.FALSE){
//                            elevatorSocketBox.sendCommandToElevatorBox(ElevatorSocketBox.ElevatorBoxCommand.OPEN_BUZZER);
//                        }
                        if(prePersonOpenDoorCount > prePersonOpenDoorDuration) {
                            elevatorSocketBox.sendCommandToElevatorBox(ElevatorSocketBox.ElevatorBoxCommand.CLOSE_BUZZER);
                            callQueue.poll();
//                            elevatorCaller.setICallButton(false);
//                            controlElevatorTO(null);
                            elevatorPermission = ElevatorPermission.FREE;
                            hasOpenedDoorBefore = false;
                            prePersonOpenDoorCount = 0;
                        }
                    } else {
                        elevatorSocketBox.sendCommandToElevatorBox(ElevatorSocketBox.ElevatorBoxCommand.CLOSE_BUZZER);
                        callQueue.poll();
//                        elevatorCaller.setICallButton(false);
//                        controlElevatorTO(null);
                        elevatorPermission = ElevatorPermission.PERSON;
                        prePersonOpenDoorCount = 0;
                    }
                }
            }
            case PERSON -> {
                elevatorPersonCount++;
                if (elevatorSocketBox.isElevatorBoxBuzzer() == ElevatorSocketBox.ElevatorBoxStatus.TRUE)
                    elevatorSocketBox.sendCommandToElevatorBox(ElevatorSocketBox.ElevatorBoxCommand.CLOSE_BUZZER);
                if (elevatorSocketBox.isElevatorBoxManual() == ElevatorSocketBox.ElevatorBoxStatus.FALSE) {
                    elevatorPersonCount = 0;
                    hasOpenedDoorBefore = false;
                    elevatorPermission = ElevatorPermission.FREE;
                }
            }
            case FREE -> {
                elevatorPersonCount = 0;
                if (elevatorSocketBox.isElevatorBoxManual() == ElevatorSocketBox.ElevatorBoxStatus.TRUE) {
                    elevatorPermission = ElevatorPermission.PERSON;
                }
                if (!callQueue.isEmpty()) {
                    Integer floor = callQueue.peek();
                    if (floor != null) {
                        controlElevatorTO(floor);
                        elevatorPermission = ElevatorPermission.PRE_PERSON;
                    }
                }
            }
        }

        // 發送指令到TrafficControl
        elevatorCallerMap.values().forEach(elevatorCaller -> {
            if (!elevatorCaller.isICaller1Offline() && !elevatorCaller.isICaller2Offline()){
                sendCaller(elevatorCaller);
            }
        });

    }

    /**
     * 更新 Caller1 離線狀態的方法。
     * @param elevatorCaller 電梯呼叫器對象
     */
    private void updateCaller1OfflineStatus(ElevatorCaller elevatorCaller){
        if (!elevatorCaller.isICaller1Offline()) {
            switch (elevatorCaller.getFloor()) {
                case 1 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_1, NotificationDao.Status.OFFLINE);
                case 2 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_3, NotificationDao.Status.OFFLINE);
                case 3 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_5, NotificationDao.Status.OFFLINE);
                case 4 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_7, NotificationDao.Status.OFFLINE);
                case 5 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_9, NotificationDao.Status.OFFLINE);
            }
        }
        elevatorCaller.setICaller1Offline(true);
        elevatorCaller.setGreenLight(ElevatorCaller.IOStatus.UNKNOWN);
        elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.UNKNOWN);
        elevatorCaller.setRedLight(ElevatorCaller.IOStatus.UNKNOWN);
        elevatorCaller.setIBuzz(ElevatorCaller.IOStatus.UNKNOWN);
    }

    /**
     * 更新 Caller2 離線狀態的方法。
     * @param elevatorCaller 電梯呼叫器對象
     */
    private void updateCaller2OfflineStatus(ElevatorCaller elevatorCaller){
        if (!elevatorCaller.isICaller2Offline()) {
            switch (elevatorCaller.getFloor()) {
                case 1 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_2, NotificationDao.Status.OFFLINE);
                case 2 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_4, NotificationDao.Status.OFFLINE);
                case 3 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_6, NotificationDao.Status.OFFLINE);
                case 4 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_8, NotificationDao.Status.OFFLINE);
                case 5 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_10, NotificationDao.Status.OFFLINE);
            }
        }
        elevatorCaller.setICaller2Offline(true);
    }

    /**
     * 更新 Caller1 在線狀態的方法。
     * @param elevatorCaller 電梯呼叫器對象
     * @param data 電梯呼叫器數據
     */
    private void updateCaller1OnlineStatus(ElevatorCaller elevatorCaller, String[] data){
        if (elevatorCaller.isICaller1Offline()) {
            switch (elevatorCaller.getFloor()) {
                case 1 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_1, NotificationDao.Status.ONLINE);
                case 2 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_3, NotificationDao.Status.ONLINE);
                case 3 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_5, NotificationDao.Status.ONLINE);
                case 4 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_7, NotificationDao.Status.ONLINE);
                case 5 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_9, NotificationDao.Status.ONLINE);
            }
        }
        elevatorCaller.setICaller1Offline(false);
        elevatorCaller.setInstantCaller1Value(Integer.parseInt(data[1]));
        Optional<boolean[]> optionalIOValue = Optional.ofNullable(parseStatus(Integer.parseInt(data[1])));

        elevatorCaller.setGreenLight(optionalIOValue.map(ioValue ->
                ioValue[1] ? ElevatorCaller.IOStatus.TOGGLE :
                        ioValue[0] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)
                .orElse(ElevatorCaller.IOStatus.UNKNOWN));
        elevatorCaller.setYellowLight(optionalIOValue.map(ioValue ->
                ioValue[3] ? ElevatorCaller.IOStatus.TOGGLE :
                        ioValue[2] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)
                .orElse(ElevatorCaller.IOStatus.UNKNOWN));
        elevatorCaller.setRedLight(optionalIOValue.map(ioValue ->
                ioValue[5] ? ElevatorCaller.IOStatus.TOGGLE :
                        ioValue[4] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)
                .orElse(ElevatorCaller.IOStatus.UNKNOWN));
        elevatorCaller.setIBuzz(optionalIOValue.map(ioValue ->
                ioValue[7] ? ElevatorCaller.IOStatus.TOGGLE :
                        ioValue[6] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)
                .orElse(ElevatorCaller.IOStatus.UNKNOWN));

    }

    /**
     * 更新 Caller2 在線狀態的方法。
     * @param elevatorCaller 電梯呼叫器對象
     * @param data 電梯呼叫器數據
     */
    private void updateCaller2OnlineStatus(ElevatorCaller elevatorCaller, String[] data){
        if (elevatorCaller.isICaller2Offline()) {
            switch (elevatorCaller.getFloor()) {
                case 1 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_2, NotificationDao.Status.ONLINE);
                case 2 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_4, NotificationDao.Status.ONLINE);
                case 3 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_6, NotificationDao.Status.ONLINE);
                case 4 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_8, NotificationDao.Status.ONLINE);
                case 5 -> notificationDao.insertMessage(NotificationDao.Title.CALLER_10, NotificationDao.Status.ONLINE);
            }
        }

        elevatorCaller.setICaller2Offline(false);
        elevatorCaller.setInstantCaller2Value(Integer.parseInt(data[1]));
        Optional<boolean[]> optionalIOValue = Optional.ofNullable(parseStatus(Integer.parseInt(data[1])));

        optionalIOValue.ifPresent(ioValue -> {
            // ioValue[1] 為 Button Trigger (真正呼叫電梯的I/O)(TOGGLE)
            elevatorCaller.setDoCallElevator(ioValue[5]);

            // 假設呼叫按鈕按下
            if (elevatorCaller.getClrCallCount() <= 0 && ioValue[8]) {
                elevatorCaller.setICallButton(true);
                AtomicBoolean had = new AtomicBoolean(false);
                callQueue.forEach(floor -> {
                    if (floor == elevatorCaller.getFloor()) {
                        had.set(true);
                    }
                });
                if(!had.get()) {
                    callQueue.offer(elevatorCaller.getFloor());
                }
                // 按下按鈕且不是電梯目標樓層則顯示閃黃燈
                if (elevatorCaller.getFloor() != targetFloor) {
                    elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.TOGGLE);
                    elevatorCaller.setDoCallElevator(false);
                } else {
                    elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.ON);
                    elevatorCaller.setDoCallElevator(true);
                }
            } else {
                elevatorCaller.setICallButton(false);
                elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.OFF);
                elevatorCaller.setDoCallElevator(false);
                if (elevatorCaller.getClrCallCount() > 0) {
                    elevatorCaller.setClrCallCount(elevatorCaller.getClrCallCount()-1);
                }
            }

            if (elevatorPermission == ElevatorPermission.SYSTEM) {
                elevatorCaller.setDoCallElevator(elevatorCaller.getFloor() == targetFloor);
            }

            elevatorCaller.setIOpenDoor(!ioValue[6]);
        });
    }

    /**
     * 獲取電梯許可權的方法。
     * @return 如果成功獲取電梯許可權，返回 true；否則返回 false。
     */
    public boolean acquireElevatorPermission() {
        if (elevatorPermission == ElevatorPermission.SYSTEM) {
            return true;
        } else if (elevatorPermission != ElevatorPermission.FREE || !callQueue.isEmpty()){
            return false;
        } else if (elevatorSocketBox.isElevatorBoxScan() == ElevatorSocketBox.ElevatorBoxStatus.TRUE) {
            iAlarmObstacle = true;
            return false;
        } else {
            iAlarmObstacle = false;
            elevatorPermission = ElevatorPermission.SYSTEM;
            return true;
        }

    }

    /**
     * 重置電梯許可權的方法。
     */
    public void resetElevatorPermission() {
        elevatorPermission = ElevatorPermission.FREE;
    }

    /**
     * 獲取是否存在障礙物告警的方法。
     * @return 如果存在障礙物告警，返回 true；否則返回 false。
     */
    public boolean getIAlarmObstacle(){
        return iAlarmObstacle;
    }

    /**
     * 獲取指定樓層的電梯是否開門的方法。
     * @param floor 指定的樓層
     * @return 如果電梯門打開，返回 true；否則返回 false。
     */
    public boolean iOpenDoorByFloor(int floor){
        return elevatorCallerMap.get(floor).isIOpenDoor();
    }

    /**
     * 擷取電梯呼叫器的狀態的方法。
     * @return 獲取到的電梯呼叫器狀態，以字符串數組形式返回
     */
    public Optional<String[]> crawlStatus() {
        Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agvUrl + "/callers"))
                .GET()
                .timeout(timeout)
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String webpageContent = response.body();
            String[] data = Arrays.stream(webpageContent.split("<br>"))
                    .map(String::trim)
                    .toArray(String[]::new);
            return Optional.of(data);
        } catch (IOException | InterruptedException ignored) {
        }

        return Optional.empty();
    }

    private int targetFloor = 0;
    /**
     * 控制電梯移動到指定樓層的方法。
     * @param floor 指定的樓層，為 null 時表示取消指定樓層
     */
    public void controlElevatorTO(Integer floor){
        if (floor == null) {
            elevatorCallerMap.values().forEach(elevatorCaller -> elevatorCaller.setDoCallElevator(false));
            targetFloor = 0;
        } else {
            targetFloor = floor;
        }
    }

    /**
     * 用於總和 ElevatorCaller 對象資料後發送電梯呼叫器指令的方法。
     * @param elevatorCaller 電梯呼叫器對象
     */
    public void sendCaller(ElevatorCaller elevatorCaller) {
        int caller1Id = elevatorCaller.getFloor()*2-1;
        int caller1OutputValue = convertToCaller1ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.ON);
        int caller1ToggleValue = convertToCaller1ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.TOGGLE);

        boolean[] instantCaller1Status = parseStatus(elevatorCaller.getInstantCaller1Value());
        int instantCaller1OutputValue = 0;
        int instantCaller1ToggleValue = 0;
        for (int i = 0, index = 0; i < Objects.requireNonNull(instantCaller1Status).length && i < 8; i += 2, index++) {
            if (instantCaller1Status[i]) {
                instantCaller1OutputValue += (int) Math.pow(2, index);
            }
            if (instantCaller1Status[i+1]) {
                instantCaller1ToggleValue += (int) Math.pow(2, index);
            }
        }


        if(elevatorCaller.getLastCaller1OutputValue() != caller1OutputValue || caller1OutputValue != instantCaller1OutputValue) {
            log.info("SendIOControlBoxOutput Id: " + caller1Id + " Value: " + caller1OutputValue);
            elevatorCaller.setLastCaller1OutputValue(caller1OutputValue);
            sendCaller(caller1Id, caller1OutputValue, "output");
        }
        if(elevatorCaller.getLastCaller1ToggleValue() != caller1ToggleValue || caller1ToggleValue != instantCaller1ToggleValue) {
            elevatorCaller.setLastCaller1ToggleValue(caller1ToggleValue);
            if (caller1ToggleValue == 0){
                log.info("SendIOControlBoxToggle Id: " + caller1Id + " Value: " + caller1ToggleValue + ", resend output");
                elevatorCaller.setLastCaller1OutputValue(-1);  // 因為 toggle 0 無法解除，重新發送新的 output 即可
            } else {
                log.info("SendIOControlBoxToggle Id: " + caller1Id + " Value: " + caller1ToggleValue);
                sendCaller(caller1Id, caller1ToggleValue, "toggle");
            }

        }

        int caller2Id = elevatorCaller.getFloor() * 2;
        int caller2OutputValue = convertToCaller2ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.ON); // callBTN clrcall
        int caller2ToggleValue = convertToCaller2ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.TOGGLE);

        boolean[] instantCaller2Status = parseStatus(elevatorCaller.getInstantCaller2Value());
        int instantCaller2OutputValue = 0;
        int instantCaller2ToggleValue = 0;
        for (int i = 0, index = 0; i < Objects.requireNonNull(instantCaller2Status).length && i < 6; i += 2, index++) {
            if (instantCaller2Status[i]) {
                instantCaller2OutputValue += (int) Math.pow(2, index);
            }
            if (instantCaller2Status[i+1]) {
                instantCaller2ToggleValue += (int) Math.pow(2, index);
            }
        }

        if (instantCaller2Status[8]) instantCaller2OutputValue += 256;


        if (elevatorCaller.getLastCaller2OutputValue() != caller2OutputValue || caller2OutputValue != instantCaller2OutputValue) {
            log.info("SendIOControlBoxOutput Id: " + caller2Id + " Value: " + caller2OutputValue);
            elevatorCaller.setLastCaller2OutputValue(caller2OutputValue);
            if (caller2OutputValue == 0) {
                sendCaller(caller2Id, 0, "clrcall");
                elevatorCaller.setClrCallCount(5);
                log.info("clrcall");
            }
        }
        if (elevatorCaller.getLastCaller2ToggleValue() != caller2ToggleValue || caller2ToggleValue != instantCaller2ToggleValue) {
            elevatorCaller.setLastCaller2ToggleValue(caller2ToggleValue);
            log.info("SendIOControlBoxToggle Id: " + caller2Id + " Value: " + caller2ToggleValue);
            if (caller2ToggleValue == 0){
                sendCaller(caller2Id, 0, "output");
            } else {
                sendCaller(caller2Id, caller2ToggleValue, "toggle");
            }
        }
    }

    /**
     * 用於直接發送電梯呼叫器指令的方法。
     * @param callerId 電梯呼叫器ID
     * @param value 指令值 參考利基文件內容 v0.903
     * @param command 指令類型 output|toggle|clrcall
     */
    public void sendCaller(int callerId, int value, String command){
        Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agvUrl + "/caller=" + callerId + "&" + value + "&" + command))
                .GET()
                .timeout(timeout)
                .build();
        try {
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析電梯呼叫器的狀態值的方法。
     * @param statusValue 狀態值
     * @return 電梯呼叫器狀態的布爾數組
     */
    private boolean[] parseStatus(int statusValue) {
        if(statusValue < 0) return null;
        boolean[] statusArray = new boolean[16];
        // 從右到左解析各個位元狀態
        for (int i = 0; i < statusArray.length ; i++) {
            // 檢查第i位是否為1，若是則代表狀態為真
            statusArray[i] = (statusValue & 1) == 1;
            // 右移一位，繼續解析下一位元
            statusValue >>= 1;
        }
        return statusArray;
    }

    /**
     * 根據 IO 狀態轉換為 Caller1 的值的方法。
     * @param elevatorCaller 電梯呼叫器對象
     * @param ioStatus IO 狀態
     * @return Caller1 的值
     */
    private int convertToCaller1ValueByIOStatus(ElevatorCaller elevatorCaller, ElevatorCaller.IOStatus ioStatus){
        int value = 0;
        if (elevatorCaller.getGreenLight() == ioStatus) {
            value += 1;
        }
        if (elevatorCaller.getYellowLight() == ioStatus) {
            value += 2;
        }
        if (elevatorCaller.getRedLight() == ioStatus) {
            value += 4;
        }
        if (elevatorCaller.getIBuzz() == ioStatus) {
            value += 8;
        }
        return value;
    }

    /**
     * 根據 IO 狀態轉換為 Caller2 的值的方法。
     * @param elevatorCaller 電梯呼叫器對象
     * @param ioStatus IO 狀態
     * @return Caller2 的值
     */
    private int convertToCaller2ValueByIOStatus(ElevatorCaller elevatorCaller, ElevatorCaller.IOStatus ioStatus){
        int value = 0;
        if (ioStatus == ElevatorCaller.IOStatus.TOGGLE) {
            if (elevatorCaller.isDoCallElevator()) {
                value += 4;
            }
        } else if (ioStatus == ElevatorCaller.IOStatus.ON) {
            if (elevatorCaller.isICallButton()) {
                value += 256;
            }
        }
        return value;
    }


    public Map<Integer, ElevatorCaller> getElevatorCallerMap() {
        return elevatorCallerMap;
    }

}
