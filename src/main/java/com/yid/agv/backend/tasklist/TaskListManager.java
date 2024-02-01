package com.yid.agv.backend.tasklist;

import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.model.NowTaskList;
import com.yid.agv.model.TaskDetail;
import com.yid.agv.model.TaskList;
import com.yid.agv.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaskListManager {
    @Autowired
    private NowTaskListDao nowTaskListDao;
    @Autowired
    private TaskListDao taskListDao;
    @Autowired
    private TaskDetailDao taskDetailDao;
    @Autowired
    private GridListDao gridListDao;
    @Autowired
    private GridManager gridManager;

    private final Map<Integer, NowTaskList> taskListMap;
    private final Map<String, List<TaskDetail>> taskDetailsMap;

    public TaskListManager() {
        taskListMap = new HashMap<>();
        taskDetailsMap = new HashMap<>();
    }

    @PostConstruct
    public void initialize() {
        taskListMap.put(1, null);
        taskListMap.put(2, null);
        System.out.println("Initialize taskListMap: " + taskListMap);

        // 處理資料庫中狀態為執行中的任務
//        List<TaskList> unexpectedTasks = taskListDao.queryUnexpectedTaskLists();
        taskListMap.forEach((taskProcessId, taskList) -> {
            List<NowTaskList> unCompletedTaskLists = nowTaskListDao.queryNowTaskListsByProcessId(taskProcessId);
            unCompletedTaskLists.forEach(unCompletedTaskList -> {
                if (unCompletedTaskList.getProgress() != 0){
                    List<TaskDetail> unCompletedTaskDetails = taskDetailDao.queryTaskDetailsByTaskNumber(unCompletedTaskList.getTaskNumber());
                    unCompletedTaskDetails.forEach(unCompletedTaskDetail -> {
                        if(unCompletedTaskDetail.getStatus() != 100) {
                            System.out.println("cancel： " + unCompletedTaskDetail.getTaskNumber() + " : " + unCompletedTaskDetail.getSequence());
                            if(unCompletedTaskDetail.getMode()!=100 && unCompletedTaskDetail.getMode()!=101){
                                gridManager.setGridStatus(unCompletedTaskDetail.getStartId(), Grid.Status.FREE);
                                gridManager.setGridStatus(unCompletedTaskDetail.getTerminalId(), Grid.Status.FREE);
                                gridListDao.clearWorkOrder(unCompletedTaskDetail.getTerminalId());
                            }
                            taskDetailDao.updateStatusByTaskNumberAndSequence(unCompletedTaskDetail.getTaskNumber(), unCompletedTaskDetail.getSequence(), -1);
                        }
                    });
                    nowTaskListDao.deleteNowTaskList(unCompletedTaskList.getTaskNumber());
                    taskListDao.cancelTaskList(unCompletedTaskList.getTaskNumber());
                }
            });
        });
    }

    @Scheduled(fixedRate = 2000)
    public synchronized void refreshTaskList(){
        taskListMap.forEach((taskProcessId, taskList) -> {
            if(taskList == null) {
                List<NowTaskList> taskLists = nowTaskListDao.queryNowTaskListsByProcessId(taskProcessId);
                if (taskLists.size()>0){
                    NowTaskList doTaskList = taskLists.get(0);
                    if (doTaskList != null){
                        taskListMap.put(taskProcessId, doTaskList);
                        taskDetailsMap.put(doTaskList.getTaskNumber(), taskDetailDao.queryTaskDetailsByTaskNumber(doTaskList.getTaskNumber()));
                    }
                }
            }
        });
    }

    public NowTaskList getNowTaskListByTaskProcessId(int taskProcessId){
        return taskListMap.get(taskProcessId);
    }

    public List<NowTaskList> getNowAllTaskList(){
        List<NowTaskList> allTaskLists = new ArrayList<>();

        // 遍歷所有任務處理ID，取得非空任務列表
        taskListMap.forEach((taskProcessId, taskList) -> {
            if (taskList != null) {
                allTaskLists.add(taskList);
            }
        });

        return allTaskLists;
    }

    public List<TaskDetail> getTaskDetailByTaskNumber(String taskNumber){
        return taskDetailsMap.get(taskNumber);
    }

    public int getTaskDetailLengthByTaskNumber(String taskNumber){
        return taskDetailsMap.get(taskNumber).size();
    }

    public void setTaskListPhase(NowTaskList nowTaskList, Phase phase){
        nowTaskList.setPhase(phase);
        nowTaskListDao.updateNowTaskListPhase(nowTaskList.getTaskNumber(), phase);
        taskListDao.updateTaskListPhase(nowTaskList.getTaskNumber(), phase);
    }

    public void setTaskListProgress(NowTaskList nowTaskList, int progress){
        nowTaskList.setProgress(progress);
        nowTaskListDao.updateNowTaskListProgress(nowTaskList.getTaskNumber(), progress);
        taskListDao.updateTaskListProgress(nowTaskList.getTaskNumber(), progress);
    }

    public void setTaskListProgressBySequence(String taskNumber, int sequence){
        int steps = getTaskDetailLengthByTaskNumber(taskNumber);
        int progress = (int)(((double)sequence/(double)steps)*(double)99);
        nowTaskListDao.updateNowTaskListProgress(taskNumber, progress);
        taskListDao.updateTaskListProgress(taskNumber, progress);
    }

    public void completedTaskList(int taskProcessId){
        String taskNumber = getNowTaskListByTaskProcessId(taskProcessId).getTaskNumber();
        nowTaskListDao.deleteNowTaskList(taskNumber);
        taskListDao.updateTaskListStatus(taskNumber, 100);
        taskListDao.updateTaskListProgress(taskNumber, 100);
        taskListMap.put(taskProcessId, null);
        taskDetailsMap.put(taskNumber, null);
    }

    public int getTaskListMapSize(){
        return taskListMap.size();
    }
}
