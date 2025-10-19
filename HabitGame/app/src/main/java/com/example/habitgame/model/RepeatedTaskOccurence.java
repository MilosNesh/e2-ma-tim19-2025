package com.example.habitgame.model;

public class RepeatedTaskOccurence {

    private String id;
    private String repeatedTaskId;
    private String userId;

    private String taskName;
    private String taskDescription;
    private String categoryId;
    private boolean seriesPaused;

    private Long when;
    private TaskStatus status;
    private boolean isCompleted;
    private Long completedAt;

    private int xp;

    private Long createdAt;

    public RepeatedTaskOccurence(){}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRepeatedTaskId() { return repeatedTaskId; }
    public void setRepeatedTaskId(String repeatedTaskId) { this.repeatedTaskId = repeatedTaskId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public boolean isSeriesPaused() { return seriesPaused; }
    public void setSeriesPaused(boolean seriesPaused) { this.seriesPaused = seriesPaused; }

    public Long getWhen() { return when; }
    public void setWhen(Long when) { this.when = when; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
