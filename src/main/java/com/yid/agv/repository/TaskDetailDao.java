package com.yid.agv.repository;

import com.yid.agv.model.TaskDetail;

import java.util.List;

public interface TaskDetailDao {

    enum Title{
        AMR_1(1), AMR_2(2), AMR_3(3), ELEVATOR(4);
        private final int value;
        Title(int value) {
            this.value = value;
        }
        public int getValue(){
            return value;
        }
    }
    List<TaskDetail> queryTaskDetailsByTaskNumber(String taskNumber);

    List<TaskDetail> queryAllTaskDetails();

    boolean insertTaskDetail(String taskNumber, Title title, int sequence, String startId, String terminalId, String modeId);

    boolean insertTaskDetail(String taskNumber, Title title, int sequence, String modeId);

    boolean updateStatusByTaskNumberAndSequence(String taskNumber, int sequence, int status);

}
