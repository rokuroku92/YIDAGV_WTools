package com.yid.agv.repository.impls;

import com.yid.agv.model.TaskDetail;
import com.yid.agv.repository.TaskDetailDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskDetailDaoImpl implements TaskDetailDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<TaskDetail> queryTaskDetailsByTaskNumber(String taskNumber){
        String sql = "SELECT td.id, td.task_number, tdt.name AS title, td.sequence, sd.name AS start, sd.id AS start_id, " +
                "sdd.name AS terminal, sdd.id AS terminal_id, md.mode AS mode, md.memo AS mode_memo, td.status, td.create_task_time FROM task_detail td " +
                "INNER JOIN task_detail_title tdt ON td.title_id = tdt.id " +
                "LEFT JOIN station_data sd ON td.start_id = sd.id " +
                "LEFT JOIN station_data sdd ON td.terminal_id = sdd.id " +
                "INNER JOIN mode_data md ON td.mode_id = md.id WHERE task_number = ? ORDER BY td.sequence";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TaskDetail.class), taskNumber);
    }

    @Override
    public List<TaskDetail> queryAllTaskDetails(){
        String sql = "SELECT td.id, td.task_number, tdt.name AS title, td.sequence, sd.name AS start, sd.tag AS start_tag, " +
                "sdd.name AS terminal, sdd.tag AS terminal_tag, md.mode AS mode, md.memo AS mode_memo, td.status, td.create_task_time FROM task_detail td " +
                "INNER JOIN task_detail_title tdt ON td.title_id = tdt.id " +
                "LEFT JOIN station_data sd ON td.start_id = sd.id " +
                "LEFT JOIN station_data sdd ON td.terminal_id = sdd.id " +
                "INNER JOIN mode_data md ON td.mode_id = md.id WHERE td.task_number NOT LIKE '#SB%' " +
                "ORDER BY CONVERT(SUBSTRING(td.task_number, 4, 14), SIGNED INTEGER) DESC, td.sequence DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TaskDetail.class));
    }

    @Override
    public boolean insertTaskDetail(String taskNumber, Title title, int sequence, String startId, String terminalId, Mode mode, String time){
        String sql = "INSERT INTO `task_detail`(`task_number`, `title_id`, `sequence`, `start_id`, `terminal_id`, `mode_id`, `create_task_time`) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?)";

        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        int rowsAffected = jdbcTemplate.update(sql, taskNumber, title.getValue(), sequence, startId, terminalId, mode.getValue(), time);
        return (rowsAffected > 0);
    }

    @Override
    public boolean insertTaskDetail(String taskNumber, Title title, int sequence, Mode mode, String time){
        String sql = "INSERT INTO `task_detail`(`task_number`, `title_id`, `sequence`, `mode_id`, `create_task_time`) " +
                "VALUES(?, ?, ?, ?, ?)";

        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        int rowsAffected = jdbcTemplate.update(sql, taskNumber, title.getValue(), sequence, mode.getValue(), time);
        return (rowsAffected > 0);
    }

    @Override
    public boolean updateStatusByTaskNumberAndSequence(String taskNumber, int sequence, int status){
        String sql = "UPDATE `task_detail` SET `status` = ? WHERE `task_number` = ? AND `sequence` = ?";

        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        int rowsAffected = jdbcTemplate.update(sql, status, taskNumber, sequence);
        return (rowsAffected > 0);
    }
}
