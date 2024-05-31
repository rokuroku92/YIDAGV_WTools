package com.yid.agv.repository.impls;

import com.yid.agv.model.Notification;
import com.yid.agv.repository.NotificationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationDaoImpl implements NotificationDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public List<Notification> queryTodayNotifications(){
        String sql = "SELECT nh.id, ntd.name, nh.level, nh.message, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time " +
                "FROM notification_history nh INNER JOIN notification_history_title_data ntd ON nh.title_id = ntd.id " +
                "WHERE DATE_FORMAT(nh.create_time, '%Y-%m-%d') = CURDATE() ORDER BY nh.id DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Notification.class));
    }
    @Override
    public List<Notification> queryNotificationsByDate(String date){  // date 2023-10-31 YYYY-MM-DD
        String sql = "SELECT nh.id, ntd.name, nh.level, nh.message, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time " +
                "FROM notification_history nh INNER JOIN notification_history_title_data ntd ON nh.title_id = ntd.id " +
                "WHERE DATE_FORMAT(nh.create_time, '%Y%m%d') = ? ORDER BY nh.id DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Notification.class), date);
    }
    @Override
    public List<Notification> queryAllNotifications(){
        String sql = "SELECT nh.id, ntd.name, nh.level, nh.message, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time " +
                "FROM notification_history nh INNER JOIN notification_history_title_data ntd ON nh.title_id = ntd.id " +
                "ORDER BY nh.id DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Notification.class));
    }
    @Override
    public List<Notification> queryNotifications(){
        String sql = "SELECT nh.id, ntd.name, nh.level, nh.message, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time " +
                "FROM notification_history nh INNER JOIN notification_history_title_data ntd ON nh.title_id = ntd.id " +
                "ORDER BY nh.id DESC LIMIT 1000";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Notification.class));
    }
    @Override
    public List<Notification> queryNotificationsL(){  // LIMIT 50
        String sql = "SELECT nh.id, ntd.name, nh.level, nh.message, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time " +
                "FROM notification_history nh INNER JOIN notification_history_title_data ntd ON nh.title_id = ntd.id " +
                "ORDER BY nh.id DESC LIMIT 50";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Notification.class));
    }

    @Override
    public void insertMessage(Title title, Status messageStatus){
        String sql = "INSERT INTO `notification_history`(`title_id`, `level`, `message`) VALUES(?, ?, ?)";
        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        jdbcTemplate.update(sql, title.getValue(), messageStatus.getLevel(), messageStatus.getContent());
    }

    @Override
    public void insertMessage(Title title, String content){
        String sql = "INSERT INTO `notification_history`(`title_id`, `level`, `message`) VALUES(?, ?, ?)";
        // 使用 JdbcTemplate 的 update 方法執行 SQL 語句
        jdbcTemplate.update(sql, title.getValue(), 0, content);
    }
}
