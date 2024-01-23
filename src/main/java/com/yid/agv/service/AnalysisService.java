package com.yid.agv.service;

import com.yid.agv.model.Analysis;

import java.util.List;
import java.util.Map;

import com.yid.agv.model.WorkNumberAnalysis;
import com.yid.agv.model.WorkNumberTA001Analysis;
import com.yid.agv.repository.AnalysisDao;
import com.yid.agv.repository.WorkNumberDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    @Autowired
    private AnalysisDao analysisDao;

    @Autowired
    private WorkNumberDao workNumberDao;

    public List<Analysis> queryAnalysisByAGV(Integer agvId){
        return analysisDao.queryAnalysisByAGV(agvId);
    }

    public List<Analysis> queryAnalysisRecentlyByAGV(Integer agvId){
        return analysisDao.queryAnalysisRecentlyByAGV(agvId);
    }

    public List<Analysis> queryAnalysisByAGVAndYearAndMonth(Integer agvId, Integer year, Integer month){
        // 參數檢查
        if(year<2022 || month < 1 || month > 12) return null;
        return analysisDao.queryAnalysisByAGVAndYearAndMonth(agvId, year, month);
    }

    public List<Map<String, Object>> getAnalysisYearsAndMonths(){
        return analysisDao.getAnalysisYearsAndMonths();
    }

    public List<WorkNumberAnalysis> getWorkNumberAnalysis(int numberOfMonths){
        return workNumberDao.getWorkNumberAnalysis(numberOfMonths);
    }

    public List<WorkNumberTA001Analysis> getWorkNumberTA001Analysis(int numberOfMonths){
        return workNumberDao.getWorkNumberTA001Analysis(numberOfMonths);
    }

}
