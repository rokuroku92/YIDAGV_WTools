package com.yid.agv.backend;

import com.yid.agv.backend.agv.AGVManager;
import com.yid.agv.backend.agv.AGV;
import com.yid.agv.backend.agvtask.AGVQTask;
import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.backend.agvtask.AGVTaskManager;
import com.yid.agv.model.Station;
import com.yid.agv.repository.*;
import com.yid.agv.service.HomePageService;
import jakarta.annotation.PostConstruct;
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
import java.util.stream.Collectors;

@Component
public class AGVInstantStatus {

    @Autowired
    private TestFakeData testFakeData;
    @Value("${http.timeout}")
    private int HTTP_TIMEOUT;
    @Value("${agvControl.url}")
    private String agvUrl;
    @Value("${agv.low_battery}")
    private int LOW_BATTERY;
    @Value("${agv.low_battery_duration}")
    private int LOW_BATTERY_DURATION;
    @Value("${agv.obstacle_duration}")
    private int OBSTACLE_DURATION;
    @Autowired
    private AGVIdDao agvIdDao;
    @Autowired
    private StationDao stationDao;
    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private AnalysisDao analysisDao;
    @Autowired
    private TaskDetailDao taskDetailDao;
    @Autowired
    private AGVManager agvManager;
    @Autowired
    private AGVTaskManager taskQueue;
    @Autowired
    private ProcessAGVTask processTasks;
    @Autowired
    private GridManager stationManager;
    @Autowired
    private HomePageService homePageService;

    private Map<Integer, Integer> stationIdTagMap;


    @PostConstruct
    public void initialize() {
        stationIdTagMap = stationDao.queryStations().stream()
                .collect(Collectors.toMap(Station::getId, Station::getTag));
    }

    @Scheduled(fixedRate = 1000) // 每秒執行
    public void updateAgvStatuses() {
        // 抓取AGV狀態，並更新到agvStatuses
        String[] agvStatusData = crawlAGVStatus().orElse(new String[0]);
        if(agvStatusData.length == 0) {
            for (int i = 0; i < agvManager.getAgvLength(); i++) {
                AGV agv = agvManager.getAgv(i+1);
                NotificationDao.Title agvTitle = switch (i) {
                    case 0 -> NotificationDao.Title.AMR_1;
                    case 1 -> NotificationDao.Title.AMR_2;
                    case 2 -> NotificationDao.Title.AMR_3;
                    default -> throw new IllegalStateException("Unexpected agvManager.getAgvLength() value: " + i);
                };
                updateAGVOfflineStatus(agv, agvTitle);
            }
            return;
        }
        for (int i = 0; i < agvManager.getAgvLength(); i++) {
            AGV agv = agvManager.getAgv(i+1);
//            String[] data = agvStatusData[i].split(",");  // 分隔 AGV 系統資料
            String[] data = null;  // 分隔 AGV 系統資料
            for(int j = 0; j < agvManager.getAgvLength(); j++){
                if (agvStatusData[j].split(",")[0].trim().equals(Integer.toString(agv.getId()))) {
                    data = agvStatusData[j].split(",");
                }
            }
            NotificationDao.Title agvTitle = switch (agv.getId()) {
                case 1 -> NotificationDao.Title.AMR_1;
                case 2 -> NotificationDao.Title.AMR_2;
                case 3 -> NotificationDao.Title.AMR_3;
                default -> throw new IllegalStateException("Unexpected agvManager.getAgvLength() value: " + i);
            };
            if (data == null) {
                updateAGVOfflineStatus(agv, agvTitle);
            } else {
                updateAGVOnlineStatus(agv, data, agvTitle);
            }
            updateTaskStatus(agv);
        }
    }


