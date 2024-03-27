package com.yid.agv.backend;

import com.yid.agv.backend.agv.AGVManager;
import com.yid.agv.backend.agv.AGV;
import com.yid.agv.backend.agvtask.AGVQTask;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.backend.agvtask.AGVTaskManager;
import com.yid.agv.model.Station;
import com.yid.agv.repository.*;
import com.yid.agv.service.HomePageService;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
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
import java.util.stream.Collectors;

@Component
public class AGVInstantStatus {
    private static final Logger log = LoggerFactory.getLogger(AGVInstantStatus.class);
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
    @Value("${agv.task_exception_option}")
    private int TASK_EXCEPTION_OPTION;
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
    private GridManager gridManager;
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
        // 從 Traffic Control 抓取 AGV 狀態，並更新到 agv
        String[] allAgvInstantStatuses = crawlAGVStatus().orElse(new String[0]);
//        String[] allAgvInstantStatuses = new String[]{"2,2001,115,100,128,2","1,1019,115,100,128,11","3,3073,115,100,128,2"};

        if (allAgvInstantStatuses.length == 0) {
            // 資料錯誤時，通常不應該進入到這邊
            agvManager.getAgvs().forEach(this::updateAGVOfflineStatus);
            return;
        }

        agvManager.getAgvs().forEach(agv -> {
            String[] identifiedAgvData = null;
            // 比對資料，取出正確 AGV 資料
            for (String agvInstantStatus : allAgvInstantStatuses) {
                String[] unidentifiedAgvData = agvInstantStatus.split(",");  // 分隔 Traffic Control 資料
                if (unidentifiedAgvData[0].trim().equals(Integer.toString(agv.getId()))) {
                    identifiedAgvData = unidentifiedAgvData;
                }
            }
            // 若正確取得，更新到 AGV 實例；反之設為離線。
            if (identifiedAgvData == null) {
                updateAGVOfflineStatus(agv);
            } else {
                updateAGVOnlineStatus(agv, identifiedAgvData);
                updateTaskStatus(agv);
            }
        });
    }

    private void updateTaskStatus(AGV agv){
        switch (agv.getTaskStatus()){
            case NO_TASK -> {
            }
            case PRE_START_STATION -> {
                Integer startStation = agv.getTask().getStartStationId();
                if (startStation != null && startStation != 0 && agv.getPlace().equals(Integer.toString(stationIdTagMap.get(startStation)))) {
//                    if (agv.getTask().getStartStation().startsWith("E-")){  // 3F->1F
//                        gridManager.setGridStatus(task.getTerminalStationId(), Grid.Status.OCCUPIED);  // Booked to Occupied
//                    } else if (taskTerminalStation.startsWith("E-")){  // 1F->3F
//                        gridManager.setGridStatus(task.getStartStationId(), Grid.Status.FREE);  // Booked to Free
//                    }
                    // 處理
//                    gridManager.setGridStatus(startStation, Grid.Status.FREE);
                    agv.setTaskStatus(AGV.TaskStatus.PRE_TERMINAL_STATION);
                }
            }
            case PRE_TERMINAL_STATION -> {
                Integer terminalStation = agv.getTask().getTerminalStationId();
                if (terminalStation != null && terminalStation != 0 && agv.getPlace().equals(Integer.toString(stationIdTagMap.get(terminalStation)))) {
//                    gridManager.setGridStatus(terminalStation, Grid.Status.OCCUPIED);
                    agv.setTaskStatus(AGV.TaskStatus.COMPLETED);
                }
            }
        }
    }

    private void updateAGVOfflineStatus(AGV agv){
        if(agv.getStatus() != AGV.Status.OFFLINE){
            notificationDao.insertMessage(agv.getTitle(), NotificationDao.Status.OFFLINE);
            agv.setStatus(AGV.Status.OFFLINE);
            agv.setSignal(0);
            agv.setBattery(0);
            agv.setIAlarm(false);
        }
    }

    private void updateAGVOnlineStatus(AGV agv, String[] data){
        // data[1] 位置
        agv.setPlace(data[1].trim());
        // data[2] 訊號
        agv.setSignal((int)Math.ceil(Integer.parseInt(data[2].trim()) / 120.0 * 100));
        // data[3] 電量
        agv.setBattery(Integer.parseInt(data[3].trim()));
        if(agv.getBattery()<LOW_BATTERY){
            if(agv.getLowBatteryCount()>LOW_BATTERY_DURATION){
                if(!agv.isILowBattery()){
                    notificationDao.insertMessage(agv.getTitle(), NotificationDao.Status.BATTERY_TOO_LOW);
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
        updateAgvStatus(agv, data[5].trim());

        // data[4] 任務狀態
        boolean[] taskStatus = parseAGVStatus(Integer.parseInt(data[4].trim()));
        agv.setIScan(taskStatus[4]);  // AMR 背後看站板的 sensor

        if (agv.isTagError()){
            handleTagError(parseAGVStatus(Integer.parseInt(data[4].trim())), agv);
        } else if (agv.getStatus() == AGV.Status.ONLINE && agv.getTask() != null){
            agv.setObstacleCount(0);
            if (taskStatus[7]) {
                handleFailedTask(agv);
            } else {
                if (taskStatus[0]) {
                    handleExecutingTask(agv);
                } else {
                    handleNotExecutingTask(agv);
                }
            }
        } else if (agv.getStatus() == AGV.Status.OBSTACLE) { // 若前有障礙時
            if (agv.getObstacleCount() < OBSTACLE_DURATION) {
                agv.setObstacleCount(agv.getObstacleCount()+1);
                agv.setIAlarm(false);
            } else {
                agv.setIAlarm(true);
            }
        }

    }

    private void handleTagError(boolean @NotNull [] taskStatus, AGV agv){
        if (taskStatus[0] && !agv.isLastTaskBuffer()) {
//            if(!agv.isFixAgvTagErrorCompleted()) OLD
            if(agv.getStatus() == AGV.Status.WRONG_TAG_NUMBER){
                fixAgvTagError(agv);
            }
        } else if (taskStatus[0]) {  // && agv.isLastTaskBuffer()
            agv.setTagError(false);
            agv.setFixAgvTagErrorCompleted(false);
            agv.setTagErrorDispatchCompleted(false);
            agv.setLastTaskBuffer(false);
        } else {  // !taskStatus[0]
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
                agv.setReDispatchCount(++reDispatchCount);
            } else if (reDispatchCount == 3) {
                processTasks.failedTask(agv);
                agv.setReDispatchCount(0);
            }
        }
    }

    private void tagErrorDispatch(AGV agv){
        if(processTasks.dispatchTaskToAGV(agv).equals("OK")) {
            agv.setTagErrorDispatchCompleted(true);
        } else {
            throw new IllegalStateException("Unexpected Path!");
        }
    }

    private void handleExecutingTask(AGV agv){
        AGVQTask task = agv.getTask();
        if(task.getStatus() == 1){
            task.setStatus(2);
            taskDetailDao.updateStatusByTaskNumberAndSequence(task.getTaskNumber(), task.getSequence(), 2);
        }
    }
    private void handleNotExecutingTask(AGV agv){
//        if(agv.getTaskStatus() == AGV.TaskStatus.PRE_START_STATION || agv.getTask().getStatus() == 1){
        if (agv.getTask().getStatus() == 1) {
            return;
        }
        AGVQTask task = agv.getTask();

        if (agv.getPlace().equals(stationDao.getStationTagByGridName(task.getTerminalStation()))) {
            // handleCompletedTask
            processTasks.completedTask(agv);
        } else {
            // 刪除任務或重派任務
            switch (TASK_EXCEPTION_OPTION){
                case 0 -> processTasks.failedTask(agv);
                case 1 -> {
                    if (agv.getTaskStatus() == AGV.TaskStatus.PRE_TERMINAL_STATION) {
                        // 檢查 AGV 車上是否有棧板。有，則繼續派遣；無，則刪除任務。
                        if (agv.isIScan()) {
                            processTasks.dispatchTaskToAGV(agv);
                        } else {
                            processTasks.failedTask(agv);
                        }
                    } else {
                        processTasks.dispatchTaskToAGV(agv);
                    }
                }
                default -> log.warn("TASK_EXCEPTION_OPTION值錯誤");
            }
        }
    }

    private void updateAgvStatus(AGV agv, String data){
        NotificationDao.Title agvTitle = agv.getTitle();
        switch (Integer.parseInt(data) / 10) {
            case 0 -> {
                switch (Integer.parseInt(data) % 10) {
                    case 0 -> {
                        // AGV 重新啟動
                        if(agv.getStatus() != AGV.Status.REBOOT){
                            agv.setStatus(AGV.Status.REBOOT);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.REBOOT);
                            agv.setIAlarm(false);
                        }
                    }
                    case 1 -> {
                        // AGV 手動模式
                        if(agv.getStatus() != AGV.Status.MANUAL){
                            agv.setStatus(AGV.Status.MANUAL);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.MANUAL);
                            agv.setIAlarm(false);
                        }
                    }
                    case 2 -> {
                        // AGV 連線中(自動上位模式)
                        if(agv.getStatus() != AGV.Status.ONLINE){
                            agv.setStatus(AGV.Status.ONLINE);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.ONLINE);
                            agv.setIAlarm(false);
                        }
                    }
                    default -> {
                        // 系統異常資料
                        if(agv.getStatus() != AGV.Status.ERROR_AGV_DATA){
                            agv.setStatus(AGV.Status.ERROR_AGV_DATA);
                            notificationDao.insertMessage(agvTitle, NotificationDao.Status.ERROR_AGV_DATA);
                            agv.setIAlarm(false);
                        }
                        log.warn("異常agv狀態資料");
                    }
                }
            }
            case 1 -> {
                // AGV 緊急停止
                if(agv.getStatus() != AGV.Status.STOP){
                    agv.setStatus(AGV.Status.STOP);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.STOP);
                    agv.setIAlarm(true);
                }
            }
            case 2 -> {
                // AGV 出軌
                if(agv.getStatus() != AGV.Status.DERAIL){
                    agv.setStatus(AGV.Status.DERAIL);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.DERAIL);
                    agv.setIAlarm(true);
                }
            }
            case 3 -> {
                // AGV 發生碰撞
                if(agv.getStatus() != AGV.Status.COLLIDE){
                    agv.setStatus(AGV.Status.COLLIDE);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.COLLIDE);
                    agv.setIAlarm(true);
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
                    agv.setIAlarm(true);
                }
            }
            case 6 -> {
                // AGV 卡號錯誤
                if(agv.getStatus() != AGV.Status.WRONG_TAG_NUMBER){
                    agv.setStatus(AGV.Status.WRONG_TAG_NUMBER);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.WRONG_TAG_NUMBER);
//                    agv.setTagError(true);  是否處理卡號錯誤
                    agv.setIAlarm(true);
                }
            }
            case 7 -> {
                // AGV 未知卡號
                if(agv.getStatus() != AGV.Status.UNKNOWN_TAG_NUMBER){
                    agv.setStatus(AGV.Status.UNKNOWN_TAG_NUMBER);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.UNKNOWN_TAG_NUMBER);
                    agv.setIAlarm(true);
                }
            }
            case 8 -> {
                // AGV 異常排除
                if(agv.getStatus() != AGV.Status.EXCEPTION_EXCLUSION){
                    agv.setStatus(AGV.Status.EXCEPTION_EXCLUSION);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.EXCEPTION_EXCLUSION);
                    agv.setIAlarm(false);
                }
            }
            case 9 -> {
                // AGV 感知器偵測異常
                if(agv.getStatus() != AGV.Status.SENSOR_ERROR){
                    agv.setStatus(AGV.Status.SENSOR_ERROR);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.SENSOR_ERROR);
                    agv.setIAlarm(false);
                }
            }
            case 10 -> {
                // AGV 充電異常
                if(agv.getStatus() != AGV.Status.CHARGE_ERROR){
                    agv.setStatus(AGV.Status.CHARGE_ERROR);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.CHARGE_ERROR);
                    agv.setIAlarm(false);
                }
            }
            case 11 -> {
                // AMR 迷航
                if(agv.getStatus() != AGV.Status.NAVIGATION_LOST){
                    agv.setStatus(AGV.Status.NAVIGATION_LOST);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.NAVIGATION_LOST);
                    agv.setIAlarm(true);
                }
            }
            default -> {
                // 系統異常資料
                if(agv.getStatus() != AGV.Status.ERROR_AGV_DATA){
                    agv.setStatus(AGV.Status.ERROR_AGV_DATA);
                    notificationDao.insertMessage(agvTitle, NotificationDao.Status.ERROR_AGV_DATA);
                    agv.setIAlarm(false);
                }
                log.warn("異常agv狀態資料");
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
            if(iCon){
                log.warn("Traffic Control is not connected");
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
