package com.yid.agv.backend.agv;


import com.yid.agv.model.Station;
import com.yid.agv.repository.AGVIdDao;
import com.yid.agv.repository.StationDao;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AGVManager 管理 AGV（Automated Guided Vehicle，自動導引車）的類。
 * 這個類負責初始化 AGV、查詢 AGV 信息，以及提供對 AGV 的各種操作。
 * 基本上大部分功能已棄用改為 AGVInstantStatus 控制。
 */
@Component
public class AGVManager {
    private static final Logger log = LoggerFactory.getLogger(AGVManager.class);
    @Autowired
    private AGVIdDao agvIdDao;
    @Autowired
    private StationDao stationDao;
    private final Map<Integer, AGV> agvMap;

    /**
     * AGVManager 的建構子，初始化 AGVMap。
     */
    public AGVManager(){
        agvMap = new HashMap<>();
    }

    /**
     * 在初始化之後調用，從 AGVIdDao 中獲取 AGV 列表並初始化 AGVMap。
     */
    @PostConstruct
    public void initialize() {
        agvIdDao.queryAGVList().forEach(agvId -> agvMap.put(agvId.getId(), new AGV(agvId.getId())));
        log.info("Initialize agvMap: "+ agvMap);
    }

    /**
     * 獲取 AGVMap 的大小。
     * @return AGVMap 的大小。
     */
    public int getAgvSize(){
        return agvMap.size();
    }

    /**
     * 根據 AGV ID 獲取對應的 AGV。
     * @param agvId AGV 的 ID。
     * @return 對應的 AGV 對象。
     */
    public AGV getAgv(int agvId){
        return agvMap.get(agvId);
    }

    /**
     * 獲取所有 AGV 的列表。
     * @return 包含所有 AGV 的列表。
     */
    public List<AGV> getAgvs() {
        return new ArrayList<>(agvMap.values());
    }

    /**
     * 判斷指定的 AGV 是否在電梯中。
     * @param agvId AGV 的 ID。
     * @return 如果指定的 AGV 在電梯中，則返回 true；否則返回 false。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean iAgvInElevator(int agvId){
        int place = Integer.parseInt(agvMap.get(agvId).getPlace());
        List<String> tags = stationDao.getStationTagByAreaName("E-");
        for (String tagString: tags) {
            int tag = Integer.parseInt(tagString);
            tag = tag/1000*1000 + (tag%250);
            if (tag == place
                    || tag+250 == place
                    || tag+500 == place
                    || tag+750 == place)
                return true;
        }
        return false;
    }

    /**
     * 判斷指定的 AGV 是否在待命點。
     * @param agvId AGV 的 ID。
     * @return 如果指定的 AGV 在待命點，則返回 true；否則返回 false。
     */
    public boolean iAgvInStandbyStation(int agvId){
        int place = Integer.parseInt(agvMap.get(agvId).getPlace());
        if (place == -1) return false;

        List<Integer> standbyTags = stationDao.queryStandbyStations().stream()
                .map(Station::getTag).toList();

        for (int standbyTag : standbyTags) {
            standbyTag = standbyTag/1000*1000 + (standbyTag%250);
            if (standbyTag == place
                    || standbyTag+250 == place
                    || standbyTag+500 == place
                    || standbyTag+750 == place)
                return true;
        }

        return false;
    }


    /**
     * 獲取 AGVMap 中所有 AGV 的副本。
     * @return AGVMap 中所有 AGV 的副本數組。
     */
    public AGV[] getAgvCopyArray() {
        return agvMap.values()
                .stream()
                .map(this::copyAGV)
                .toArray(AGV[]::new);
    }

    private AGV copyAGV(AGV originalAGV) {
        AGV copy = new AGV(originalAGV.getId());
        copy.setStatus(originalAGV.getStatus());
        copy.setPlace(originalAGV.getPlace());
        copy.setBattery(originalAGV.getBattery());
        copy.setSignal(originalAGV.getSignal());
        copy.setTask(originalAGV.getTask());
        copy.setTaskStatus(originalAGV.getTaskStatus());
        copy.setIScan(originalAGV.isIScan());
        copy.setTitle(originalAGV.getTitle());
        copy.setLastAgvSystemStatusData(originalAGV.getLastAgvSystemStatusData());
        copy.setILowBattery(originalAGV.isILowBattery());
        copy.setLowBatteryCount(originalAGV.getLowBatteryCount());
        copy.setReDispatchCount(originalAGV.getReDispatchCount());
        copy.setTagError(originalAGV.isTagError());
        copy.setFixAgvTagErrorCompleted(originalAGV.isFixAgvTagErrorCompleted());
        copy.setTagErrorDispatchCompleted(originalAGV.isTagErrorDispatchCompleted());
        copy.setLastTaskBuffer(originalAGV.isLastTaskBuffer());
        copy.setObstacleCount(originalAGV.getObstacleCount());
        copy.setIAlarm(originalAGV.isIAlarm());
        return copy;
    }


}
