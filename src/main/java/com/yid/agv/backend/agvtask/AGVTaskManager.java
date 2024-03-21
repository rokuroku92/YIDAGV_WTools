package com.yid.agv.backend.agvtask;


import com.yid.agv.backend.station.GridManager;
import com.yid.agv.repository.AGVIdDao;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class AGVTaskManager {
    @Autowired
    private AGVIdDao agvIdDao;
    @Autowired
    private GridManager gridManager;
    private final Map<Integer, Queue<AGVQTask>> taskQueueMap;

    public AGVTaskManager() {
        taskQueueMap = new HashMap<>();
    }

    @PostConstruct
    public void initialize() {
        agvIdDao.queryAGVList().forEach(agvId -> taskQueueMap.put(agvId.getId(), new ConcurrentLinkedDeque<>()));
    }

    public Queue<AGVQTask> getTaskQueue(int agvId) {
        return taskQueueMap.get(agvId);
    }

    public void forceClearTaskQueueByAGVId(int agvId) {
        taskQueueMap.put(agvId, new ConcurrentLinkedDeque<>());
    }

    public AGVQTask getNewTaskByAGVId(int agvId) {
        return taskQueueMap.get(agvId).poll();
    }

    public AGVQTask peekNewTaskByAGVId(int agvId) {
        return taskQueueMap.get(agvId).peek();
    }

    public boolean isEmpty(int agvId) {
        return taskQueueMap.get(agvId).isEmpty();
    }

    public boolean addTaskToQueue(AGVQTask task) {
//        if(taskQueue.size() >= 5)
//            return false;
        Queue<AGVQTask> taskQueue = taskQueueMap.get(task.getAgvId());
        taskQueue.offer(task);
        return true;
    }
//    public Integer getTerminalByNotification(String notificationId);  // 這個專案不用自動選擇終點站
//    public QTask peekTaskWithPlace();  // 這個專案不用優先派遣演算法


//    public boolean removeTaskByTaskNumber(String taskNumber) {
//        Iterator<QTask> taskIterator = taskQueue.iterator();
//        while (taskIterator.hasNext()) {
//            QTask task = taskIterator.next();
//            if (task.getTaskNumber().equals(taskNumber) && task.getStatus()==0) {
//                taskIterator.remove();
//                bookedStation[task.getStartStationId()-1] = 0;
//                bookedStation[task.getTerminalStationId()-1] = 0;
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public void failedTask() {
//        Iterator<QTask> taskIterator = taskQueue.iterator();
//        while (taskIterator.hasNext()) {
//            QTask task = taskIterator.next();
//            if (task.getTaskNumber().equals(nowTaskNumber)) {
//                taskIterator.remove();
//                bookedStation[task.getStartStationId()-1] = 0;
//                bookedStation[task.getTerminalStationId()-1] = 0;
//                nowTaskNumber = "";
//            }
//        }
//        taskDao.cancelTask(nowTaskNumber);
//    }

//    public void completedTask() {
//        taskQueue.removeIf(task -> task.getTaskNumber().equals(nowTaskNumber));
//        taskDao.updateTaskStatus(nowTaskNumber, 100);
//    }
//
//    public QTask getTaskByTaskNumber(String taskNumber) {
//        return taskQueue.stream()
//                .filter(task -> task.getTaskNumber().equals(taskNumber))
//                .findFirst()
//                .orElse(null);
//    }
//
//    public void updateTaskStatus(String taskNumber, int status){
//        for (QTask task : taskQueue) {
//            if (task.getTaskNumber().equals(taskNumber)) {
//                task.setStatus(status);
//            }
//        }
//        taskDao.updateTaskStatus(nowTaskNumber, status);
//    }

//    public String getNowTaskNumber() {
//        return nowTaskNumber;
//    }

//    public void setNowTaskNumber(String taskNumber) {
//        nowTaskNumber = taskNumber;
//    }
//
//    public int getBookedStationStatusByStation(int station){
//        return bookedStation[station-1];
//    }
//
//    public void setBookedStation(int station, int status){
//        bookedStation[station-1] = status;
//    }

//    public Collection<QTask> getTaskQueueCopy(){
//        return Collections.unmodifiableCollection(taskQueue);
//    }

}
