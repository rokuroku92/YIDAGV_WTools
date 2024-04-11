package com.yid.agv.backend.agvtask;


import com.yid.agv.backend.station.GridManager;
import com.yid.agv.repository.AGVIdDao;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 表示 AGV 任務管理器（AGVTaskManager）的類。
 * 這個類負責管理 AGV 的任務，包括初始化任務佇列、獲取任務、添加任務等操作。
 */
@Component
public class AGVTaskManager {
    @Autowired
    private AGVIdDao agvIdDao;
    @Autowired
    private GridManager gridManager;
    private final Map<Integer, Queue<AGVQTask>> taskQueueMap;

    /**
     * AGVTaskManager 的構造函數，初始化任務佇列映射表。
     */
    public AGVTaskManager() {
        taskQueueMap = new HashMap<>();
    }

    /**
     * 在初始化之後調用，從 AGVIdDao 中獲取 AGV 列表並初始化任務佇列映射表。
     */
    @PostConstruct
    public void initialize() {
        agvIdDao.queryAGVList().forEach(agvId -> taskQueueMap.put(agvId.getId(), new ConcurrentLinkedDeque<>()));
    }

    /**
     * 根據 AGV ID 獲取對應的任務佇列。
     * @param agvId AGV 的 ID。
     * @return 對應 AGV 的任務佇列。
     */
    public Queue<AGVQTask> getTaskQueue(int agvId) {
        return taskQueueMap.get(agvId);
    }

    /**
     * 強制清空指定 AGV 的任務佇列。
     * @param agvId AGV 的 ID。
     */
    public void forceClearTaskQueueByAGVId(int agvId) {
        taskQueueMap.put(agvId, new ConcurrentLinkedDeque<>());
    }

    /**
     * 從指定 AGV 的任務佇列中獲取新任務。
     * @param agvId AGV 的 ID。
     * @return 從指定 AGV 的任務佇列中獲取的新任務。
     */
    public AGVQTask getNewTaskByAGVId(int agvId) {
        return taskQueueMap.get(agvId).poll();
    }

    /**
     * 查看指定 AGV 的任務佇列中的新任務，但不從佇列中移除。
     * @param agvId AGV 的 ID。
     * @return 指定 AGV 的任務佇列中的新任務。
     */
    public AGVQTask peekNewTaskByAGVId(int agvId) {
        return taskQueueMap.get(agvId).peek();
    }

    /**
     * 判斷指定 AGV 的任務佇列是否為空。
     * @param agvId AGV 的 ID。
     * @return 如果指定 AGV 的任務佇列為空，則返回 true；否則返回 false。
     */
    public boolean isEmpty(int agvId) {
        return taskQueueMap.get(agvId).isEmpty();
    }

    /**
     * 將任務添加到指定 AGV 的任務佇列中。
     * @param task 要添加的任務。
     * @return 如果成功添加任務，則返回 true；否則返回 false。
     */
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

//    public QTask getTaskByTaskNumber(String taskNumber) {
//        return taskQueue.stream()
//                .filter(task -> task.getTaskNumber().equals(taskNumber))
//                .findFirst()
//                .orElse(null);
//    }

    /**
     * 根據 AGV ID 獲取對應且無法修改的任務佇列。
     * @param agvId AGV 的 ID。
     * @return 對應 AGV 的任務佇列。
     */
    public Collection<AGVQTask> getTaskQueueCopyByAGVId(int agvId){
        return Collections.unmodifiableCollection(getTaskQueue(agvId));
    }

}
