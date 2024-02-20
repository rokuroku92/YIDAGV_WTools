
package com.yid.agv.repository;


import com.yid.agv.model.Station;

import java.util.List;

public interface StationDao {
    List<Station> queryStations();
    Integer getAreaGridsLength(String areaName);
    List<String> getStationTagByAreaName(String areaName);
    String getStationTagByGridName(String gridName);
    List<Station> queryStandbyStations();
}
