package com.example.habitgame.model;

public class Task {

    private String id;
    private String userId;
    private String name;
    private String description;
    private String categoryId;

    private String weight;
    private String importance;
    private int xpValue;

    private Long executionTime;
    private boolean isRepeating;
    private Long startDate;
    private Long endDate;
    private Integer repeatInterval;
    private String repeatUnit;
    private boolean isCompleted;
    private Long creationTimestamp;
    private Long lastCompletionTimestamp;
    private int completionsTodayCount;
    private TaskStatus status;

    public Task() {
        this.xpValue = 0;
        this.isCompleted = false;
        this.isRepeating = false;
        this.lastCompletionTimestamp = null;
        this.completionsTodayCount = 0;
    }

    public Task(String userId, String name, String description, String categoryId,
                String weight, String importance, int xpValue, Long executionTime,
                boolean isRepeating, Long startDate, Long endDate,
                Integer repeatInterval, String repeatUnit, Long creationTimestamp) {

        this.userId = userId;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.weight = weight;
        this.importance = importance;
        this.xpValue = xpValue;
        this.executionTime = executionTime;
        this.isRepeating = isRepeating;
        this.startDate = startDate;
        this.endDate = endDate;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.creationTimestamp = creationTimestamp;

        this.isCompleted = false;
        this.id = null;
        this.lastCompletionTimestamp = null;
        this.completionsTodayCount = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }

    public int getXpValue() { return xpValue; }
    public void setXpValue(int xpValue) { this.xpValue = xpValue; }

    public Long getExecutionTime() { return executionTime; }
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }

    public boolean getIsRepeating() { return isRepeating; }
    public void setIsRepeating(boolean isRepeating) { this.isRepeating = isRepeating; }

    public Long getStartDate() { return startDate; }
    public void setStartDate(Long startDate) { this.startDate = startDate; }

    public Long getEndDate() { return endDate; }
    public void setEndDate(Long endDate) { this.endDate = endDate; }

    public Integer getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(Integer repeatInterval) { this.repeatInterval = repeatInterval; }

    public String getRepeatUnit() { return repeatUnit; }
    public void setRepeatUnit(String repeatUnit) { this.repeatUnit = repeatUnit; }

    public boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }
    public Long getLastCompletionTimestamp() { return lastCompletionTimestamp; }
    public void setLastCompletionTimestamp(Long lastCompletionTimestamp) { this.lastCompletionTimestamp = lastCompletionTimestamp; }

    public int getCompletionsTodayCount() { return completionsTodayCount; }
    public void setCompletionsTodayCount(int completionsTodayCount) { this.completionsTodayCount = completionsTodayCount; }

    public Long getCreationTimestamp() {return creationTimestamp; }
    public void setCreationTimestamp(Long creationTimestamp) { this.creationTimestamp = creationTimestamp; }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}