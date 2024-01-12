package com.yid.agv.backend.elevator;

import com.yid.agv.model.Station;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class ElevatorManager {
    @Value("${http.timeout}")
    private int HTTP_TIMEOUT;
    @Value("${agvControl.url}")
    private String agvUrl;
    @Value("${elevator.pre_person_open_door_duration}")
    private int prePersonOpenDoorDuration;
    private ElevatorPermission elevatorPermission;

    private int prePersonOpenDoorCount;
    private int elevatorPersonCount;
    private boolean iOpenDoor;
    private boolean iPersonOccupancyButton;
    private boolean iObstacle;
    private boolean iAlarmObstacle;
    private final int[] lastCallerStatus;

    private final Queue<Integer> callQueue;
    public ElevatorManager() {
        callQueue = new ConcurrentLinkedDeque<>();
        lastCallerStatus = new int[8];
    }

    @PostConstruct
    public void initialize() {
        Arrays.fill(lastCallerStatus, -1);
        prePersonOpenDoorCount = 0;
        elevatorPersonCount = 0;
    }

//    @Scheduled(fixedRate = 1000)
    public void elevatorProcess() {
        String[] statusData = crawlStatus().orElse(new String[0]);
        if (statusData.length == 0) return;
        int[] callerStatus = new int[8];
        Arrays.fill(callerStatus, 0);
        String[] elevatorData = statusData[0].split(",");
        // 假設開門
        iOpenDoor = elevatorData[1].equals("1");
        // 假設人員按鈕被按下
        iPersonOccupancyButton = elevatorData[2].equals("1");
        // 假設電梯有障礙
        iObstacle = elevatorData[3].equals("1");

        for(int i = 0; i < statusData.length-1; i++){
            String[] data = statusData[i].split(",");  // 分隔資料
            if (data[0].equals("-1")){
                callerStatus[i] = -1;
                continue;
            }
            if(i % 2 == 0) {
                // 假設呼叫按鈕按下
                if (Objects.requireNonNull(parseStatus(Integer.parseInt(data[1])))[0]){
                    int finalI = i;
                    AtomicBoolean had = new AtomicBoolean(false);
                    callQueue.forEach(floor -> {
                        if(floor == finalI){
                            had.set(true);
                        }
                    });
                    if(!had.get()){
                        callQueue.offer(i);
                    }
                    callerStatus[i] = 2;  // TODO: 假設 2 是閃黃燈
                }
            }
        }

        switch (elevatorPermission){
            case SYSTEM -> {
                return;
            }
            case PRE_PERSON -> {
                Integer floor = callQueue.peek();
                callerStatus[floor] = 3;  // TODO: 假設黃色恆亮是3
                if (iOpenDoor){
                    if (!iPersonOccupancyButton){
                        prePersonOpenDoorCount++;
                        if(prePersonOpenDoorCount > prePersonOpenDoorDuration){
                            callQueue.poll();
                            // TODO: clear callerButton
                            controlElevatorDoor(floor, false);
                            elevatorPermission = ElevatorPermission.FREE;
                            prePersonOpenDoorCount = 0;
                        }
                    } else {
                        callQueue.poll();
                        // TODO: clear callerButton
                        controlElevatorDoor(floor, false);
                        elevatorPermission = ElevatorPermission.PERSON;
                        prePersonOpenDoorCount = 0;
                    }
                }
            }
            case PERSON -> {
                elevatorPersonCount++;
                if (!iPersonOccupancyButton){
                    elevatorPersonCount = 0;
                    // TODO: close Door
                    elevatorPermission = ElevatorPermission.FREE;
                }
            }
            case FREE -> {
                elevatorPersonCount = 0;
                if (!callQueue.isEmpty() && !iOpenDoor){
                    Integer floor = callQueue.peek();
                    if (controlElevatorDoor(floor, true)){
                        elevatorPermission = ElevatorPermission.PRE_PERSON;
                    }
                }
            }
        }

        for (int i = 0; i < callerStatus.length; i+=2) {
            if(callerStatus[i] == -1){
                lastCallerStatus[i] = -1;
                continue;
            }
            switch (elevatorPermission){
                case FREE,PRE_PERSON,PERSON -> {
                    if(callerStatus[i] == 3){
                        sendCaller(i+1, 6);  // TODO: 假設黃色、綠色燈號恆亮是6
                    } else if (callerStatus[i] == 2){
                        sendCaller(i+1, 5);  // TODO: 假設綠色燈號恆亮、黃色閃爍是5
                    } else {
                        sendCaller(i+1, 4);  // TODO: 假設綠色燈號恆亮4
                    }
                }
                case SYSTEM -> {
                    if (callerStatus[i] == 2){
                        sendCaller(i+1, 9);  // TODO: 假設紅色、黃色燈號恆亮是9
                    } else {
                        sendCaller(i+1, 8);  // TODO: 假設紅燈號恆亮8
                    }
                }

            }
        }

    }

    public ElevatorPermission getElevatorPermission() {
        return elevatorPermission;
    }

    public boolean getIOpenDoor(){
        return iOpenDoor;
    }

    public boolean acquireElevatorPermission() {
        if (elevatorPermission == ElevatorPermission.SYSTEM){
            return true;
        } else if (elevatorPermission == ElevatorPermission.PRE_PERSON || elevatorPermission == ElevatorPermission.PERSON || callQueue.size() > 0){
            return false;
        } else if (iObstacle){
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

    public Optional<String[]> crawlStatus() {
        Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agvUrl + "/callers"))  // TODO: fix
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

    public boolean controlElevatorDoor(int floor, boolean iOpen){
        String cmd = iOpen ? "J0130" : "J0132";
        Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agvUrl + "/cmd=" + floor + "&Q" + cmd + "X"))  // TODO: fix
                .GET()
                .timeout(timeout)
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String webpageContent = response.body();
            return webpageContent.trim().equals("OK");
        } catch (IOException | InterruptedException ignored) {
            return false;
        }
    }


    public void sendCaller(int id, int value){
        if(lastCallerStatus[id-1] != value) {
            System.out.println("Id: " + id + "  Value: " + value);
            lastCallerStatus[id-1] = value;
            Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
            HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("http://192.168.0.100:20100/caller=" + id + "&" + value + "&output"))
                    .uri(URI.create(agvUrl + "/caller=" + id + "&" + value + "&output"))
                    .GET()
                    .timeout(timeout)
                    .build();
            try {
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
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


}
