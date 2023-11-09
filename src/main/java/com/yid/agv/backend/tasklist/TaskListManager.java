package com.yid.agv.backend.tasklist;

import com.yid.agv.model.NowTaskList;
import com.yid.agv.model.TaskDetail;
import com.yid.agv.model.TaskList;
import com.yid.agv.repository.NowTaskListDao;
import com.yid.agv.repository.Phase;
import com.yid.agv.repository.TaskDetailDao;
import com.yid.agv.repository.TaskListDao;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    }

    @Scheduled(fixedRate = 4000)
    public synchronized void refreshTaskList(){
        taskListMap.forEach((taskProcessId, taskList) -> {
            if(taskList == null) {
                List<NowTaskList> taskLists = nowTaskListDao.queryNowTaskLists(taskProcessId);
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
        nowTaskListDao.updateNowTaskListProgress(taskNumber, (sequence/steps)*99);
        taskListDao.updateTaskListProgress(taskNumber, (sequence/steps)*99);
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
