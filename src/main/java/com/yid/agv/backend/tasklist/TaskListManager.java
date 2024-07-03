package com.yid.agv.backend.tasklist;

import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.model.NowTaskList;
import com.yid.agv.model.TaskDetail;
import com.yid.agv.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TaskListManager 類負責管理當前任務列表和任務細節。
 */
@Component
public class TaskListManager {
    private static final Logger log = LoggerFactory.getLogger(TaskListManager.class);
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

    /**
     * TaskListManager 的建構函數，初始化 taskListMap 和 taskDetailsMap。
     */
    public TaskListManager() {
        taskListMap = new HashMap<>();
        taskDetailsMap = new HashMap<>();
    }

    /**
     * 在 Spring 容器初始化後執行，初始化 taskListMap。
     */
    @PostConstruct
    public void initialize() {
        taskListMap.put(1, null);
        taskListMap.put(2, null);
        log.info("Initialize taskListMap: {}", taskListMap);

        // 處理資料庫中狀態為執行中的任務
//        List<TaskList> unexpectedTasks = taskListDao.queryUnexpectedTaskLists();
        taskListMap.forEach((taskProcessId, taskList) -> {
            cancelTaskList(taskProcessId);
        });
    }

    public void cancelTaskList(Integer taskProcessId) {
        taskListMap.put(taskProcessId, null);
        List<NowTaskList> unCompletedTaskLists = nowTaskListDao.queryNowTaskListsByProcessId(taskProcessId);
        unCompletedTaskLists.forEach(unCompletedTaskList -> {
            if (unCompletedTaskList.getProgress() != 0){
                List<TaskDetail> unCompletedTaskDetails = taskDetailDao.queryTaskDetailsByTaskNumber(unCompletedTaskList.getTaskNumber());
                unCompletedTaskDetails.forEach(unCompletedTaskDetail -> {
                    if(unCompletedTaskDetail.getStatus() != 100) {
                        log.info("Cancel： {} : {}", unCompletedTaskDetail.getTaskNumber(), unCompletedTaskDetail.getSequence());
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
    }

    /**
     * 定時任務，定期刷新任務列表(與資料庫對照)。
     */
    @Scheduled(fixedRate = 1000)
    public synchronized void refreshTaskList(){
        taskListMap.forEach((taskProcessId, taskList) -> {
            if(taskList == null) {
                List<NowTaskList> taskLists = nowTaskListDao.queryNowTaskListsByProcessId(taskProcessId);
                if (!taskLists.isEmpty()) {
                    NowTaskList doTaskList = taskLists.get(0);
                    if (doTaskList != null){
                        taskListMap.put(taskProcessId, doTaskList);
                        taskDetailsMap.put(doTaskList.getTaskNumber(), taskDetailDao.queryTaskDetailsByTaskNumber(doTaskList.getTaskNumber()));
                    }
                }
            }
        });
    }

    /**
     * 根據任務處理 ID 獲取當前任務列表。
     * @param taskProcessId 任務處理 ID
     * @return 當前任務列表
     */
    public NowTaskList getNowTaskListByTaskProcessId(int taskProcessId){
        return taskListMap.get(taskProcessId);
    }

    public Integer getProcessIdByTaskNumber(String taskNumber) {
        return taskListMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getTaskNumber().equals(taskNumber))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void clearNowTaskListByTaskNumber(String taskNumber){
        taskListMap.forEach((taskProcessId, taskList) -> {
            if(taskList.getTaskNumber().equals(taskNumber)){
                taskListMap.put(taskProcessId, null);
            }
        });
    }

    /**
     * 獲取所有當前任務列表。
     * @return 所有當前任務列表
     */
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

    /**
     * 根據任務編號獲取任務細節列表。
     * @param taskNumber 任務編號
     * @return 任務細節列表
     */
    public List<TaskDetail> getTaskDetailByTaskNumber(String taskNumber){
        return taskDetailsMap.get(taskNumber);
    }

    /**
     * 根據任務編號獲取任務細節列表的長度。
     * @param taskNumber 任務編號
     * @return 任務細節列表的長度
     */
    public int getTaskDetailLengthByTaskNumber(String taskNumber){
        return taskDetailsMap.get(taskNumber).size();
    }

    /**
     * 設置任務列表的階段。
     * @param nowTaskList 現在的任務列表
     * @param phase 要設置的階段
     */
    public void setTaskListPhase(NowTaskList nowTaskList, Phase phase){
        nowTaskList.setPhase(phase);
        nowTaskListDao.updateNowTaskListPhase(nowTaskList.getTaskNumber(), phase);
        taskListDao.updateTaskListPhase(nowTaskList.getTaskNumber(), phase);
    }

    /**
     * 設置任務列表的進度。
     * @param nowTaskList 現在的任務列表
     * @param progress 要設置的進度
     */
    public void setTaskListProgress(NowTaskList nowTaskList, int progress){
        nowTaskList.setProgress(progress);
        nowTaskListDao.updateNowTaskListProgress(nowTaskList.getTaskNumber(), progress);
        taskListDao.updateTaskListProgress(nowTaskList.getTaskNumber(), progress);
    }

    /**
     * 根據序列設置任務列表的進度。
     * @param taskNumber 任務編號
     * @param sequence 序列
     */
    public void setTaskListProgressBySequence(String taskNumber, int sequence){
        int steps = getTaskDetailLengthByTaskNumber(taskNumber);
        int progress = (int)(((double)sequence/(double)steps)*(double)99);
        nowTaskListDao.updateNowTaskListProgress(taskNumber, progress);
        taskListDao.updateTaskListProgress(taskNumber, progress);
    }

    /**
     * 完成任務列表。
     * @param taskProcessId 任務處理 ID
     */
    public void completedTaskList(int taskProcessId){
        String taskNumber = getNowTaskListByTaskProcessId(taskProcessId).getTaskNumber();
        nowTaskListDao.deleteNowTaskList(taskNumber);
        taskListDao.updateTaskListStatus(taskNumber, 100);
        taskListDao.updateTaskListProgress(taskNumber, 100);
        taskListMap.put(taskProcessId, null);
        taskDetailsMap.put(taskNumber, null);
    }

    /**
     * 獲取任務列表大小(任務處理進程的數量)。
     * @return 任務列表的大小(任務處理進程的數量)
     */
    public int getTaskListMapSize(){
        return taskListMap.size();
    }
}
