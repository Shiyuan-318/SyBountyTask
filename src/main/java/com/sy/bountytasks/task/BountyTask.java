package com.sy.bountytasks.task;

import java.util.UUID;

public class BountyTask {
    private int id;
    private UUID publisherUuid;
    private String publisherName;
    private TaskType type;
    private String title;
    private String details;
    private double reward;
    private double serviceFee;
    private long createTime;
    private UUID claimerUuid;
    private String claimerName;
    private TaskStatus status;

    public enum TaskStatus {
        PENDING,
        CLAIMED,
        COMPLETED,
        CONFIRMED
    }

    public BountyTask(int id, UUID publisherUuid, String publisherName, TaskType type,
                      String title, String details, double reward, double serviceFee) {
        this.id = id;
        this.publisherUuid = publisherUuid;
        this.publisherName = publisherName;
        this.type = type;
        this.title = title;
        this.details = details;
        this.reward = reward;
        this.serviceFee = serviceFee;
        this.createTime = System.currentTimeMillis();
        this.status = TaskStatus.PENDING;
    }

    public int getId() {
        return id;
    }

    public UUID getPublisherUuid() {
        return publisherUuid;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public TaskType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public double getReward() {
        return reward;
    }

    public double getServiceFee() {
        return serviceFee;
    }

    public long getCreateTime() {
        return createTime;
    }

    public UUID getClaimerUuid() {
        return claimerUuid;
    }

    public void setClaimerUuid(UUID claimerUuid) {
        this.claimerUuid = claimerUuid;
    }

    public String getClaimerName() {
        return claimerName;
    }

    public void setClaimerName(String claimerName) {
        this.claimerName = claimerName;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public double getTotalCost() {
        return reward + serviceFee;
    }
}
