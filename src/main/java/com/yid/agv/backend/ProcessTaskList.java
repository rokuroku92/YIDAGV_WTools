package com.yid.agv.backend;

import com.yid.agv.backend.agv.AGV;
import com.yid.agv.backend.agv.AGVManager;
import com.yid.agv.backend.agvtask.AGVQTask;
import com.yid.agv.backend.agvtask.AGVTaskManager;
import com.yid.agv.backend.elevator.ElevatorManager;
import com.yid.agv.backend.elevator.ElevatorPermission;
import com.yid.agv.backend.tasklist.TaskListManager;
import com.yid.agv.model.NowTaskList;
import com.yid.agv.model.TaskDetail;
import com.yid.agv.repository.Phase;
import com.yid.agv.repository.TaskDetailDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessTaskList {
    private static final Logger log = LoggerFactory.getLogger(ProcessTaskList.class);
    private final TaskDetailDao taskDetailDao;
    private final AGVManager agvManager;
    private final AGVTaskManager agvTaskManager;
    private final TaskListManager taskListManager;
    private final ElevatorManager elevatorManager;
    private final ProcessAGVTask processAGVTask;

    public ProcessTaskList(TaskDetailDao taskDetailDao,
                           AGVManager agvManager,
                           AGVTaskManager agvTaskManager,
                           TaskListManager taskListManager,
                           ElevatorManager elevatorManager,
                           ProcessAGVTask processAGVTask) {
        this.taskDetailDao = taskDetailDao;
        this.agvManager = agvManager;
        this.agvTaskManager = agvTaskManager;
        this.taskListManager = taskListManager;
        this.elevatorManager = elevatorManager;
        this.processAGVTask = processAGVTask;
    }

    @Scheduled(fixedRate = 4000)
    public void checkTaskList() {

        for(int i = 1; i <= taskListManager.getTaskListMapSize(); i++){
            NowTaskList nowTaskList = taskListManager.getNowTaskListByTaskProcessId(i);
            if (nowTaskList == null) continue;
            List<TaskDetail> taskDetails = taskListManager.getTaskDetailByTaskNumber(nowTaskList.getTaskNumber());

            if (nowTaskList.getTaskNumber().startsWith("#YE")) {
                AGV agv1 = agvManager.getAgv(1);
                AGV agv3 = agvManager.getAgv(3);
                if (nowTaskList.getPhase() == Phase.PRE_START
                        && (agv1.getStatus() != AGV.Status.ONLINE
                        || agv1.isILowBattery()
                        || agv3.getStatus() != AGV.Status.ONLINE
                        || agv3.isILowBattery())) continue;
                handleYETask(nowTaskList, taskDetails, i);
//                handleYETaskTEST(nowTaskList, taskDetails, i);
            } else if (nowTaskList.getTaskNumber().startsWith("#RE")) {
                AGV agv1 = agvManager.getAgv(1);
                AGV agv3 = agvManager.getAgv(3);
                if (nowTaskList.getPhase() == Phase.PRE_START
                        && (agv1.getStatus() != AGV.Status.ONLINE
                        || agv1.isILowBattery()
                        || agv3.getStatus() != AGV.Status.ONLINE
                        || agv3.isILowBattery())) continue;
                handleRETask(nowTaskList, taskDetails, i);
//                handleRETaskTEST(nowTaskList, taskDetails, i);
            } else if (nowTaskList.getTaskNumber().startsWith("#NE")) {
                AGV agv2 = agvManager.getAgv(2);
                if (nowTaskList.getPhase() == Phase.PRE_START
                        && (agv2.getStatus() != AGV.Status.ONLINE
                        || agv2.isILowBattery())) continue;
                handleNETask(nowTaskList, taskDetails, i);
            }
        }

    }

    @SuppressWarnings("unused")
    private void handleYETaskTEST(NowTaskList nowTaskList, List<TaskDetail> taskDetails, int taskProcessId){
        switch (nowTaskList.getPhase()) {
            case PRE_START -> {
                if(elevatorManager.acquireElevatorPermission()){  // check elevator permission
                    elevatorManager.controlElevatorTO(2);
                    taskListManager.setTaskListPhase(nowTaskList, Phase.CALL_ELEVATOR);
                    taskListManager.setTaskListProgress(nowTaskList, 1);
                }
            }
            case CALL_ELEVATOR -> {
                if(elevatorManager.iOpenDoorByFloor(2)){
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#1")) {
                            AGVQTask task = new AGVQTask();
                            task.setAgvId(1);
                            task.setTaskNumber(taskDetail.getTaskNumber());
                            task.setSequence(taskDetail.getSequence());
                            task.setModeId(taskDetail.getMode());
                            task.setStartStation(taskDetail.getStart());
                            task.setStartStationId(taskDetail.getStartId());
                            task.setTerminalStation(taskDetail.getTerminal());
                            task.setTerminalStationId(taskDetail.getTerminalId());
                            task.setStatus(0);
                            log.info(task.toString());
                            AGV agv = agvManager.getAgv(1);
                            agv.setTask(task);
                            processAGVTask.completedTask(agv);
                        }
                    });
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getMode() == 100) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.FIRST_STAGE_1F);
                }
            }
            case FIRST_STAGE_1F -> {
                log.info("已搬運至1F電梯");
                elevatorManager.controlElevatorTO(4);
                taskListManager.setTaskListPhase(nowTaskList, Phase.ELEVATOR_TRANSFER);
            }
            case ELEVATOR_TRANSFER -> {
                if(elevatorManager.iOpenDoorByFloor(4)){
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getStart().startsWith("E-")) {
                            AGVQTask task = new AGVQTask();
                            task.setAgvId(3);
                            task.setTaskNumber(taskDetail.getTaskNumber());
                            task.setSequence(taskDetail.getSequence());
                            task.setModeId(taskDetail.getMode());
                            task.setStartStation(taskDetail.getStart());
                            task.setStartStationId(taskDetail.getStartId());
                            task.setTerminalStation(taskDetail.getTerminal());
                            task.setTerminalStationId(taskDetail.getTerminalId());
                            task.setStatus(0);
                            log.info(task.toString());
                            AGV agv = agvManager.getAgv(3);
                            agv.setTask(task);
                            processAGVTask.completedTask(agv);
                        }
                    });
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getMode() == 101) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.SECOND_STAGE_3F);
                }
            }
            case SECOND_STAGE_3F -> {
                log.info("已從電梯中搬運至3F暫存區完成");
                elevatorManager.controlElevatorTO(null);
                elevatorManager.resetElevatorPermission();  // unlock elevator permission
                taskDetails.forEach(taskDetail -> {
                    if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getStart().startsWith("3-T-")) {
                        AGVQTask task = new AGVQTask();
                        task.setAgvId(3);
                        task.setTaskNumber(taskDetail.getTaskNumber());
                        task.setSequence(taskDetail.getSequence());
                        task.setModeId(taskDetail.getMode());
                        task.setStartStation(taskDetail.getStart());
                        task.setStartStationId(taskDetail.getStartId());
                        task.setTerminalStation(taskDetail.getTerminal());
                        task.setTerminalStationId(taskDetail.getTerminalId());
                        task.setStatus(0);
                        log.info(task.toString());
                        AGV agv = agvManager.getAgv(3);
                        agv.setTask(task);
                        processAGVTask.completedTask(agv);
                    }
                });
                taskListManager.setTaskListPhase(nowTaskList, Phase.THIRD_STAGE_3F);
            }
            case THIRD_STAGE_3F -> {
                log.info("YE任務已完成");
                taskListManager.setTaskListPhase(nowTaskList, Phase.COMPLETED);
            }
            case COMPLETED -> taskListManager.completedTaskList(taskProcessId);
        }
    }
    @SuppressWarnings("unused")
    private void handleRETaskTEST(NowTaskList nowTaskList, List<TaskDetail> taskDetails, int taskProcessId){
        switch (nowTaskList.getPhase()) {
            case PRE_START -> {
                taskDetails.forEach(taskDetail -> {
                    if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getTerminal().startsWith("3-T-")) {
                        AGVQTask task = new AGVQTask();
                        task.setAgvId(3);
                        task.setTaskNumber(taskDetail.getTaskNumber());
                        task.setSequence(taskDetail.getSequence());
                        task.setModeId(taskDetail.getMode());
                        task.setStartStation(taskDetail.getStart());
                        task.setStartStationId(taskDetail.getStartId());
                        task.setTerminalStation(taskDetail.getTerminal());
                        task.setTerminalStationId(taskDetail.getTerminalId());
                        task.setStatus(0);
                        AGV agv = agvManager.getAgv(3);
                        agv.setTask(task);
                        processAGVTask.completedTask(agv);
                    }
                });
                taskListManager.setTaskListPhase(nowTaskList, Phase.FIRST_STAGE_3F);
            }
            case FIRST_STAGE_3F -> {
                log.info("已從倉庫搬運至3F暫存區");
                if(elevatorManager.acquireElevatorPermission()){  // check elevator permission
                    elevatorManager.controlElevatorTO(4);
                    taskListManager.setTaskListPhase(nowTaskList, Phase.CALL_ELEVATOR);
                    taskListManager.setTaskListProgress(nowTaskList, 1);
                }
            }
            case CALL_ELEVATOR -> {
                if(elevatorManager.iOpenDoorByFloor(4)){
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getTerminal().startsWith("E-")) {
                            AGVQTask task = new AGVQTask(taskDetail.getTaskNumber(), 3, taskDetail.getSequence(), taskDetail.getStart(),
                                    taskDetail.getTerminal(), taskDetail.getStartId(), taskDetail.getTerminalId(), taskDetail.getMode(),
                                    0);
                            AGV agv = agvManager.getAgv(3);
                            agv.setTask(task);
                            processAGVTask.completedTask(agv);
                        } else if (taskDetail.getMode() == 100) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.SECOND_STAGE_3F);
                }
            }
            case SECOND_STAGE_3F -> {
                log.info("已從3F暫存區搬運至電梯中");
                elevatorManager.controlElevatorTO(2);
                taskListManager.setTaskListPhase(nowTaskList, Phase.ELEVATOR_TRANSFER);
            }
            case ELEVATOR_TRANSFER -> {
                if(elevatorManager.iOpenDoorByFloor(2)){
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#1") && taskDetail.getStart().startsWith("E-")) {
                            AGVQTask task = new AGVQTask(taskDetail.getTaskNumber(), 1, taskDetail.getSequence(), taskDetail.getStart(),
                                    taskDetail.getTerminal(), taskDetail.getStartId(), taskDetail.getTerminalId(), taskDetail.getMode(),
                                    0);
                            AGV agv = agvManager.getAgv(1);
                            agv.setTask(task);
                            processAGVTask.completedTask(agv);
                        } else if (taskDetail.getMode() == 101) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.THIRD_STAGE_1F);
                }
            }
            case THIRD_STAGE_1F -> {
                log.info("Completed TaskList");
                elevatorManager.controlElevatorTO(null);
                elevatorManager.resetElevatorPermission();  // unlock elevator permission
                taskListManager.setTaskListPhase(nowTaskList, Phase.COMPLETED);
            }
            case COMPLETED -> taskListManager.completedTaskList(taskProcessId);
        }
    }

    private void handleYETask(NowTaskList nowTaskList, List<TaskDetail> taskDetails, int taskProcessId){
        switch (nowTaskList.getPhase()) {
            case PRE_START -> {
                if(elevatorManager.acquireElevatorPermission()){  // check elevator permission
                    elevatorManager.controlElevatorTO(2);
                    taskListManager.setTaskListPhase(nowTaskList, Phase.CALL_ELEVATOR);
                    taskListManager.setTaskListProgress(nowTaskList, 1);
                }
            }
            case CALL_ELEVATOR -> {
//                if(elevatorManager.iOpenDoorByFloor(2)) {
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#1")) {
                            AGVQTask task = new AGVQTask(taskDetail.getTaskNumber(), 1, taskDetail.getSequence(), taskDetail.getStart(),
                                    taskDetail.getTerminal(), taskDetail.getStartId(), taskDetail.getTerminalId(), taskDetail.getMode(),
                                    0);
                            agvTaskManager.addTaskToQueue(task);
                        } else if (taskDetail.getMode() == 100) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.FIRST_STAGE_1F);
//                }
            }
            case FIRST_STAGE_1F -> {
                AGVQTask task = agvManager.getAgv(1).getTask();
                if((task != null && task.getTaskNumber().startsWith("#SB") && !agvManager.iAgvInElevator(1)) ||
                        (agvTaskManager.isEmpty(1) && task == null && agvManager.iAgvInStandbyStation(1))){
                    elevatorManager.controlElevatorTO(4);
                    taskListManager.setTaskListPhase(nowTaskList, Phase.ELEVATOR_TRANSFER);
                }
            }
            case ELEVATOR_TRANSFER -> {
//                if(elevatorManager.iOpenDoorByFloor(4)) {
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getStart().startsWith("E-")) {
                            AGVQTask task = new AGVQTask(taskDetail.getTaskNumber(), 3, taskDetail.getSequence(), taskDetail.getStart(),
                                    taskDetail.getTerminal(), taskDetail.getStartId(), taskDetail.getTerminalId(), taskDetail.getMode(),
                                    0);
                            agvTaskManager.addTaskToQueue(task);
                        } else if (taskDetail.getMode() == 101) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.SECOND_STAGE_3F);
