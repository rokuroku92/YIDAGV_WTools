package com.yid.agv.backend;

import com.yid.agv.backend.agv.AGV;
import com.yid.agv.backend.agv.AGVManager;
import com.yid.agv.model.Analysis;
import com.yid.agv.model.AnalysisId;
import com.yid.agv.model.YearMonthDay;
import com.yid.agv.repository.AGVIdDao;
import com.yid.agv.repository.AnalysisDao;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CountUtilizationRate {
    @Autowired
    private AnalysisDao analysisDao;
    @Autowired
    private AGVIdDao agvIdDao;
    @Autowired
    private AGVManager agvManager;
    private String lastDate;
    private List<AnalysisId> analysisIds;


    @Scheduled(fixedRate = 60000) // 每分鐘執行一次
    public void checkAgvStatus() {
        // 現在時間
        LocalDate currentDate = LocalDate.now();
        String year = String.valueOf(currentDate.getYear());
        String month = String.valueOf(currentDate.getMonthValue());
        String day = String.valueOf(currentDate.getDayOfMonth());
        String week = String.valueOf(currentDate.getDayOfWeek().getValue());
        String nowDate = year.concat(month).concat(day);

        if(lastDate == null){
            YearMonthDay ymd = analysisDao.getLastAnalysisYMD();
            lastDate = String.valueOf(ymd.getYear()).concat(String.valueOf(ymd.getMonth())).concat(String.valueOf(ymd.getDay()));
            analysisIds = analysisDao.getTodayAnalysisId();
        }
        if(!lastDate.equals(nowDate)){
            for(int i=0;i<agvIdDao.queryAGVList().size();i++)
                analysisDao.insertNewDayAnalysis(Integer.toString(agvIdDao.queryAGVList().get(i).getId()), year, month, day, week);
            lastDate = nowDate;
            analysisIds = analysisDao.getTodayAnalysisId();
        }

        for(int i=0;i<agvManager.getAgvSize();i++){
            AGV agv = agvManager.getAgv(i+1);
            // 執行相應的處理邏輯，如更新資料庫、記錄日誌等
            Analysis analysis = analysisDao.queryAnalysisByAnalysisId(analysisIds.get(i).getAnalysisId());
            if (agv.getStatus() != AGV.Status.OFFLINE) {
                // AGV開機處理邏輯
                analysisDao.updateOpenMinute(analysis.getOpenMinute()+1, analysisIds.get(i).getAnalysisId());
                if (agv.getTask() != null && !agv.getTask().getTaskNumber().matches("#(SB|LB).*")) {
                    // AGV工作處理邏輯
                    analysisDao.updateWorkingMinute(analysis.getWorkingMinute()+1, analysisIds.get(i).getAnalysisId());
                } // AGV停止工作則不處理業務邏輯
            } // AGV未開機則不處理業務邏輯

        }

    }

}
