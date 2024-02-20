package com.yid.agv.backend.agv;


import com.yid.agv.backend.ProcessAGVTask;
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

@Component
public class AGVManager {
    private static final Logger log = LoggerFactory.getLogger(ProcessAGVTask.class);
    @Autowired
    private AGVIdDao agvIdDao;
    @Autowired
    private StationDao stationDao;
    private final Map<Integer, AGV> agvMap;

    public AGVManager(){
        agvMap = new HashMap<>();
    }

    @PostConstruct
    public void initialize() {
        agvIdDao.queryAGVList().forEach(agvId -> agvMap.put(agvId.getId(), new AGV(agvId.getId())));
        log.info("Initialize agvMap: "+ agvMap);
    }

    public int getAgvSize(){
        return agvMap.size();
    }

    public AGV getAgv(int agvId){
        return agvMap.get(agvId);
    }

    public List<AGV> getAgvs() {
        return new ArrayList<>(agvMap.values());
    }

    public boolean iAgvInElevator(int agvId){
        String place = agvMap.get(agvId).getPlace();
        List<String> tags = stationDao.getStationTagByAreaName("E-");
        for (String tag: tags) {
            if (place.equals(tag)){
                return true;
            }
        }
        return false;
    }

    public int getAgvLength(){
        return agvMap.size();
    }

    public AGV[] getAgvCopyArray() {
        return agvMap.values()
                .stream()
                .map(originalAGV -> {
                    AGV copy = new AGV(originalAGV.getId());
                    copy.setStatus(originalAGV.getStatus());
                    copy.setPlace(originalAGV.getPlace());
                    copy.setBattery(originalAGV.getBattery());
                    copy.setSignal(originalAGV.getSignal());
                    copy.setTask(originalAGV.getTask());
                    copy.setTaskStatus(originalAGV.getTaskStatus());
                    return copy;
                })
                .toArray(AGV[]::new);
    }

}
