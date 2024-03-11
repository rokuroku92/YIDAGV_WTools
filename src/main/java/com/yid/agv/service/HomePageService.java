package com.yid.agv.service;

import com.yid.agv.backend.agv.AGV;
import com.yid.agv.backend.agv.AGVManager;
import com.yid.agv.backend.elevator.ElevatorManager;
import com.yid.agv.model.*;
import com.yid.agv.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomePageService {
    
    @Autowired
    private AGVIdDao agvIdDao;
    
    @Autowired
    private NotificationDao notificationDao;
    
    @Autowired
    private ModeDao modeDao;

    @Autowired
    private AGVManager agvManager;

    public AGV[] getAgv(){
        return agvManager.getAgvCopyArray();
    }

    public List<AGVId> queryAGVList(){
        return agvIdDao.queryAGVList();
    }

    public List<Notification> queryNotificationsL(){
        return notificationDao.queryNotificationsL();
    }

    public List<Notification> queryTodayNotifications(){
        return notificationDao.queryTodayNotifications();
    }

    public List<Notification> queryNotificationsByDate(String date){
        return notificationDao.queryNotificationsByDate(date);
    }

    public List<Notification> queryAllNotifications(){
        return notificationDao.queryAllNotifications();
    }

    public List<Notification> queryNotifications(){
        return notificationDao.queryNotifications();
    }


    public List<Mode> queryModes(){
        return modeDao.queryModes();
    }
    
    
}
