package com.yid.agv.backend.station;

import com.yid.agv.model.GridList;
import lombok.Data;

@Data
public class Grid {
    public enum Status{
        FREE(0), BOOKED(1), OCCUPIED(2), OVER_TIME(3),
        DISABLE(6);
        private final int value;
        Status(int value) {
            this.value = value;
        }

        public int getValue(){
            return value;
        }

    }

    private String gridName;
    private int id;
    private Status status;

    public Grid(GridList gridList) {
        this.gridName = gridList.getStation();
        this.id = gridList.getId();
        int statusValue = gridList.getStatus();

        for (Status statusEnum : Status.values()) {
            if (statusEnum.getValue() == statusValue) {
                this.status = statusEnum;
                break;
            }
        }
    }
}
