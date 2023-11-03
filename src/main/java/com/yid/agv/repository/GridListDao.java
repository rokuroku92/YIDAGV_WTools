package com.yid.agv.repository;

import com.yid.agv.model.GridList;

import java.util.List;

public interface GridListDao {
    List<GridList> queryAllGrids();
    boolean updateStatus(int stationId, int status);
    boolean updateWorkOrder(int stationId, String createTime, String workNumber, String objectName, String objectNumber);
    boolean updateWorkOrder(int stationId, String createTime, String workNumber1, String workNumber2, String objectName1,
                            String objectName2, String objectNumber1, String objectNumber2);
    boolean updateWorkOrder(int stationId, String createTime, String workNumber1, String workNumber2, String workNumber3,
                            String objectName1, String objectName2, String objectName3, String objectNumber1,
                            String objectNumber2, String objectNumber3);
    boolean updateWorkOrder(int stationId, String createTime, String workNumber1, String workNumber2, String workNumber3,
                            String workNumber4, String objectName1, String objectName2, String objectName3,
                            String objectName4, String objectNumber1, String objectNumber2, String objectNumber3,
                            String objectNumber4);
    boolean clearWorkOrder(int stationId);
}