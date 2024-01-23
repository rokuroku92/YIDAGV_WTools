package com.yid.agv.repository;

import com.yid.agv.model.WorkNumberAnalysis;
import com.yid.agv.model.WorkNumberTA001Analysis;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import java.util.List;

public interface WorkNumberDao {
    List<WorkNumberAnalysis> getWorkNumberAnalysis(int numberOfMonths);
    List<WorkNumberTA001Analysis> getWorkNumberTA001Analysis(int numberOfMonths);
    boolean insertWorkNumber(String taskNumber);
}