    private void updateTaskStatus(AGV agv){
        switch (agv.getTaskStatus()){
            case NO_TASK -> {
            }
            case PRE_START_STATION -> {
                Integer startStation = agv.getTask().getStartStationId();
                if (startStation != null && startStation != 0 && agv.getPlace().equals(Integer.toString(stationIdTagMap.get(startStation)))){
//                    gridManager.setGridStatus(startStation, Grid.Status.FREE);
                    // TODO: 取消Booked
                    agv.setTaskStatus(AGV.TaskStatus.PRE_TERMINAL_STATION);
                }
            }
            case PRE_TERMINAL_STATION -> {
                Integer terminalStation = agv.getTask().getTerminalStationId();
                if (terminalStation != null && terminalStation != 0 && agv.getPlace().equals(Integer.toString(stationIdTagMap.get(terminalStation)))){
//                    gridManager.setGridStatus(terminalStation, Grid.Status.OCCUPIED);
                    // TODO: 取消Booked
                    agv.setTaskStatus(AGV.TaskStatus.COMPLETED);
                }
            }
        }
    }

    private void updateAGVOfflineStatus(AGV agv, NotificationDao.Title agvTitle){
        if(agv.getStatus() != AGV.Status.OFFLINE){
            notificationDao.insertMessage(agvTitle, NotificationDao.Status.OFFLINE);
            agv.setStatus(AGV.Status.OFFLINE);
            agv.setSignal(0);
            agv.setBattery(0);
            CountUtilizationRate.isPoweredOn[agv.getId()-1] = false;
            CountUtilizationRate.isWorking[agv.getId()-1] = false;
        }
    }


    private void updateAGVOnlineStatus(AGV agv, String[] data, NotificationDao.Title agvTitle){
        CountUtilizationRate.isPoweredOn[agv.getId()-1] = true;

        // data[1] 位置
        agv.setPlace(data[1].trim());
        // data[2] 訊號
        agv.setSignal((int)Math.ceil(Integer.parseInt(data[2].trim()) / 120.0 * 100));
        // data[3] 電量
        agv.setBattery(Integer.parseInt(data[3].trim()));
        if(agv.getBattery()<LOW_BATTERY){
            if(agv.getLowBatteryCount()>LOW_BATTERY_DURATION){
                if(!agv.isILowBattery()){
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.BATTERY_TOO_LOW);
                    agv.setILowBattery(true);
                }
            } else {
                agv.setLowBatteryCount(agv.getLowBatteryCount()+1);
            }
        } else {
            agv.setLowBatteryCount(0);
            agv.setILowBattery(false);
        }

        // data[5] agv狀態
        updateAgvStatus(agv, data[5].trim(), agvTitle);

