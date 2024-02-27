package com.yid.agv.backend.elevator;

import com.yid.agv.backend.ProcessAGVTask;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
public class ElevatorManager {
    private static final Logger log = LoggerFactory.getLogger(ProcessAGVTask.class);
    @Value("${http.timeout}")
    private int HTTP_TIMEOUT;
    @Value("${agvControl.url}")
    private String agvUrl;
    @Value("${elevator.pre_person_open_door_duration}")
    private int prePersonOpenDoorDuration;
    @Autowired
    private ElevatorSocketBox elevatorSocketBox;
    private ElevatorPermission elevatorPermission;

    private int prePersonOpenDoorCount;
    private int elevatorPersonCount;
    private boolean iAlarmObstacle;
    private final Map<Integer, ElevatorCaller> elevatorCallerMap;
    private final Queue<Integer> callQueue;

    public ElevatorManager() {
        elevatorCallerMap = new HashMap<>();
        callQueue = new ConcurrentLinkedDeque<>();
    }

    @PostConstruct
    public void initialize() {
        prePersonOpenDoorCount = 0;
        elevatorPersonCount = 0;
        for (int i = 1; i <= 4; i++) {
            elevatorCallerMap.put(i, new ElevatorCaller(i));
        }
        elevatorPermission = ElevatorPermission.FREE;
    }

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
            elevatorCaller.setGreenLight(ElevatorCaller.IOStatus.OFF);
            elevatorCaller.setRedLight(ElevatorCaller.IOStatus.OFF);
        });
        switch (elevatorPermission) {
            case SYSTEM -> {
                elevatorPersonCount = 0;
                elevatorCallerMap.values().forEach(elevatorCaller -> elevatorCaller.setRedLight(ElevatorCaller.IOStatus.ON));
            }
            case PRE_PERSON -> {
                Integer floor = callQueue.peek();
                ElevatorCaller elevatorCaller = elevatorCallerMap.get(floor);
                if (elevatorCaller.isIOpenDoor()) {
                    if (!elevatorSocketBox.isElevatorBoxManual()) {
                        prePersonOpenDoorCount++;
                        if(prePersonOpenDoorCount > prePersonOpenDoorDuration){
                            callQueue.poll();
                            elevatorCaller.setICallButton(false);
                            controlElevatorTO(null);
                            elevatorPermission = ElevatorPermission.FREE;
                            prePersonOpenDoorCount = 0;
                        }
                    } else {
                        callQueue.poll();
                        elevatorCaller.setICallButton(false);
                        controlElevatorTO(null);
                        elevatorPermission = ElevatorPermission.PERSON;
                        prePersonOpenDoorCount = 0;
                    }
                }
            }
            case PERSON -> {
                elevatorPersonCount++;
                if (!elevatorSocketBox.isElevatorBoxManual()) {
                    elevatorPersonCount = 0;
                    elevatorPermission = ElevatorPermission.FREE;
                }
            }
            case FREE -> {
                elevatorPersonCount = 0;
                if (!callQueue.isEmpty()) {
                    Integer floor = callQueue.peek();
                    if (floor != null){
                        controlElevatorTO(floor);
                        elevatorPermission = ElevatorPermission.PRE_PERSON;
                    }
                } else {
                    elevatorCallerMap.values().forEach(elevatorCaller -> elevatorCaller.setGreenLight(ElevatorCaller.IOStatus.ON));
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

    private void updateCaller1OfflineStatus(ElevatorCaller elevatorCaller){
        elevatorCaller.setICaller1Offline(true);
        elevatorCaller.setGreenLight(ElevatorCaller.IOStatus.UNKNOWN);
        elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.UNKNOWN);
        elevatorCaller.setRedLight(ElevatorCaller.IOStatus.UNKNOWN);
        elevatorCaller.setIBuzz(ElevatorCaller.IOStatus.UNKNOWN);
    }
    private void updateCaller2OfflineStatus(ElevatorCaller elevatorCaller){
        elevatorCaller.setICaller2Offline(true);
    }
    private void updateCaller1OnlineStatus(ElevatorCaller elevatorCaller, String[] data){
        elevatorCaller.setICaller1Offline(false);

        Optional<boolean[]> optionalIOValue = Optional.ofNullable(parseStatus(Integer.parseInt(data[1])));

        elevatorCaller.setGreenLight(optionalIOValue.filter(ioValue -> elevatorCaller.getGreenLight() != ElevatorCaller.IOStatus.TOGGLE).map(ioValue -> (ioValue[0] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)).orElse(ElevatorCaller.IOStatus.UNKNOWN));
        elevatorCaller.setYellowLight(optionalIOValue.filter(ioValue -> elevatorCaller.getYellowLight() != ElevatorCaller.IOStatus.TOGGLE).map(ioValue -> (ioValue[1] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)).orElse(ElevatorCaller.IOStatus.UNKNOWN));
        elevatorCaller.setRedLight(optionalIOValue.filter(ioValue -> elevatorCaller.getRedLight() != ElevatorCaller.IOStatus.TOGGLE).map(ioValue -> (ioValue[2] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)).orElse(ElevatorCaller.IOStatus.UNKNOWN));
        elevatorCaller.setIBuzz(optionalIOValue.filter(ioValue -> elevatorCaller.getIBuzz() != ElevatorCaller.IOStatus.TOGGLE).map(ioValue -> (ioValue[3] ? ElevatorCaller.IOStatus.ON : ElevatorCaller.IOStatus.OFF)).orElse(ElevatorCaller.IOStatus.UNKNOWN));

    }
    private void updateCaller2OnlineStatus(ElevatorCaller elevatorCaller, String[] data){
        elevatorCaller.setICaller2Offline(false);

        Optional<boolean[]> optionalIOValue = Optional.ofNullable(parseStatus(Integer.parseInt(data[1])));

        optionalIOValue.ifPresent(ioValue -> {
            // ioValue[0] 為 Button Trigger (真正呼叫電梯的I/O)

            // 假設呼叫按鈕按下
            if (ioValue[4]) {
                elevatorCaller.setICallButton(true);
                AtomicBoolean had = new AtomicBoolean(false);
                callQueue.forEach(floor -> {
                    if (floor == elevatorCaller.getFloor()) {
                        had.set(true);
                    }
                });
                if(!had.get()){
                    callQueue.offer(elevatorCaller.getFloor());
                }
                // 按下按鈕且不是電梯目標樓層則顯示閃黃燈
                if (!elevatorCaller.isDoCallElevator() && elevatorCaller.getYellowLight() != ElevatorCaller.IOStatus.TOGGLE) {
                    elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.TOGGLE);
                }
            } else {
                elevatorCaller.setICallButton(false);
                elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.OFF);
            }

            elevatorCaller.setIOpenDoor(!ioValue[2]);
        });
    }

    public ElevatorPermission getElevatorPermission() {
        return elevatorPermission;
    }

    public boolean acquireElevatorPermission() {
        if (elevatorPermission == ElevatorPermission.SYSTEM){
            return true;
        } else if (elevatorPermission != ElevatorPermission.FREE || callQueue.size() > 0){
            return false;
        } else if (elevatorSocketBox.isElevatorBoxScan()) {
            iAlarmObstacle = true;
            return false;
        } else {
            iAlarmObstacle = false;
            elevatorPermission = ElevatorPermission.SYSTEM;
            return true;
        }

    }

    public void resetElevatorPermission() {
        elevatorPermission = ElevatorPermission.FREE;
    }

    public int getElevatorPersonCount(){
        return elevatorPersonCount;
    }

    public boolean getIAlarmObstacle(){
        return iAlarmObstacle;
    }

    public boolean iOpenDoorByFloor(int floor){
        return elevatorCallerMap.get(floor).isIOpenDoor();
    }

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

    public void controlElevatorTO(Integer floor){
        if (floor == null) {
            elevatorCallerMap.values().forEach(elevatorCaller -> elevatorCaller.setDoCallElevator(false));
        } else {
            elevatorCallerMap.values().forEach(elevatorCaller -> {
                if (elevatorCaller.getFloor() == floor && !elevatorCaller.isDoCallElevator()) {
                    elevatorCaller.setDoCallElevator(true);
                    elevatorCaller.setYellowLight(ElevatorCaller.IOStatus.ON);
                } else {
                    elevatorCaller.setDoCallElevator(false);
                }
            });
        }
    }

    public void sendCaller(ElevatorCaller elevatorCaller){
        int caller1Id = elevatorCaller.getFloor()*2-1;
        int caller1OutputValue = convertToCaller1ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.ON);
        int caller1ToggleValue = convertToCaller1ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.TOGGLE);

        if(elevatorCaller.getLastCaller1OutputValue() != caller1OutputValue) {
            log.info("SendIOControlBoxOutput Id: " + caller1Id + " Value: " + caller1OutputValue);
            elevatorCaller.setLastCaller1OutputValue(caller1OutputValue);
            sendCaller(caller1Id, caller1OutputValue, "output");
        }
        if(elevatorCaller.getLastCaller1ToggleValue() != caller1ToggleValue) {
            log.info("SendIOControlBoxToggle Id: " + caller1Id + " Value: " + caller1ToggleValue);
            elevatorCaller.setLastCaller1ToggleValue(caller1ToggleValue);
            sendCaller(caller1Id, caller1ToggleValue, "toggle");
        }

        int caller2Id = elevatorCaller.getFloor()*2;
        int caller2OutputValue = convertToCaller2ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.ON); // callBTN clrcall
        int caller2ToggleValue = convertToCaller2ValueByIOStatus(elevatorCaller, ElevatorCaller.IOStatus.TOGGLE);

        if(elevatorCaller.getLastCaller2OutputValue() != caller2OutputValue) {
            log.info("SendIOControlBoxOutput Id: " + caller2Id + " Value: " + caller2OutputValue);
            elevatorCaller.setLastCaller2OutputValue(caller2OutputValue);
            if (caller2OutputValue == 0) {
                sendCaller(caller2Id, 0, "clrcall");
                log.info("clrcall");
            }
        }
        if(elevatorCaller.getLastCaller2ToggleValue() != caller2ToggleValue) {
            log.info("SendIOControlBoxToggle Id: " + caller2Id + " Value: " + caller2ToggleValue);
            elevatorCaller.setLastCaller2ToggleValue(caller2ToggleValue);
            sendCaller(caller1Id, caller2ToggleValue, "toggle");
        }
    }


    public void sendCaller(int callerId, int value, String command){
        Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agvUrl + "/caller=" + callerId + "&" + value + "&" + command))
                .GET()
                .timeout(timeout)
                .build();
        try {
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Thread.sleep(500);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean[] parseStatus(int statusValue) {
        if(statusValue < 0) return null;
        boolean[] statusArray = new boolean[8];
        // 從右到左解析各個位元狀態
        for (int i = 0; i < statusArray.length ; i++) {
            // 檢查第i位是否為1，若是則代表狀態為真
            statusArray[i] = (statusValue & 1) == 1;
            // 右移一位，繼續解析下一位元
            statusValue >>= 1;
        }
        return statusArray;
    }

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

    private int convertToCaller2ValueByIOStatus(ElevatorCaller elevatorCaller, ElevatorCaller.IOStatus ioStatus){
        int value = 0;
        if (ioStatus == ElevatorCaller.IOStatus.TOGGLE) {
            if (elevatorCaller.isDoCallElevator()) {
                value += 1;
            }
        } else if (ioStatus == ElevatorCaller.IOStatus.ON) {
            if (elevatorCaller.isICallButton()) {
                value += 16;
            }
        }
        return value;
    }



}
