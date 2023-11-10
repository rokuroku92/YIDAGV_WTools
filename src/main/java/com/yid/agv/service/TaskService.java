
package com.yid.agv.service;

import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.backend.agvtask.AGVTaskManager;
import com.yid.agv.dto.TaskListRequest;
import com.yid.agv.model.NowTaskListResponse;
import com.yid.agv.model.TaskList;
import com.yid.agv.repository.GridListDao;
import com.yid.agv.repository.NowTaskListDao;
import com.yid.agv.repository.TaskDetailDao;
import com.yid.agv.repository.TaskListDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    
    private String lastDate;


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
                            gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime);  // TODO: Remove
//                            List<String> objectNumbers = taskListRequest.getTasks().get(index).getObjectNumber();
//                            switch (objectNumbers.size()){
//                                case 0 -> gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime);
//                                case 1 -> {
//                                    gridListDao.updateWorkOrder(availableGrids.get(index).getStationId(), formattedDateTime, objectNumbers.get(0), );
//                                }
//                                case 2 -> {}
//                                case 3 -> {}
//                                case 4 -> {}
//                            }

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
                if ("A".equals(taskListRequest.getTerminal())
                    ) {
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
                    for (int i = taskSize-1, index=0; i >= 0; i--, index++) {
                        gridManager.setGridStatus(availableGrids.get(index).getGridName(), Grid.Status.BOOKED);
                        taskDetailDao.insertTaskDetail(taskNumber, TaskDetailDao.Title.AMR_1, ++step,
                                Integer.toString(gridManager.getGirdStationId("E-".concat(Integer.toString(i+1)))),
                                Integer.toString(availableGrids.get(index).getStationId()), TaskDetailDao.Mode.DEFAULT);  // TODO: wait confirm
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

}
