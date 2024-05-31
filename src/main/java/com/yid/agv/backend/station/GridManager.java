package com.yid.agv.backend.station;
import com.yid.agv.repository.GridListDao;
import com.yid.agv.repository.StationDao;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * GridManager 類負責管理站點的格位信息和狀態。
 */
@Component
public class GridManager {
    private static final Logger log = LoggerFactory.getLogger(GridManager.class);
    @Autowired
    private StationDao stationDao;
    @Autowired
    private GridListDao gridListDao;
    private final Map<String, Grid> gridMap;

    /**
     * GridManager 的構造函數，初始化 gridMap。
     */
    public GridManager(){
        gridMap = new HashMap<>();
    }

    /**
     * 在 Spring 容器初始化後執行，初始化 gridMap。
     */
    @PostConstruct
    public void initialize() {
        gridListDao.queryAllGrids().forEach(grid -> gridMap.put(grid.getStation(), new Grid(grid)));
        log.info("Initialize gridMap: " + gridMap);
    }

    /**
     * 定時任務，定期與數據庫同步站點網格信息。
     */
    @Scheduled(fixedRate = 1000)
    public void synchronizeDB() {
        gridListDao.queryAllGrids().forEach(grid -> gridMap.put(grid.getStation(), new Grid(grid)));
//        gridMap.forEach((index, grid) -> System.out.println(index + grid.getStatus()));
    }

    /**
     * 根據站點名稱獲取對應網格的 ID。
     * @param stationName 站點名稱
     * @return 對應網格的 ID
     */
    public int getGirdStationId(String stationName){
        return gridMap.get(stationName).getId();
    }

    /**
     * 根據站點 ID 獲取對應網格的名稱。
     * @param stationId 站點 ID
     * @return 對應網格的名稱
     */
    public String getGridNameByStationId(int stationId){
        Optional<Map.Entry<String, Grid>> result = gridMap.entrySet().stream()
                .filter(entry -> entry.getValue().getId() == stationId)
                .findFirst();

        return result.map(Map.Entry::getKey).orElse(null); // 返回找到的 gridName，或者返回 null
    }

    /**
     * 根據站點名稱獲取對應網格的狀態。
     * @param stationName 站點名稱
     * @return 對應網格的狀態
     */
    public Grid.Status getGridStatus(String stationName){
        return gridMap.get(stationName).getStatus();
    }

    /**
     * 獲取特定區域可用的網格列表。
     * <p>初始化一個空的availableGrids列表，以便儲存可用的網格。
     * <p>根據areaName的前綴設定groupGrids變量，以確定每個群組中包含多少個網格。 如果areaName以"3-C"開頭，則將groupGrids設為3，否則預設為2。
     * <p>使用循循環遍歷區域內的所有網格，檢查每個網格的狀態（使用getGridStatus方法）是否為Grid.Status.FREE。
     * <p>如果找到可用的網格，則將count增加1。
     * <p>如果達到了groupGrids的數量（即達到了群組的大小），並且count等於groupGrids，則將這些網格新增至availableGrids清單中，並重置count。
     * <p>最後，返回availableGrids列表，其中包含了滿足條件的可用網格。
     * @param areaName 區域名稱
     * @return 可用的網格列表
     */
    public List<Grid> getAvailableGrids(String areaName){  // 1-R, 2-A, 3-A, 3-B

        String fullAreaName = areaName + "-";
        int totalGrids = stationDao.getAreaGridsLength(areaName);
        int groupGrids = areaName.startsWith("3-C") ? 3 : 2;

        List<Grid> availableGrids = new ArrayList<>();
        int count = 0;
        if (areaName.startsWith("1-R")) {
            if (getGridStatus("1-R-2") == Grid.Status.FREE) {
                if (getGridStatus("1-R-1") == Grid.Status.FREE) {
                    availableGrids.add(gridMap.get("1-R-1"));
                }
                availableGrids.add(gridMap.get("1-R-2"));
            }
        } else {
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
        }

        return availableGrids;
    }

    /**
     * 設置特定網格的狀態。
     * @param gridName 網格名稱
     * @param status 狀態
     * @return 操作是否成功
     */
    public boolean setGridStatus(String gridName, Grid.Status status){
        boolean dbResult = gridListDao.updateStatus(getGirdStationId(gridName), status);
        if(dbResult){
            gridMap.get(gridName).setStatus(status);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 設置特定網格的狀態。
     * @param stationId 站點 ID
     * @param status 狀態
     * @return 操作是否成功
     */
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
