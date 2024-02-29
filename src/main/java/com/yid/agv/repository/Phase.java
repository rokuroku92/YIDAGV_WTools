package com.yid.agv.repository;

public enum Phase {
    PRE_START(1), CALL_ELEVATOR(2), FIRST_STAGE_1F(3), ELEVATOR_TRANSFER(4),
    SECOND_STAGE_3F(5), THIRD_STAGE_3F(6), COMPLETED(7),
    TRANSFER(8), FIRST_STAGE_3F(9), THIRD_STAGE_1F(10), CANCEL_TASK(11);
    private final int value;
    Phase(int value) {
        this.value = value;
    }
    public int getValue(){
        return value;
    }
    public static Phase valueOfByValue(int value) {
        for (Phase phase : Phase.values()) {
            if (phase.getValue() == value) {
                return phase;
            }
        }
        throw new IllegalArgumentException("No enum constant with value: " + value);
    }
}
