package com.yid.agv.backend;

import com.yid.agv.backend.agv.AGV;
import com.yid.agv.backend.agv.AGVManager;
import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.backend.agvtask.AGVTaskManager;
import com.yid.agv.backend.agvtask.AGVQTask;
import com.yid.agv.backend.tasklist.TaskListManager;
import com.yid.agv.model.GridList;
import com.yid.agv.model.NowTaskList;
import com.yid.agv.model.Station;
import com.yid.agv.repository.*;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProcessAGVTask {
    private static final Logger log = LoggerFactory.getLogger(ProcessAGVTask.class);
    @Value("${agvControl.url}")
    private String agvUrl;
    @Value("${http.timeout}")
    private int HTTP_TIMEOUT;
    @Value("${http.max_retry}")
    private int MAX_RETRY;

    private final StationDao stationDao;
    private final AnalysisDao analysisDao;
    private final TaskDetailDao taskDetailDao;
    private final NowTaskListDao nowTaskListDao;
    private final NotificationDao notificationDao;
    private final GridListDao gridListDao;
    private final WorkNumberDao workNumberDao;
    private final AGVTaskManager AGVTaskManager;
    private final AGVManager agvManager;
    private final GridManager gridManager;
    private final TaskListManager taskListManager;
    private final Map<Integer, Integer> stationIdTagMap;

    public ProcessAGVTask(StationDao stationDao,
                        AnalysisDao analysisDao,
                        TaskDetailDao taskDetailDao,
                        NowTaskListDao nowTaskListDao,
                        NotificationDao notificationDao,
                        GridListDao gridListDao,
                        WorkNumberDao workNumberDao,
                        AGVTaskManager AGVTaskManager,
                        AGVManager agvManager,
                        GridManager gridManager,
                        TaskListManager taskListManager) {
        this.stationDao = stationDao;
        this.analysisDao = analysisDao;
        this.taskDetailDao = taskDetailDao;
        this.nowTaskListDao = nowTaskListDao;
        this.notificationDao = notificationDao;
        this.gridListDao = gridListDao;
        this.workNumberDao = workNumberDao;
        this.AGVTaskManager = AGVTaskManager;
        this.agvManager = agvManager;
        this.gridManager = gridManager;
        this.taskListManager = taskListManager;
        this.stationIdTagMap = stationDao.queryStations().stream().
                collect(Collectors.toMap(Station::getId, Station::getTag));
    }

    @Scheduled(fixedRate = 1000)
    public void dispatchTasks() {
        if (isRetrying) return;

        //noinspection CommentedOutCode
        agvManager.getAgvs().forEach(agv -> {
            if(agv.getStatus() != AGV.Status.ONLINE) return;  // AGV未連線則無法派遣
            if(agv.getTask() != null) return;  // AGV任務中

//            boolean iAtStandbyStation = iEqualsStandbyStation(agv.getPlace());
            boolean iAtStandbyStation = agvManager.iAgvInStandbyStation(agv.getId());
            boolean taskQueueIEmpty = AGVTaskManager.isEmpty(agv.getId());

            boolean hasNextTaskList = false;
            if (agv.getId() == 2) {
                List<NowTaskList> taskLists = nowTaskListDao.queryNowTaskListsByProcessId(2);
                if (!taskLists.isEmpty()) hasNextTaskList = true;
            } else if (agv.getId() == 3) {
                NowTaskList nowTaskList = taskListManager.getNowTaskListByTaskProcessId(1);
                if (nowTaskList != null && nowTaskList.getTaskNumber().startsWith("#RE") && (nowTaskList.getPhase() == Phase.FIRST_STAGE_3F || nowTaskList.getPhase() == Phase.CALL_ELEVATOR)) {
                    hasNextTaskList = true;
                }
            }

            if (!taskQueueIEmpty){  // 正常派遣
                // 這個專案不用寫優先派遣邏輯
                AGVQTask goTask = AGVTaskManager.peekNewTaskByAGVId(agv.getId());
                log.info("Process dispatch...");
                String result = dispatchTaskToAGV(agv, goTask);
                switch (result) {
                    case "OK" -> {
                        AGVTaskManager.getNewTaskByAGVId(agv.getId());
                        agv.setTask(goTask);
                        Objects.requireNonNull(goTask).setStatus(1);
                        agv.setTaskStatus( goTask.getModeId() == 2 ? AGV.TaskStatus.PRE_TERMINAL_STATION : AGV.TaskStatus.PRE_START_STATION );
                        taskDetailDao.updateStatusByTaskNumberAndSequence(goTask.getTaskNumber(), goTask.getSequence(), 1);
                    }
                    case "BUSY" -> {}
                    case "FAIL" -> {
                        AGVTaskManager.getNewTaskByAGVId(agv.getId());
                        agv.setTask(goTask);
                        failedTask(agv);
                    }
                    default -> log.warn("dispatchTaskToAGV result exception: {}", result);
                }
            } else if (!iAtStandbyStation && !hasNextTaskList){  // 派遣回待命點
                goStandbyTask(agv);
            }

//            if (agv.isILowBattery() && !iAtStandbyStation) {  // 低電量時，派遣回待命點
//                goStandbyTask(agv);
//            } else if (!taskQueueIEmpty && !agv.isILowBattery()){  // 正常派遣
//                // 這個專案不用寫優先派遣邏輯
//                AGVQTask goTask = AGVTaskManager.peekNewTaskByAGVId(agv.getId());
//                log.info("Process dispatch...");
//                String result = dispatchTaskToAGV(agv, goTask);
//                switch (result) {
//                    case "OK" -> {
//                        AGVTaskManager.getNewTaskByAGVId(agv.getId());
//                        agv.setTask(goTask);
//                        Objects.requireNonNull(goTask).setStatus(1);
//                        agv.setTaskStatus(AGV.TaskStatus.PRE_START_STATION);
//                        taskDetailDao.updateStatusByTaskNumberAndSequence(goTask.getTaskNumber(), goTask.getSequence(), 1);
//                    }
//                    case "BUSY" -> {}
//                    case "FAIL" -> {
//                        AGVTaskManager.getNewTaskByAGVId(agv.getId());
//                        agv.setTask(goTask);
//                        failedTask(agv);
//                    }
//                    default -> log.warn("dispatchTaskToAGV result exception: " + result);
//                }
//            } else if (taskQueueIEmpty && !iAtStandbyStation && !hasNextTaskList){  // 派遣回待命點
//                goStandbyTask(agv);
//            }
        });

    }

    private boolean isRetrying = false;
    public synchronized String dispatchTaskToAGV(AGV agv, AGVQTask task) {
        int retryCount = 0;
        while (retryCount < MAX_RETRY) {
            try {
                // Dispatch the task to the Traffic Control
                if (task == null) return null;

                String nowPlace = agv.getPlace();
                if(nowPlace.equals(Integer.toString(stationIdTagMap.get(task.getTerminalStationId())))){  // 主要是為了防止派遣回待命點時，出現無限輪迴。
                    failedTask(agv);
                    isRetrying = false;
                    return "FAIL";
                }
                String url;
                if (task.getTaskNumber().matches("#(SB|LB).*") || agv.getTaskStatus() == AGV.TaskStatus.PRE_TERMINAL_STATION || task.getModeId() == 2){
                    url = agvUrl + "/task0=" + task.getAgvId() + "&" + task.getModeId() + "&" + nowPlace +
                            "&" + stationIdTagMap.get(task.getTerminalStationId());
                } else if (task.getTaskNumber().startsWith("#YE")|| task.getTaskNumber().startsWith("#RE") || task.getTaskNumber().startsWith("#NE")){
                    url = agvUrl + "/task0=" + task.getAgvId() + "&" + task.getModeId() + "&" + nowPlace +
                            "&" + stationIdTagMap.get(task.getStartStationId()) + "&" + stationIdTagMap.get(task.getTerminalStationId());
                } else {
                    url = agvUrl + "/task0=" + task.getAgvId() + "&" + task.getModeId() + "&" + nowPlace +
                            "&" + stationIdTagMap.get(task.getStartStationId()) + "&" + stationIdTagMap.get(task.getTerminalStationId());
                }

                log.info("Dispatch URL: {}", url);

                Duration timeout = Duration.ofSeconds(HTTP_TIMEOUT);
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(timeout)
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String webpageContent = response.body().trim();

                switch (webpageContent) {
                    case "OK" -> {
                        log.info("Task number {} has been dispatched.", task.getTaskNumber());
                        isRetrying = false;
                        return "OK";
                    }
                    case "BUSY" -> {
                        log.info("Send task failed: BUSY");
                        isRetrying = false;
                        return "BUSY";
                    }
                    case "FAIL" -> {
                        isRetrying = true;
                        retryCount++;
                        notificationDao.insertMessage(NotificationDao.Title.AGV_SYSTEM, NotificationDao.Status.FAILED_SEND_TASK);
                        log.warn("Failed to dispatch task, retrying... (Attempt {})", retryCount);
                        try {
                            // noinspection BusyWait
                            Thread.sleep(3000); // 延遲再重新發送
                        } catch (InterruptedException ignored) {
                        }
                    }
                    default -> {
                        log.warn("TrafficControl result exception: {}", webpageContent);
                        notificationDao.insertMessage(NotificationDao.Title.AGV_SYSTEM, NotificationDao.Status.ERROR_AGV_DATA);
                        isRetrying = false;
                        return "FAIL";
                    }
                }
            } catch (IOException | InterruptedException e) {
                log.warn("發送任務發生意外，3秒後重新發送");
                notificationDao.insertMessage(NotificationDao.Title.AGV_SYSTEM, NotificationDao.Status.FAILED_SEND_TASK);
                isRetrying = true;
                retryCount++;
                log.warn("Failed to dispatch task, retrying... (Attempt {})", retryCount);
                try {
                    // noinspection BusyWait
                    Thread.sleep(3000); // 延遲再重新發送
                } catch (InterruptedException ignored) {
                }
            }
        }
        log.warn("Failed to dispatch task after {} attempts.", MAX_RETRY);
        log.warn("任務發送三次皆失敗，已取消任務");
//        failedTask(agv);
        notificationDao.insertMessage(NotificationDao.Title.AGV_SYSTEM, NotificationDao.Status.FAILED_SEND_TASK_THREE_TIMES);
        isRetrying = false;
        return "FAIL";
    }

    public synchronized String dispatchTaskToAGV(AGV agv) {
        return dispatchTaskToAGV(agv, agv.getTask());
    }

    public void failedTask(AGV agv) {
        AGVQTask task = agv.getTask();
        if (task == null) return;
        log.info("Failed task:{}", task);
        if (task.getStartStation().matches("\\d+-T-\\d+")) {
            gridManager.setGridStatus(task.getStartStationId(), Grid.Status.FREE);
        } else {
            gridManager.setGridStatus(task.getStartStationId(), Grid.Status.OCCUPIED);
        }
        gridManager.setGridStatus(task.getTerminalStationId(), Grid.Status.FREE);
        notificationDao.insertMessage(agv.getTitle(), "Failed task " + task.getTaskNumber() + ":" + task.getSequence());
        taskDetailDao.updateStatusByTaskNumberAndSequence(task.getTaskNumber(), task.getSequence(), -1);
        agv.setTaskStatus(AGV.TaskStatus.NO_TASK);
        agv.setReDispatchCount(0);
        agv.setTask(null);
    }

    public void completedTask(AGV agv){
        AGVQTask task = agv.getTask();
        if(!task.getTaskNumber().matches("#(SB|LB).*")){
            int analysisId = analysisDao.getTodayAnalysisId().get(task.getAgvId() - 1).getAnalysisId();
            analysisDao.updateTask(analysisDao.queryAnalysisByAnalysisId(analysisId).getTask() + 1, analysisId);
//            String taskStartStation = gridManager.getGridNameByStationId(task.getStartStationId());
            String taskStartStation = task.getStartStation();
//            String taskTerminalStation = gridManager.getGridNameByStationId(task.getTerminalStationId());
            String taskTerminalStation = task.getTerminalStation();

            switch (task.getAgvId()){
                case 1 -> {
                    if (taskStartStation.startsWith("E-")){  // 3F->1F
                        gridManager.setGridStatus(task.getTerminalStationId(), Grid.Status.OCCUPIED);  // Booked to Occupied
                    } else if (taskTerminalStation.startsWith("E-")){  // 1F->3F
                        gridManager.setGridStatus(task.getStartStationId(), Grid.Status.FREE);  // Booked to Free
                    }
                }
                case 2 -> {
                    gridManager.setGridStatus(task.getStartStationId(), Grid.Status.FREE);  // Booked to Free
                    gridManager.setGridStatus(task.getTerminalStationId(), Grid.Status.OCCUPIED);  // Booked to Occupied
                }
                case 3 -> {
                    if (taskStartStation.startsWith("3-R-")) {  // 3F->1F
                        gridManager.setGridStatus(task.getStartStationId(), Grid.Status.FREE);  // Booked to Free
                    } else if (!taskStartStation.startsWith("E-")){
                        gridManager.setGridStatus(task.getTerminalStationId(), Grid.Status.OCCUPIED);  // Booked to Occupied
                    } else if (taskTerminalStation.startsWith("3-A-")) {
                        gridManager.setGridStatus(task.getTerminalStationId(), Grid.Status.OCCUPIED);  // Booked to Occupied
                    } else if (taskStartStation.startsWith("E-") && !taskTerminalStation.startsWith("3-T-")){
                        gridManager.setGridStatus(task.getTerminalStationId(), Grid.Status.OCCUPIED);  // Booked to Occupied
                    }
                }
            }
            if(!taskTerminalStation.startsWith("E-")){
                GridList completedTaskGrid = gridListDao.queryGridByGridName(taskTerminalStation);
                String workNumber1 = completedTaskGrid.getWorkNumber_1();
                String workNumber2 = completedTaskGrid.getWorkNumber_2();
                String workNumber3 = completedTaskGrid.getWorkNumber_3();
                String workNumber4 = completedTaskGrid.getWorkNumber_4();
                if(workNumber1!=null && workNumber1.matches("^[A-Za-z0-9]{4}-\\d{11}$")){
                    workNumberDao.insertWorkNumber(workNumber1);
                }
                if(workNumber2!=null && workNumber2.matches("^[A-Za-z0-9]{4}-\\d{11}$")){
                    workNumberDao.insertWorkNumber(workNumber2);
                }
                if(workNumber3!=null && workNumber3.matches("^[A-Za-z0-9]{4}-\\d{11}$")){
                    workNumberDao.insertWorkNumber(workNumber3);
                }
                if(workNumber4!=null && workNumber4.matches("^[A-Za-z0-9]{4}-\\d{11}$")){
                    workNumberDao.insertWorkNumber(workNumber4);
                }
            }
            taskListManager.setTaskListProgressBySequence(task.getTaskNumber(), task.getSequence());
        }
        log.info("Completed task {}:{}.", task.getTaskNumber(), task.getSequence());
        notificationDao.insertMessage(agv.getTitle(), "Completed task " + task.getTaskNumber() + ":" + task.getSequence());
        taskDetailDao.updateStatusByTaskNumberAndSequence(task.getTaskNumber(), task.getSequence(), 100);
        agv.setTaskStatus(AGV.TaskStatus.NO_TASK);
        agv.setReDispatchCount(0);
        agv.setTask(null);
    }

    public void goStandbyTask(AGV agv){
//        int place = Integer.parseInt(agv.getPlace());
        // 這個專案待命點不用選擇(低電量時)
        String agvIdPrefix = String.valueOf(agv.getId());
        List<Station> standbyStations = stationDao.queryStandbyStations();

        Optional<Integer> standbyStation = standbyStations.stream()
                .filter(station -> station.getName().startsWith(agvIdPrefix))
                .map(Station::getId)
                .findFirst();

        if(standbyStation.isEmpty()) {
            throw new RuntimeException();
        }

        String standbyStationName = gridManager.getGridNameByStationId(standbyStation.get());

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDateTime = now.format(formatter);

        String taskNumber = agv.isILowBattery() ? "#LB" + formattedDateTime + agv.getId() : "#SB" + formattedDateTime + agv.getId();

        AGVQTask toStandbyTask = new AGVQTask();
        toStandbyTask.setAgvId(agv.getId());
        toStandbyTask.setModeId(1);
        toStandbyTask.setStatus(1);
        toStandbyTask.setSequence(1);
        toStandbyTask.setTaskNumber(taskNumber);
        toStandbyTask.setStartStationId(standbyStation.get());
        toStandbyTask.setStartStation(standbyStationName);
        toStandbyTask.setTerminalStationId(standbyStation.get());
        toStandbyTask.setTerminalStation(standbyStationName);

        agv.setTask(toStandbyTask);

        TaskDetailDao.Title title = switch (agv.getId()){
            case 1 -> TaskDetailDao.Title.AMR_1;
            case 2 -> TaskDetailDao.Title.AMR_2;
            case 3 -> TaskDetailDao.Title.AMR_3;
            default -> throw new IllegalStateException("Unexpected value: " + agv.getId());
        };

        log.info("toStandbyTask: {}", toStandbyTask);

        taskDetailDao.insertTaskDetail(toStandbyTask.getTaskNumber(), title, toStandbyTask.getSequence(),
                Integer.toString(toStandbyTask.getStartStationId()), Integer.toString(toStandbyTask.getTerminalStationId()),
                TaskDetailDao.Mode.FORK_DN, formattedDateTime);
        dispatchTaskToAGV(agv);
    }
    public boolean getIsRetrying(){
        return isRetrying;
    }

}