package com.yid.agv.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskListRequest {
    private int mode;
    private String terminal;
    private List<Task> tasks;

    @Data
    public static class Task {
        private String startGrid;
        private List<String> workNumber;

        @Override
        public String toString() {
            return super.toString()+"Task{" +
                    "startGrid='" + startGrid + '\'' +
                    ", objectNumber=" + workNumber +
                    '}';
        }
    }

}