//                }
            }
            case SECOND_STAGE_3F -> {
                if (agvTaskManager.isEmpty(3)
                        && agvManager.getAgv(3).getTaskStatus() == AGV.TaskStatus.PRE_TERMINAL_STATION
                        && !agvManager.iAgvInElevator(3)) {
                    elevatorManager.controlElevatorTO(null);
                    elevatorManager.resetElevatorPermission();  // unlock elevator permission
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getStart().startsWith("3-T-")) {
                            AGVQTask task = new AGVQTask();
                            task.setAgvId(3);
                            task.setTaskNumber(taskDetail.getTaskNumber());
                            task.setSequence(taskDetail.getSequence());
                            task.setModeId(taskDetail.getMode());
                            task.setStartStation(taskDetail.getStart());
                            task.setStartStationId(taskDetail.getStartId());
                            task.setTerminalStation(taskDetail.getTerminal());
                            task.setTerminalStationId(taskDetail.getTerminalId());
                            task.setStatus(0);
                            agvTaskManager.addTaskToQueue(task);
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.THIRD_STAGE_3F);
                }
            }
            case THIRD_STAGE_3F -> {
                AGVQTask task = agvManager.getAgv(3).getTask();
                if(agvTaskManager.isEmpty(3) &&
                        (task == null || task.getTaskNumber().startsWith("#SB"))){
                    taskListManager.setTaskListPhase(nowTaskList, Phase.COMPLETED);
                }
            }
            case COMPLETED -> taskListManager.completedTaskList(taskProcessId);
        }
    }

    private void handleRETask(NowTaskList nowTaskList, List<TaskDetail> taskDetails, int taskProcessId){
        switch (nowTaskList.getPhase()) {
            case PRE_START -> {
                taskDetails.forEach(taskDetail -> {
                    if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getTerminal().startsWith("3-T-")) {
                        AGVQTask task = new AGVQTask();
                        task.setAgvId(3);
                        task.setTaskNumber(taskDetail.getTaskNumber());
                        task.setSequence(taskDetail.getSequence());
                        task.setModeId(taskDetail.getMode());
                        task.setStartStation(taskDetail.getStart());
                        task.setStartStationId(taskDetail.getStartId());
                        task.setTerminalStation(taskDetail.getTerminal());
                        task.setTerminalStationId(taskDetail.getTerminalId());
                        task.setStatus(0);
                        agvTaskManager.addTaskToQueue(task);
                    }
                });
                taskListManager.setTaskListPhase(nowTaskList, Phase.FIRST_STAGE_3F);
            }
            case FIRST_STAGE_3F -> {
                if (agvTaskManager.isEmpty(3) && agvManager.getAgv(3).getTask() == null) {
                    if(elevatorManager.acquireElevatorPermission()){  // check elevator permission
                        elevatorManager.controlElevatorTO(4);
                        taskListManager.setTaskListPhase(nowTaskList, Phase.CALL_ELEVATOR);
                    }
                }
            }
            case CALL_ELEVATOR -> {
//                if(elevatorManager.iOpenDoorByFloor(4)) {
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#3") && taskDetail.getTerminal().startsWith("E-")) {
                            AGVQTask task = new AGVQTask(taskDetail.getTaskNumber(), 3, taskDetail.getSequence(), taskDetail.getStart(),
                                    taskDetail.getTerminal(), taskDetail.getStartId(), taskDetail.getTerminalId(), taskDetail.getMode(),
                                    0);
                            agvTaskManager.addTaskToQueue(task);
                        } else if (taskDetail.getMode() == 100) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.SECOND_STAGE_3F);
