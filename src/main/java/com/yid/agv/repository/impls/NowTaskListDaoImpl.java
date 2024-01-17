package com.yid.agv.repository.impls;

import com.yid.agv.model.NowTaskList;
import com.yid.agv.model.NowTaskListResponse;
import com.yid.agv.repository.NowTaskListDao;
import com.yid.agv.repository.Phase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NowTaskListDaoImpl implements NowTaskListDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<NowTaskList> queryNowTaskListsByProcessId(int processId){
        String sql = "SElECT * FROM now_task_list ORDER BY id";
        switch (processId){
            case 1 -> sql = "SElECT * FROM now_task_list WHERE task_number LIKE '#YE%' OR task_number LIKE '#RE%' ORDER BY id";
            case 2 -> sql = "SElECT * FROM now_task_list WHERE task_number LIKE '#NE%' ORDER BY id";
        }

        List<NowTaskList> nowTaskLists = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(NowTaskList.class));

        for (NowTaskList nowTask : nowTaskLists) {
            int phaseValue = nowTask.getPhaseId();
            Phase phaseEnum = Phase.valueOfByValue(phaseValue); // 使用自定義方法映射到 enum Phase
            nowTask.setPhase(phaseEnum); // 設置映射後的 enum Phase 到 NowTaskList
        }
        return nowTaskLists;
    }

    @Override
    public List<NowTaskListResponse> queryNowTaskListsResult(){
        String sql = "SELECT ntl.id, ntl.task_number, ntl.steps, ntl.progress, tp.name AS phase " +
                "FROM now_task_list ntl INNER JOIN task_phase tp ON ntl.phase_id = tp.id ORDER BY id";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(NowTaskListResponse.class));
    }

    @Override
    public boolean insertNowTaskList(String taskNumber, int step){
        String sql = "INSERT INTO `now_task_list`(`task_number`, `steps`) VALUES(?, ?)";
        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        int rowsAffected = jdbcTemplate.update(sql, taskNumber, step);
        return (rowsAffected > 0);
    }

    @Override
    public boolean updateNowTaskListPhase(String taskNumber, Phase phase){
        String sql = "UPDATE `now_task_list` SET `phase_id` = ? WHERE `task_number` = ?";
        int rowsAffected = jdbcTemplate.update(sql, phase.getValue(), taskNumber);
        return (rowsAffected > 0);
    }

    @Override
    public boolean updateNowTaskListProgress(String taskNumber, int progress){
        String sql = "UPDATE `now_task_list` SET `progress` = ? WHERE `task_number` = ?";
        int rowsAffected = jdbcTemplate.update(sql, progress, taskNumber);
        return (rowsAffected > 0);
    }

    @Override
    public boolean deleteNowTaskList(String taskNumber){
        String sql = "DELETE FROM `now_task_list` WHERE task_number = ?";
        int rowsAffected = jdbcTemplate.update(sql, taskNumber);
        return (rowsAffected > 0);
    }
}
