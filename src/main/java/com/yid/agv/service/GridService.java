package com.yid.agv.service;

import com.yid.agv.backend.station.Grid;
import com.yid.agv.backend.station.GridManager;
import com.yid.agv.model.GridList;
import com.yid.agv.repository.GridListDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GridService {
    @Autowired
    private GridManager gridManager;
    @Autowired
    private GridListDao gridListDao;

    public List<GridList> getGridsStatus(){
        return gridListDao.queryAllGrids();
    }

    public boolean clearGrid(String gridName) {
        int stationId = gridManager.getGirdStationId(gridName);
        return gridListDao.clearWorkOrder(stationId) && gridListDao.updateStatus(stationId, Grid.Status.FREE);
    }

    public boolean occupiedGrid(String gridName) {
        int stationId = gridManager.getGirdStationId(gridName);
        return gridListDao.clearWorkOrder(stationId) && gridListDao.updateStatus(stationId, Grid.Status.OCCUPIED);
    }

    public boolean updateLineCode(String gridName, String lineNumber, String lineCode){
        return gridListDao.updateLineCode(gridManager.getGirdStationId(gridName), lineNumber, lineCode);
    }
}
