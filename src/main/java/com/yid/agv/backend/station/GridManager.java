package com.yid.agv.backend.station;
import com.yid.agv.backend.ProcessAGVTask;
import com.yid.agv.repository.GridListDao;
import com.yid.agv.repository.StationDao;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GridManager {
    private static final Logger log = LoggerFactory.getLogger(GridManager.class);
    @Autowired
    private StationDao stationDao;
    @Autowired
    private GridListDao gridListDao;
    private final Map<String, Grid> gridMap;

    public GridManager(){
        gridMap = new HashMap<>();
    }

    @PostConstruct
    public void initialize() {
        gridListDao.queryAllGrids().forEach(grid -> gridMap.put(grid.getStation(), new Grid(grid)));
        log.info("Initialize gridMap: " + gridMap);
    }

    @Scheduled(fixedRate = 1000)
    public void synchronizeDB() {
        gridListDao.queryAllGrids().forEach(grid -> gridMap.put(grid.getStation(), new Grid(grid)));
//        gridMap.forEach((index, grid) -> System.out.println(index + grid.getStatus()));
        // TODO: check over time
    }


    public int getGirdStationId(String stationName){
        return gridMap.get(stationName).getId();
    }

    public String getGridNameByStationId(int stationId){
        Optional<Map.Entry<String, Grid>> result = gridMap.entrySet().stream()
                .filter(entry -> entry.getValue().getId() == stationId)
                .findFirst();

        return result.map(Map.Entry::getKey).orElse(null); // 返回找到的 gridName，或者返回 null
    }

    public Grid.Status getGridStatus(String stationName){
        return gridMap.get(stationName).getStatus();
    }


    /*
     * 初始化一個空的availableGrids列表，以便儲存可用的網格。
     * 根據areaName的前綴設定groupGrids變量，以確定每個群組中包含多少個網格。 如果areaName以"3-C"開頭，則將groupGrids設為3，否則預設為2。
     * 使用循循環遍歷區域內的所有網格，檢查每個網格的狀態（使用getGridStatus方法）是否為Grid.Status.FREE。
     * 如果找到可用的網格，則將count增加1。
     * 如果達到了groupGrids的數量（即達到了群組的大小），並且count等於groupGrids，則將這些網格新增至availableGrids清單中，並重置count。
     * 最後，返回availableGrids列表，其中包含了滿足條件的可用網格。
     */
    public List<Grid> getAvailableGrids(String areaName){  // 1-R, 2-A, 3-A, 3-B

        String fullAreaName = areaName + "-";
        int totalGrids = stationDao.getAreaGridsLength(areaName);
        int groupGrids = areaName.startsWith("3-C") ? 3 : 2;

        List<Grid> availableGrids = new ArrayList<>();
        int count = 0;
        for (int i = 1; i <= totalGrids; i++) {
            if (getGridStatus(fullAreaName+i) == Grid.Status.FREE){
                count++;
            }
            if (i % groupGrids == 0){
                if (count == groupGrids){
                    for (int j = groupGrids-1; j >= 0; j--) {
                        availableGrids.add(gridMap.get(fullAreaName+(i-j)));
                    }
                }
                count = 0;
            }
        }

        return availableGrids;
    }

    public boolean setGridStatus(String gridName, Grid.Status status){
        boolean dbResult = gridListDao.updateStatus(getGirdStationId(gridName), status);
        if(dbResult){
            gridMap.get(gridName).setStatus(status);
            return true;
        } else {
            return false;
        }
    }

    public boolean setGridStatus(int stationId, Grid.Status status){
        boolean dbResult = gridListDao.updateStatus(stationId, status);
        if(dbResult){
            gridMap.forEach((name, grid) -> {
                if (grid.getId() == stationId){
                    grid.setStatus(status);
                }
            });
            return true;
        } else {
            return false;
        }
    }


}
