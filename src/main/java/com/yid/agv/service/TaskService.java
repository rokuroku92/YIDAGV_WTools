
package com.yid.agv.service;

import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.backend.agvtask.AGVTaskManager;
import com.yid.agv.dto.TaskListRequest;
import com.yid.agv.model.*;
import com.yid.agv.repository.GridListDao;
import com.yid.agv.repository.NowTaskListDao;
import com.yid.agv.repository.TaskDetailDao;
import com.yid.agv.repository.TaskListDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TaskService {

    @Autowired
    private TaskListDao taskListDao;
    @Autowired
    private NowTaskListDao nowTaskListDao;
    @Autowired
    private TaskDetailDao taskDetailDao;
    @Autowired
    private GridListDao gridListDao;
    @Autowired
    private GridManager gridManager;
    @Autowired
    private AGVTaskManager taskQueue;

    @Autowired
    @Qualifier("WToolsJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    
    private String lastDate;

    public WorkNumberResult getWToolsInformation(String workNumber){
        if(workNumber.matches("^[A-Za-z0-9]{4}-\\d{11}$")){
            String[] TA = workNumber.split("-");
            String sql = "SELECT `TA006` AS `object_number`, `TA034` AS `object_name` FROM `V_MOCTA` WHERE `TA001` = ? AND `TA002` = ?";
            System.out.println(jdbcTemplate);
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(WorkNumberResult.class),TA[0],TA[1]);
        } else {
            return null;
        }
    }

//    public Collection<AGVQTask> getTaskQueue(){
//        return taskQueue.getTaskQueueCopy();
//    }
//
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

    public boolean cancelTask(String taskNumber){
        // TODO: com
        return true;
//        return taskQueue.removeTaskByTaskNumber(taskNumber) && taskListDao.cancelTaskList(taskNumber);
    }

    public String addTaskList(TaskListRequest taskListRequest){
        int taskSize = taskListRequest.getTasks().size();
        if (taskSize == 0){
            return "未輸入起始格位";
        }
        for (int i = 0; i < taskSize; i++) {
            if(gridManager.getGridStatus(taskListRequest.getTasks().get(i).getStartGrid()) != Grid.Status.FREE){
                return "起始格位非可用";
            }
        }

        AtomicBoolean iNull = new AtomicBoolean(false);
        List<List<WorkNumberResult>> requestObjectData = new ArrayList<>();
        taskListRequest.getTasks().forEach(task -> {
            List<WorkNumberResult> objectData = new ArrayList<>();
            task.getWorkNumber().forEach(workNumber -> {
                WorkNumberResult result = getWToolsInformation(workNumber);
                if(result == null){
                    iNull.set(true);
                } else {
                    objectData.add(result);
                }
            });
            requestObjectData.add(objectData);
        });
        if(iNull.get()){
            return "工單號碼輸入錯誤";
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDateTime = currentDateTime.format(formatter);
        int step = 0;
        switch (taskListRequest.getMode()){  // 1: 1F->3F | 2: 2F->2F | 3: 3F->1F
            case 1 -> {
                switch (taskListRequest.getTerminal()) {
                    case "A", "B", "C", "D" -> {
                        String area = "3-" + taskListRequest.getTerminal();
                        List<Grid> availableGrids = gridManager.getAvailableGrids(area);

                        if(availableGrids.size() <= taskSize){
                            return "終點區域格位已滿";
                        }

                        String taskNumber = "#YE" + getPureTaskNumber();
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.CALL_ELEVATOR);
                        for (int i = 0; i < taskSize; i++) {
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_1, ++step,
                                    Integer.toString(gridManager.getGirdStationId(taskListRequest.getTasks().get(i).getStartGrid())),
                                    Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(taskSize-i)))), TaskDetailDao.Mode.DEFAULT);  // TODO: wait confirm
                            gridManager.setGridStatus(taskListRequest.getTasks().get(i).getStartGrid(), Grid.Status.BOOKED);
                        }
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.ELEVATOR_TRANSPORT);
                        for (int i = 1; i <= taskSize; i++) {
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                    Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i)))),
                                    Integer.toString(gridManager.getGirdStationId("3-T-".concat(Integer.toString(i)))), TaskDetailDao.Mode.DEFAULT);  // TODO: wait confirm
                        }
                        for (int i = taskSize, index=0; i > 0; i--, index++) {
                            gridManager.setGridStatus(availableGrids.get(index).getGridName(), Grid.Status.BOOKED);
                            taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                    Integer.toString(gridManager.getGirdStationId("3-T-".concat(Integer.toString(i)))),
                                    Integer.toString(availableGrids.get(index).getStationId()), TaskDetailDao.Mode.DEFAULT);  // TODO: wait confirm
                            insertIntoDB(taskListRequest, index, availableGrids, formattedDateTime, requestObjectData);
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

                    if (availableGrids.size() <= taskSize) {
                        return "終點區域格位已滿";
                    }

                    String taskNumber = "#NE" + getPureTaskNumber();
                    for (int i = 0; i < taskSize; i++) {
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_2, ++step,
                                Integer.toString(gridManager.getGirdStationId(taskListRequest.getTasks().get(i).getStartGrid())),
                                Integer.toString(availableGrids.get(i).getStationId()), TaskDetailDao.Mode.DEFAULT);  // TODO: wait confirm
                        gridManager.setGridStatus(taskListRequest.getTasks().get(i).getStartGrid(), Grid.Status.BOOKED);
                        gridManager.setGridStatus(availableGrids.get(i).getGridName(), Grid.Status.BOOKED);
                    }
                    for (int i = taskSize, index=0; i > 0; i--, index++) {
                        insertIntoDB(taskListRequest, index, availableGrids, formattedDateTime, requestObjectData);
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

                    if(availableGrids.size() <= taskSize){
                        return "終點區域格位已滿";
                    }

                    String taskNumber = "#RE" + getPureTaskNumber();
                    taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.CALL_ELEVATOR);
                    for (int i = 0; i < taskSize; i++) {
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_3, ++step,
                                Integer.toString(gridManager.getGirdStationId(taskListRequest.getTasks().get(i).getStartGrid())),
                                Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i+1)))), TaskDetailDao.Mode.DEFAULT);  // TODO: wait confirm
                        gridManager.setGridStatus(taskListRequest.getTasks().get(i).getStartGrid(), Grid.Status.BOOKED);
                    }
                    taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.ELEVATOR, ++step, TaskDetailDao.Mode.ELEVATOR_TRANSPORT);
                    for (int i = taskSize, index=0; i > 0; i--, index++) {
                        gridManager.setGridStatus(availableGrids.get(index).getGridName(), Grid.Status.BOOKED);
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_1, ++step,
                                Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i)))),
                                Integer.toString(availableGrids.get(index).getStationId()), TaskDetailDao.Mode.DEFAULT);  // TODO: wait confirm
                        insertIntoDB(taskListRequest, index, availableGrids, formattedDateTime, requestObjectData);
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

    private void insertIntoDB(TaskListRequest taskListRequest, int index, List<Grid> availableGrids, String formattedDateTime, List<List<WorkNumberResult>> requestObjectData){
        List<String> workNumbers = taskListRequest.getTasks().get(index).getWorkNumber();
        switch (workNumbers.size()){
            case 0 -> gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime);
            case 1 -> {
                gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime,
                        workNumbers.get(0), requestObjectData.get(index).get(0).getObjectName(),
                        requestObjectData.get(index).get(0).getObjectNumber(),
                        taskListRequest.getTasks().get(index).getLineCode().get(0));
            }
            case 2 -> {
                gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime,
                        workNumbers.get(0), workNumbers.get(1), requestObjectData.get(index).get(0).getObjectName(),
                        requestObjectData.get(index).get(1).getObjectName(),
                        requestObjectData.get(index).get(0).getObjectNumber(),
                        requestObjectData.get(index).get(1).getObjectNumber(),
                        taskListRequest.getTasks().get(index).getLineCode().get(0),
                        taskListRequest.getTasks().get(index).getLineCode().get(1));
            }
            case 3 -> {
                gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime,
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
            }
            case 4 -> {
                gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime,
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
}