        if (agv.isTagError()){
            handleTagError(parseAGVStatus(Integer.parseInt(data[4].trim())), agv);
        } else if (agv.getStatus() == AGV.Status.ONLINE && agv.getTask() != null){
            agv.setObstacleCount(0);
            // data[4] 任務狀態
            boolean[] taskStatus = parseAGVStatus(Integer.parseInt(data[4].trim()));
            if (taskStatus[7]) {
                handleFailedTask(agv);
            } else {
                if (taskStatus[0]) {
                    handleExecutingTask(agv);
                } else {
                    handleCompletedTask(agv, agvTitle);
                }
            }
        } else if (agv.getStatus() == AGV.Status.OBSTACLE) { // 若前有障礙時
            if(agv.getObstacleCount()<OBSTACLE_DURATION){
                agv.setObstacleCount(agv.getObstacleCount()+1);
            }else{
                homePageService.setIAlarm(1);
            }
        }



    }

    private void handleTagError(boolean[] taskStatus, AGV agv){
        if (taskStatus[0] && !agv.isLastTaskBuffer()) {
//            if(!agv.isFixAgvTagErrorCompleted()) OLD
            if(agv.getStatus() == AGV.Status.WRONG_TAG_NUMBER){
                fixAgvTagError(agv);
            }
        } else if (taskStatus[0] && agv.isLastTaskBuffer()) {
            agv.setTagError(false);
            agv.setFixAgvTagErrorCompleted(false);
            agv.setTagErrorDispatchCompleted(false);
            agv.setLastTaskBuffer(false);
        } else if (!taskStatus[0]) {
            if(!agv.isTagErrorDispatchCompleted() || taskStatus[7]){
                try{
                    tagErrorDispatch(agv);
                } catch (IllegalStateException e){
                    e.printStackTrace();
                }
            }
            agv.setLastTaskBuffer(true);

        }

    }

    private void handleFailedTask(AGV agv){
        if(!processTasks.getIsRetrying() && agv.getTask()!=null){
            int reDispatchCount = agv.getReDispatchCount();
            if(reDispatchCount < 3) {
                notificationDao.insertMessage(NotificationDao.Title.AGV_SYSTEM, NotificationDao.Status.FAILED_EXECUTION_TASK);
                processTasks.dispatchTaskToAGV(agv);
                agv.setReDispatchCount(reDispatchCount);
            } else if (reDispatchCount == 3) {
                processTasks.failedTask(agv);
                agv.setReDispatchCount(0);
            }
        }
    }

    private void tagErrorDispatch(AGV agv){
        if(processTasks.dispatchTaskToAGV(agv)) {
            agv.setTagErrorDispatchCompleted(true);
        } else {
            throw new IllegalStateException("Unexpected Path!");
        }
    }

    private void handleExecutingTask(AGV agv){
        if(agv.getTask().getStatus() == 1){
            agv.getTask().setStatus(2);
            AGVQTask task = agv.getTask();
            taskDetailDao.updateStatusByTaskNumberAndSequence(task.getTaskNumber(), task.getSequence(), 2);
        }
        if(!agv.getTask().getTaskNumber().matches("#(SB|LB).*")){
            CountUtilizationRate.isWorking[agv.getId()-1] = true;
        }
    }
    private void handleCompletedTask(AGV agv, NotificationDao.Title agvTitle){
        if(agv.getTaskStatus() == AGV.TaskStatus.PRE_START_STATION || agv.getTask().getStatus() == 1){
            return;
        }
        CountUtilizationRate.isWorking[agv.getId()-1] = false;
        if(agv.getTask() != null) {
            processTasks.completedTask(agv, agvTitle);
            agv.setReDispatchCount(0);
        }
    }

    private void updateAgvStatus(AGV agv, String data, NotificationDao.Title agvTitle){
        switch (Integer.parseInt(data) / 10) {
            case 0 -> {
                switch (Integer.parseInt(data) % 10) {
                    case 0 -> {
                        // AGV 重新啟動
                        if(agv.getStatus() != AGV.Status.REBOOT){
                            agv.setStatus(AGV.Status.REBOOT);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.REBOOT);
                            homePageService.setIAlarm(0);
                        }
                    }
                    case 1 -> {
                        // AGV 手動模式
                        if(agv.getStatus() != AGV.Status.MANUAL){
                            agv.setStatus(AGV.Status.MANUAL);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.MANUAL);
                            homePageService.setIAlarm(0);
                        }
                    }
                    case 2 -> {
                        // AGV 連線中(自動上位模式)
                        if(agv.getStatus() != AGV.Status.ONLINE){
                            agv.setStatus(AGV.Status.ONLINE);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.ONLINE);
                            homePageService.setIAlarm(0);
                        }
                    }
                    default -> {
                        // 系統異常資料
                        if(agv.getStatus() != AGV.Status.ERROR_AGV_DATA){
                            agv.setStatus(AGV.Status.ERROR_AGV_DATA);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.ERROR_AGV_DATA);
                            homePageService.setIAlarm(0);
                        }
                        System.out.println("異常agv狀態資料");
                    }
                }
            }
            case 1 -> {
                // AGV 緊急停止
                if(agv.getStatus() != AGV.Status.STOP){
                    agv.setStatus(AGV.Status.STOP);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.STOP);
                    homePageService.setIAlarm(1);
                }
            }
            case 2 -> {
                // AGV 出軌
                if(agv.getStatus() != AGV.Status.DERAIL){
                    agv.setStatus(AGV.Status.DERAIL);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.DERAIL);
                    homePageService.setIAlarm(1);
                }
            }
            case 3 -> {
                // AGV 發生碰撞
                if(agv.getStatus() != AGV.Status.COLLIDE){
                    agv.setStatus(AGV.Status.COLLIDE);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.COLLIDE);
                    homePageService.setIAlarm(1);
                }
            }
            case 4 -> {
                // AGV 前有障礙
                if(agv.getStatus() != AGV.Status.OBSTACLE){
                    agv.setStatus(AGV.Status.OBSTACLE);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.OBSTACLE);
                }
            }
            case 5 -> {
                // AGV 轉向角度過大
                if(agv.getStatus() != AGV.Status.EXCESSIVE_TURN_ANGLE){
                    agv.setStatus(AGV.Status.EXCESSIVE_TURN_ANGLE);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.EXCESSIVE_TURN_ANGLE);
                    homePageService.setIAlarm(0);
                }
            }
            case 6 -> {
                // AGV 卡號錯誤
                if(agv.getStatus() != AGV.Status.WRONG_TAG_NUMBER){
                    agv.setStatus(AGV.Status.WRONG_TAG_NUMBER);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.WRONG_TAG_NUMBER);
                    agv.setTagError(true);
                    homePageService.setIAlarm(0);
                }
            }
            case 7 -> {
                // AGV 未知卡號
                if(agv.getStatus() != AGV.Status.UNKNOWN_TAG_NUMBER){
                    agv.setStatus(AGV.Status.UNKNOWN_TAG_NUMBER);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.UNKNOWN_TAG_NUMBER);
                    homePageService.setIAlarm(1);
                }
            }
            case 8 -> {
                // AGV 異常排除
                if(agv.getStatus() != AGV.Status.EXCEPTION_EXCLUSION){
                    agv.setStatus(AGV.Status.EXCEPTION_EXCLUSION);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.EXCEPTION_EXCLUSION);
                    homePageService.setIAlarm(0);
                }
            }
            case 9 -> {
                // AGV 感知器偵測異常
                if(agv.getStatus() != AGV.Status.SENSOR_ERROR){
                    agv.setStatus(AGV.Status.SENSOR_ERROR);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.SENSOR_ERROR);
                    homePageService.setIAlarm(0);
                }
            }
            case 10 -> {
                // AGV 充電異常
                if(agv.getStatus() != AGV.Status.CHARGE_ERROR){
                    agv.setStatus(AGV.Status.CHARGE_ERROR);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.CHARGE_ERROR);
                    homePageService.setIAlarm(0);
                }
            }
            default -> {
                // 系統異常資料
                if(agv.getStatus() != AGV.Status.ERROR_AGV_DATA){
                    agv.setStatus(AGV.Status.ERROR_AGV_DATA);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.ERROR_AGV_DATA);
                    homePageService.setIAlarm(0);
                }
                System.out.println("異常agv狀態資料");
            }
        }
    }


    private boolean iCon = true;
    public Optional<String[]> crawlAGVStatus() {
        Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agvUrl + "/cars"))
                .GET()
                .timeout(timeout)
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String webpageContent = response.body();
            String[] data = Arrays.stream(webpageContent.split("<br>"))
                    .map(String::trim)
                    .toArray(String[]::new);
            iCon=true;
            return Optional.of(data);
        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
            CountUtilizationRate.isPoweredOn = new boolean[agvIdDao.queryAGVList().size()];
            CountUtilizationRate.isWorking = new boolean[agvIdDao.queryAGVList().size()];
            if(iCon){
                System.out.println("AGV 控制系統未連線");
                notificationDao.insertMessage(NotificationDao.Title.AGV_SYSTEM, NotificationDao.Status.OFFLINE);
                iCon=false;
            }
        }

        return Optional.empty();
    }

    private void fixAgvTagError(AGV agv) {
        Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agvUrl + "/cmd=" + agv.getId() + "&QJ0131X"))
//                .uri(URI.create(agvUrl + "/cmd=" + agv.getId() + "&QJ0130X"))
                .GET()
                .timeout(timeout)
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String webpageContent = response.body();
            if(webpageContent.trim().equals("OK")) {
                agv.setFixAgvTagErrorCompleted(true);
            }
        } catch (IOException | InterruptedException ignored) {
        }
    }


    public boolean[] parseAGVStatus(int statusValue) {
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
