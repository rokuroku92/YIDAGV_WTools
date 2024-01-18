package com.yid.agv.repository.impls;

import com.yid.agv.model.TaskList;
import com.yid.agv.repository.Phase;
import com.yid.agv.repository.TaskListDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskListDaoImpl implements TaskListDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public List<TaskList> queryTodayTaskLists(){
        String sql = "SELECT tl.id, tl.task_number, tl.create_task_time, tl.steps, tl.progress, tp.name AS phase, tl.status " +
                "FROM task_list tl INNER JOIN task_phase tp ON tl.phase_id = tp.id " +
                "WHERE DATE_FORMAT(STR_TO_DATE(create_task_time, '%Y%m%d%H%i%s'), '%Y-%m-%d') = CURDATE() ORDER BY id";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TaskList.class));
    }
    @Override
    public List<TaskList> queryTaskListsByDate(String date){
        String sql = "SELECT tl.id, tl.task_number, tl.create_task_time, tl.steps, tl.progress, tp.name AS phase, tl.status " +
                "FROM task_list tl INNER JOIN task_phase tp ON tl.phase_id = tp.id " +
                "WHERE DATE_FORMAT(STR_TO_DATE(create_task_time, '%Y%m%d%H%i%s'), '%Y-%m-%d') = ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TaskList.class), date);
    }
    @Override
    public List<TaskList> queryTaskLists(){
        String sql = "SELECT tl.id, tl.task_number, tl.create_task_time, tl.steps, tl.progress, tp.name AS phase, tl.status " +
                "FROM task_list tl INNER JOIN task_phase tp ON tl.phase_id = tp.id ORDER BY id DESC LIMIT 100";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TaskList.class));
    }
    @Override
    public List<TaskList> queryAllTaskLists(){
        String sql = "SELECT tl.id, tl.task_number, tl.create_task_time, tl.steps, tl.progress, tp.name AS phase, tl.status " +
                "FROM task_list tl INNER JOIN task_phase tp ON tl.phase_id = tp.id ORDER BY id DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TaskList.class));
    }
    @Override
    public String selectLastTaskListNumber(){
        String sql = "SELECT task_number FROM task_list WHERE `task_number` LIKE '#%E%' ORDER BY `id` DESC LIMIT 1";
        return jdbcTemplate.queryForObject(sql, String.class);
    }
    @Override
    public boolean insertTaskList(String taskNumber, String createTime, int step){
        String sql = "INSERT INTO `task_list`(`task_number`, `create_task_time`, `steps`) VALUES(?, ?, ?)";

        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        int rowsAffected = jdbcTemplate.update(sql, taskNumber, createTime, step);
        return (rowsAffected > 0);
    }

    @Override
    public boolean updateTaskListProgress(String taskNumber, int progress){
        String sql = "UPDATE `task_list` SET `progress` = ? WHERE `task_number` = ?";
        int rowsAffected = jdbcTemplate.update(sql, progress, taskNumber);
        return (rowsAffected > 0);
    }

    @Override
    public boolean updateTaskListPhase(String taskNumber, Phase phase){
        String sql = "UPDATE `task_list` SET `phase_id` = ? WHERE `task_number` = ?";
        int rowsAffected = jdbcTemplate.update(sql, phase.getValue(), taskNumber);
        return (rowsAffected > 0);
    }

    @Override
    public boolean updateTaskListStatus(String taskNumber, int status){
        String sql = "UPDATE `task_list` SET `status` = ? WHERE `task_number` = ?";
        int rowsAffected = jdbcTemplate.update(sql, status, taskNumber);
        return (rowsAffected > 0);
    }
    @Override
    public boolean cancelTaskList(String taskNumber){
        String sql = "UPDATE `task_list` SET `status` -1 WHERE `task_number` = ?";
        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        int rowsAffected = jdbcTemplate.update(sql, taskNumber);
        return (rowsAffected > 0);
    }


}
