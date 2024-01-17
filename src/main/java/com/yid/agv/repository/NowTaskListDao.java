package com.yid.agv.repository;

import com.yid.agv.model.NowTaskList;
import com.yid.agv.model.NowTaskListResponse;

import java.util.List;

public interface NowTaskListDao {
    List<NowTaskList> queryNowTaskListsByProcessId(int processId);
    List<NowTaskListResponse> queryNowTaskListsResult();
    boolean insertNowTaskList(String taskNumber, int step);

    boolean updateNowTaskListPhase(String taskNumber, Phase phase);

    boolean updateNowTaskListProgress(String taskNumber, int progress);

    boolean deleteNowTaskList(String taskNumber);

}
