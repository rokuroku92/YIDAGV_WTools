package com.yid.agv.repository.impls;

import com.yid.agv.model.WorkNumberAnalysis;
import com.yid.agv.model.WorkNumberTA001Analysis;
import com.yid.agv.repository.WorkNumberDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkNumberDaoImpl implements WorkNumberDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<WorkNumberAnalysis> getWorkNumberAnalysis(int numberOfMonths){
        String sql = "SELECT work_number, COUNT(*) AS count FROM work_number_history WHERE " +
                    "time >= CURDATE() - INTERVAL ? MONTH GROUP BY work_number";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(WorkNumberAnalysis.class), numberOfMonths);
    }

    @Override
    public List<WorkNumberTA001Analysis> getWorkNumberTA001Analysis(int numberOfMonths){
        String sql = "SELECT SUBSTRING_INDEX(work_number, '-', 1) AS TA001, COUNT(*) AS count FROM work_number_history " +
                "WHERE time >= CURDATE() - INTERVAL ? MONTH GROUP BY TA001";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(WorkNumberTA001Analysis.class), numberOfMonths);
    }

    @Override
    public boolean insertWorkNumber(String taskNumber){
        String sql = "INSERT INTO `work_number_history`(`work_number`) VALUES (?)";

        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        int rowsAffected = jdbcTemplate.update(sql, taskNumber);
        return (rowsAffected > 0);
    }
}
