
package com.yid.agv.repository.impls;

import com.yid.agv.model.Station;
import com.yid.agv.repository.StationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StationDaoImpl implements StationDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public List<Station> queryStations(){
        String sql = "SELECT * FROM `station_data`";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Station.class));
    }
    @Override
    public Integer getAreaGridsLength(String areaName){
        String sql = "SELECT COUNT(*) AS length FROM `station_data` WHERE `name` LIKE ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, areaName+"%");
    }
    @Override
    public List<String> getStationTagByAreaName(String areaName){
        String sql = "SELECT tag FROM `station_data` WHERE `name` LIKE ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(String.class), areaName+"%");
    }
    @Override
    public String getStationTagByGridName(String gridName){
        String sql = "SELECT tag FROM `station_data` WHERE `name` = ?";
        return jdbcTemplate.queryForObject(sql, String.class, gridName);
    }
    @Override
    public List<Station> queryStandbyStations(){
        String sql = "SELECT * FROM `station_data` WHERE `name` LIKE '%-S'";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Station.class));
    }

}
