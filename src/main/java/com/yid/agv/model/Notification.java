package com.yid.agv.model;

import lombok.Data;

@Data
public class Notification {
    private Long id;
    private String name;
    private int level;
    private String message;
    private String createTime;

}