//                }
            }
            case SECOND_STAGE_3F -> {
                AGVQTask task = agvManager.getAgv(3).getTask();
                if((task != null && task.getTaskNumber().startsWith("#SB") && !agvManager.iAgvInElevator(3))
                    || (agvTaskManager.isEmpty(3) && task == null && agvManager.iAgvInStandbyStation(3))) {
                    elevatorManager.controlElevatorTO(2);
                    taskListManager.setTaskListPhase(nowTaskList, Phase.ELEVATOR_TRANSFER);
                }
            }
            case ELEVATOR_TRANSFER -> {
//                if(elevatorManager.iOpenDoorByFloor(2)) {
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#1") && taskDetail.getStart().startsWith("E-")) {
                            AGVQTask task = new AGVQTask(taskDetail.getTaskNumber(), 1, taskDetail.getSequence(), taskDetail.getStart(),
                                    taskDetail.getTerminal(), taskDetail.getStartId(), taskDetail.getTerminalId(), taskDetail.getMode(),
                                    0);
                            agvTaskManager.addTaskToQueue(task);
                        } else if (taskDetail.getMode() == 101) {
                            taskDetailDao.updateStatusByTaskNumberAndSequence(taskDetail.getTaskNumber(), taskDetail.getSequence(), 100);
                            taskListManager.setTaskListProgress(nowTaskList, (int)(((double)taskDetail.getSequence()/(double)nowTaskList.getSteps())*99));
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.THIRD_STAGE_1F);
//                }
            }
            case THIRD_STAGE_1F -> {
                if(elevatorManager.getElevatorPermission() == ElevatorPermission.SYSTEM) {
                    if(agvTaskManager.isEmpty(1)
                            && agvManager.getAgv(1).getTaskStatus() == AGV.TaskStatus.PRE_TERMINAL_STATION
                            && !agvManager.iAgvInElevator(1)){
                        elevatorManager.controlElevatorTO(null);
                        elevatorManager.resetElevatorPermission();  // unlock elevator permission
                    }
                } else {
                    if(agvManager.getAgv(1).getTask() == null || agvManager.getAgv(1).getTask().getTaskNumber().startsWith("#SB")){
                        taskListManager.setTaskListPhase(nowTaskList, Phase.COMPLETED);
                    }
                }

            }
            case COMPLETED -> taskListManager.completedTaskList(taskProcessId);
        }
    }

    private void handleNETask(NowTaskList nowTaskList, List<TaskDetail> taskDetails, int taskProcessId){
        switch (nowTaskList.getPhase()) {
            case PRE_START -> {
                if (agvManager.getAgv(2).getStatus() == AGV.Status.ONLINE) {
                    taskListManager.setTaskListProgress(nowTaskList, 1);
                    taskDetails.forEach(taskDetail -> {
                        if (taskDetail.getTitle().equals("AMR#2")) {
                            AGVQTask task = new AGVQTask(taskDetail.getTaskNumber(), 2, taskDetail.getSequence(), taskDetail.getStart(),
                                    taskDetail.getTerminal(), taskDetail.getStartId(), taskDetail.getTerminalId(), taskDetail.getMode(),
                                    0);
                            agvTaskManager.addTaskToQueue(task);
                        }
                    });
                    taskListManager.setTaskListPhase(nowTaskList, Phase.TRANSFER);
                }
            }
            case TRANSFER -> {
                if (agvTaskManager.isEmpty(2) && agvManager.getAgv(2).getTask() == null) {
                    taskListManager.setTaskListPhase(nowTaskList, Phase.COMPLETED);
                }
            }
            case COMPLETED -> taskListManager.completedTaskList(taskProcessId);
        }
    }


}
