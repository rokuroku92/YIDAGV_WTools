
package com.yid.agv.service;

import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.backend.tasklist.TaskListManager;
import com.yid.agv.dto.TaskListRequest;
import com.yid.agv.model.*;
import com.yid.agv.repository.GridListDao;
import com.yid.agv.repository.NowTaskListDao;
import com.yid.agv.repository.TaskDetailDao;
import com.yid.agv.repository.TaskListDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskListDao taskListDao;
    private final NowTaskListDao nowTaskListDao;
    private final TaskDetailDao taskDetailDao;
    private final TaskListManager taskListManager;
    private final GridListDao gridListDao;
    private final GridManager gridManager;
    private final JdbcTemplate jdbcTemplate;
    private String lastDate;

    public TaskService(TaskListDao taskListDao,
                       NowTaskListDao nowTaskListDao,
                       TaskDetailDao taskDetailDao,
                       TaskListManager taskListManager,
                       GridListDao gridListDao,
                       GridManager gridManager,
                       @Qualifier("WToolsJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.taskListDao = taskListDao;
        this.nowTaskListDao = nowTaskListDao;
        this.taskDetailDao = taskDetailDao;
        this.taskListManager = taskListManager;
        this.gridListDao = gridListDao;
        this.gridManager = gridManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    private @Nullable WorkNumberResult getWToolsInformation(@NotNull String workNumber) throws CannotGetJdbcConnectionException {
        if(workNumber.matches("^[A-Za-z0-9]{4}-\\d{11}$")){
            String[] TA = workNumber.split("-");
            String sql = "SELECT TA006 AS object_number, TA034 AS object_name FROM V_MOCTA WHERE TA001 = ? AND TA002 = ?";
            WorkNumberResult result;
            try {
                result = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(WorkNumberResult.class),TA[0],TA[1]);
            } catch (EmptyResultDataAccessException ignore) {
                return null;
            } catch (CannotGetJdbcConnectionException e) {
                throw e;
            } catch (DataAccessException e) {
                // 記錄其他數據訪問例外
                log.error("Data access exception when querying work number: {}", workNumber, e);
                return null;
            }
//            result.setObjectName("1/2\"72T電金全拋八角葫蘆柄軟打 KINCROME");
//            result.setObjectNumber("0254780011");
            return result;
        } else {
            return null;
        }
    }

//    public Collection<AGVQTask> getTaskQueue(){
//        return taskQueue.getTaskQueueCopy();
//    }
    public List<NowTaskListResponse> queryNowTaskLists(){
        return nowTaskListDao.queryNowTaskListsResult();
    }

    public List<TaskList> queryTaskLists(){
        return taskListDao.queryTaskLists();
    }
    public List<TaskList> queryAllTaskLists(){
        return taskListDao.queryAllTaskLists();
    }
    public List<TaskDetail> queryTaskDetailsByTaskNumber(String taskNumber){
        return taskDetailDao.queryTaskDetailsByTaskNumber(taskNumber);
    }
    public List<TaskDetail> queryAllTaskDetails(){
        return taskDetailDao.queryAllTaskDetails();
    }

    public String cancelTask(String taskNumber){
        for (NowTaskList nowTaskList : taskListManager.getNowAllTaskList()) {
            if(nowTaskList.getTaskNumber().equals(taskNumber)){
                if(nowTaskList.getProgress() != 0){
                    return "取消任務失敗： 任務已開始";
                }
            }
        }
        nowTaskListDao.deleteNowTaskList(taskNumber);
        taskListDao.cancelTaskList(taskNumber);
        List<TaskDetail> taskDetails = taskDetailDao.queryTaskDetailsByTaskNumber(taskNumber);
        taskDetails.forEach(taskDetail -> {
            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), -1);
            if(taskDetail.getStatus() != 100 && taskDetail.getMode()!=100 && taskDetail.getMode()!=101) {
                gridManager.setGridStatus(taskDetail.getStartId(), Grid.Status.FREE);
                gridManager.setGridStatus(taskDetail.getTerminalId(), Grid.Status.FREE);
                gridListDao.clearWorkOrder(taskDetail.getTerminalId());
            }
        });
        return "取消任務 ".concat(taskNumber).concat(" 成功！");
    }

    public String addTaskList(TaskListRequest taskListRequest){
        int taskSize = taskListRequest.getTasks().size();
        if (taskSize == 0){
            return "未輸入起始格位";
        }
        sortTasksByStartGrid(taskListRequest);
        System.out.println(taskListRequest.getTasks().get(0).getStartGrid());
        for (int i = 0; i < taskSize; i++) {
            if(gridManager.getGridStatus(taskListRequest.getTasks().get(i).getStartGrid()) != Grid.Status.FREE){
                return "起始格位非可用";
            }
        }

        AtomicReference<String> failWorkNumber = new AtomicReference<>();
        List<List<WorkNumberResult>> requestObjectData = new ArrayList<>();
        AtomicBoolean iFailedConnection = new AtomicBoolean(false);
        taskListRequest.getTasks().forEach(task -> {
            List<WorkNumberResult> objectData = new ArrayList<>();
            task.getWorkNumber().forEach(workNumber -> {
                WorkNumberResult result = null;
                try {
                    result = getWToolsInformation(workNumber);
                } catch (CannotGetJdbcConnectionException e) {
                    if (e.getCause() instanceof com.mchange.v2.resourcepool.TimeoutException) {
                        // 記錄超時例外
                        log.error("Connection pool timeout when querying work number: {}", workNumber, e);
                    } else {
                        // 記錄其他連接獲取異常
                        log.error("Cannot get JDBC connection when querying work number: {}", workNumber, e);
                    }
                    iFailedConnection.set(true);
                }
                if(result == null){
                    failWorkNumber.set(workNumber);
                } else {
                    objectData.add(result);
                }
            });
            requestObjectData.add(objectData);
        });

        if(iFailedConnection.get()) {
            return "ERP 資料庫連接失敗，請稍後再試！";
        }
        if(failWorkNumber.get() != null){
            return "工單號碼： ".concat(failWorkNumber.get()).concat("輸入錯誤！");
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDateTime = currentDateTime.format(formatter);
        int step = 0;
        switch (taskListRequest.getMode()){  // 1: 1F->3F | 2: 2F->2F | 3: 3F->1F
            case 1 -> {
                switch (taskListRequest.getTerminal()) {
                    case "A" -> {
                        String area = "3-" + taskListRequest.getTerminal();
                        List<Grid> availableGrids = gridManager.getAvailableGrids(area);

                        if(availableGrids.size() < taskSize){
                            return "終點區域格位已滿";
                        }

                        String taskNumber = "#YE" + getPureTaskNumber();
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.CALL_ELEVATOR, formattedDateTime);
                        for (int i = 0; i < taskSize; i++) {
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_1, ++step,
                                    Integer.toString(gridManager.getGirdStationId(taskListRequest.getTasks().get(i).getStartGrid())),
                                    Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i+1)))), TaskDetailDao.Mode.FORK_DN,
                                    formattedDateTime);
                            gridManager.setGridStatus(taskListRequest.getTasks().get(i).getStartGrid(), Grid.Status.BOOKED);
                        }
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.ELEVATOR_TRANSPORT, formattedDateTime);
                        for (int i = taskSize, index=0; i > 0; i--, index++) {
                            gridManager.setGridStatus(availableGrids.get(index).getGridName(), Grid.Status.BOOKED);
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                    Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i)))),
                                    Integer.toString(availableGrids.get(index).getId()), TaskDetailDao.Mode.FORK_DN,
                                    formattedDateTime);
                            insertIntoDB(taskListRequest, index, i-1, availableGrids, formattedDateTime, requestObjectData);
                        }

                        taskListDao.insertTaskList(taskNumber, formattedDateTime, step);
                        nowTaskListDao.insertNowTaskList(taskNumber, step);
                        return "成功發送！ 任務號碼： ".concat(taskNumber);
                    }
                    case "B", "C", "D" -> {
                        String area = "3-" + taskListRequest.getTerminal();
                        List<Grid> availableGrids = gridManager.getAvailableGrids(area);

                        if(availableGrids.size() < taskSize){
                            return "終點區域格位已滿";
                        }

                        String taskNumber = "#YE" + getPureTaskNumber();
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.CALL_ELEVATOR, formattedDateTime);
                        for (int i = 0; i < taskSize; i++) {
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_1, ++step,
                                    Integer.toString(gridManager.getGirdStationId(taskListRequest.getTasks().get(i).getStartGrid())),
                                    Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i+1)))), TaskDetailDao.Mode.FORK_DN,
                                    formattedDateTime);
                            gridManager.setGridStatus(taskListRequest.getTasks().get(i).getStartGrid(), Grid.Status.BOOKED);
                        }
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.ELEVATOR_TRANSPORT, formattedDateTime);
                        for (int i = 0; i < taskSize - 1; i++) {
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                    Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(taskSize-i)))),
                                    Integer.toString(gridManager.getGirdStationId("3-T-".concat(Integer.toString(i+1)))), TaskDetailDao.Mode.FORK_DN,
                                    formattedDateTime);
                        }
                        gridManager.setGridStatus(availableGrids.get(0).getGridName(), Grid.Status.BOOKED);
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                Integer.toString(gridManager.getGirdStationId("E-1")),
                                Integer.toString(availableGrids.get(0).getId()), TaskDetailDao.Mode.FORK_DN,
                                formattedDateTime);
                        insertIntoDB(taskListRequest, 0, 0, availableGrids, formattedDateTime, requestObjectData);
                        for (int i = taskSize - 1, index=1; i > 0; i--, index++) {
                            gridManager.setGridStatus(availableGrids.get(index).getGridName(), Grid.Status.BOOKED);
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                    Integer.toString(gridManager.getGirdStationId("3-T-".concat(Integer.toString(i)))),
                                    Integer.toString(availableGrids.get(index).getId()), TaskDetailDao.Mode.FORK_DN,
                                    formattedDateTime);
                            insertIntoDB(taskListRequest, index, index, availableGrids, formattedDateTime, requestObjectData);
                        }

                        taskListDao.insertTaskList(taskNumber, formattedDateTime, step);
                        nowTaskListDao.insertNowTaskList(taskNumber, step);
                        return "成功發送！ 任務號碼： ".concat(taskNumber);
                    }
                    default -> {
                        return "終點站輸入錯誤";
                    }
                }
            }
            case 2 -> {
                if ("A".equals(taskListRequest.getTerminal())) {
                    String area = "2-" + taskListRequest.getTerminal();
                    List<Grid> availableGrids = gridManager.getAvailableGrids(area);

                    if (availableGrids.size() < taskSize) {
                        return "終點區域格位已滿";
                    }

                    String taskNumber = "#NE" + getPureTaskNumber();
                    for (int i = 0; i < taskSize; i++) {
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_2, ++step,
                                Integer.toString(gridManager.getGirdStationId(taskListRequest.getTasks().get(i).getStartGrid())),
                                Integer.toString(availableGrids.get(i).getId()), TaskDetailDao.Mode.FORK_DN,
                                formattedDateTime);
                        gridManager.setGridStatus(taskListRequest.getTasks().get(i).getStartGrid(), Grid.Status.BOOKED);
                        gridManager.setGridStatus(availableGrids.get(i).getGridName(), Grid.Status.BOOKED);
                    }
                    for (int i = taskSize, index=0; i > 0; i--, index++) {
                        insertIntoDB(taskListRequest, index, index, availableGrids, formattedDateTime, requestObjectData);
                    }
                    taskListDao.insertTaskList(taskNumber, formattedDateTime, step);
                    nowTaskListDao.insertNowTaskList(taskNumber, step);
                    return "成功發送！ 任務號碼： ".concat(taskNumber);
                }
                return "終點站輸入錯誤";
            }
            case 3 -> {
                if ("R".equals(taskListRequest.getTerminal())) {
                    String area = "1-" + taskListRequest.getTerminal();
                    List<Grid> availableGrids = gridManager.getAvailableGrids(area);

                    if(availableGrids.size() < taskSize){
                        return "終點區域格位已滿";
                    }

                    String taskNumber = "#RE" + getPureTaskNumber();
                    for (int i = 0; i < taskSize; i++) {
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                Integer.toString(gridManager.getGirdStationId(taskListRequest.getTasks().get(i).getStartGrid())),
                                Integer.toString(gridManager.getGirdStationId("3-T-".concat(Integer.toString(i+1)))), TaskDetailDao.Mode.FORK_DN,
                                formattedDateTime);
                        gridManager.setGridStatus(taskListRequest.getTasks().get(i).getStartGrid(), Grid.Status.BOOKED);
                    }
                    taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.CALL_ELEVATOR, formattedDateTime);
                    taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                            Integer.toString(gridManager.getGirdStationId("3-T-".concat(Integer.toString(taskSize)))),
                            Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(1)))), TaskDetailDao.Mode.FORK_UP,
                            formattedDateTime);
                    for (int i = 1; i < taskSize; i++) {
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                Integer.toString(gridManager.getGirdStationId("3-T-".concat(Integer.toString(taskSize-i)))),
                                Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i+1)))), TaskDetailDao.Mode.FORK_DN,
                                formattedDateTime);
                    }
                    taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.ELEVATOR_TRANSPORT, formattedDateTime);
                    for (int i = taskSize, index=0; i > 0; i--, index++) {
                        gridManager.setGridStatus(availableGrids.get(index).getGridName(), Grid.Status.BOOKED);
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_1, ++step,
                                Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i)))),
                                Integer.toString(availableGrids.get(index).getId()), TaskDetailDao.Mode.FORK_DN,
                                formattedDateTime);
                        insertIntoDB(taskListRequest, index, index, availableGrids, formattedDateTime, requestObjectData);
                    }

                    taskListDao.insertTaskList(taskNumber, formattedDateTime, step);
                    nowTaskListDao.insertNowTaskList(taskNumber, step);
                    return "成功發送！ 任務號碼： ".concat(taskNumber);
                }
                return "終點站輸入錯誤";
            }
            default -> {
                return "模式輸入錯誤";
            }
        }
    }

    private String getPureTaskNumber(){
        String lastTaskNumber = taskListDao.selectLastTaskListNumber();
        if (lastDate == null) {
            // 伺服器重啟
            lastDate = lastTaskNumber.substring(3, 11);
        }
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = currentDate.format(formatter);
        int serialNumber;
        if (!lastDate.equals(formattedDate)){
            serialNumber = 1;
            lastDate = formattedDate;
        } else {
            // 日期未變更，流水號遞增
            serialNumber = Integer.parseInt(lastTaskNumber.substring(11));
            serialNumber++;
        }
        return lastDate + String.format("%04d", serialNumber);
    }

    private void sortTasksByStartGrid(TaskListRequest taskListRequest) {
        // 使用Comparator進行自定義排序
        taskListRequest.getTasks().sort(new Comparator<TaskListRequest.Task>() {
            @Override
            public int compare(TaskListRequest.Task task1, TaskListRequest.Task task2) {
                int number1 = extractNumber(task1.getStartGrid());
                int number2 = extractNumber(task2.getStartGrid());

                // 從大到小排序
                return Integer.compare(number2, number1);
            }

            // 提取數字部分
            private int extractNumber(String s) {
                String[] parts = s.split("-");
                if (parts.length > 2) {
                    try {
                        return Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        // 處理無法提取數字例外
                        e.printStackTrace();
                    }
                }
                return 0; // 默認返回0，表示無法提取數字
            }
        });
    }

    private void insertIntoDB(TaskListRequest taskListRequest, int index, int index2, List<Grid> availableGrids, String formattedDateTime, List<List<WorkNumberResult>> requestObjectData){
        List<String> workNumbers = taskListRequest.getTasks().get(index).getWorkNumber();
        switch (workNumbers.size()){
            case 0 -> gridListDao.updateWorkOrder(availableGrids.get(index2).getId(), formattedDateTime);
            case 1 -> gridListDao.updateWorkOrder(availableGrids.get(index2).getId(), formattedDateTime,
                        workNumbers.get(0), requestObjectData.get(index).get(0).getObjectName(),
                        requestObjectData.get(index).get(0).getObjectNumber(),
                        taskListRequest.getTasks().get(index).getLineCode().get(0));
            case 2 -> gridListDao.updateWorkOrder(availableGrids.get(index2).getId(), formattedDateTime,
                        workNumbers.get(0), workNumbers.get(1), requestObjectData.get(index).get(0).getObjectName(),
                        requestObjectData.get(index).get(1).getObjectName(),
                        requestObjectData.get(index).get(0).getObjectNumber(),
                        requestObjectData.get(index).get(1).getObjectNumber(),
                        taskListRequest.getTasks().get(index).getLineCode().get(0),
                        taskListRequest.getTasks().get(index).getLineCode().get(1));
            case 3 -> gridListDao.updateWorkOrder(availableGrids.get(index2).getId(), formattedDateTime,
                        workNumbers.get(0), workNumbers.get(1), workNumbers.get(2),
                        requestObjectData.get(index).get(0).getObjectName(),
                        requestObjectData.get(index).get(1).getObjectName(),
                        requestObjectData.get(index).get(2).getObjectName(),
                        requestObjectData.get(index).get(0).getObjectNumber(),
                        requestObjectData.get(index).get(1).getObjectNumber(),
                        requestObjectData.get(index).get(2).getObjectNumber(),
                        taskListRequest.getTasks().get(index).getLineCode().get(0),
                        taskListRequest.getTasks().get(index).getLineCode().get(1),
                        taskListRequest.getTasks().get(index).getLineCode().get(2));
            case 4 -> gridListDao.updateWorkOrder(availableGrids.get(index2).getId(), formattedDateTime,
                        workNumbers.get(0), workNumbers.get(1), workNumbers.get(2), workNumbers.get(3),
                        requestObjectData.get(index).get(0).getObjectName(),
                        requestObjectData.get(index).get(1).getObjectName(),
                        requestObjectData.get(index).get(2).getObjectName(),
                        requestObjectData.get(index).get(3).getObjectName(),
                        requestObjectData.get(index).get(0).getObjectNumber(),
                        requestObjectData.get(index).get(1).getObjectNumber(),
                        requestObjectData.get(index).get(2).getObjectNumber(),
                        requestObjectData.get(index).get(3).getObjectNumber(),
                        taskListRequest.getTasks().get(index).getLineCode().get(0),
                        taskListRequest.getTasks().get(index).getLineCode().get(1),
                        taskListRequest.getTasks().get(index).getLineCode().get(2),
                        taskListRequest.getTasks().get(index).getLineCode().get(3));
        }
    }
}
