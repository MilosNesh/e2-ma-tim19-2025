package com.example.habitgame.model;

public class RepeatedTask {

    private String id;
    private String userId;

    private String name;
    private String description;

    private String categoryId;
    private String weight;
    private String importance;

    private Long startDate;
    private Long endDate;

    private Integer repeatInterval;
    private String  repeatUnit;

    private TaskStatus status;

    private Integer xpPerOccurrence;
    private Integer xpTotal;
    private String  xpSplitStrategy;

    private Long createdAt;

    public RepeatedTask(){}

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

    public Long getStartDate() { return startDate; }
    public void setStartDate(Long startDate) { this.startDate = startDate; }

    public Long getEndDate() { return endDate; }
    public void setEndDate(Long endDate) { this.endDate = endDate; }

    public Integer getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(Integer repeatInterval) { this.repeatInterval = repeatInterval; }

    public String getRepeatUnit() { return repeatUnit; }
    public void setRepeatUnit(String repeatUnit) { this.repeatUnit = repeatUnit; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Integer getXpPerOccurrence() { return xpPerOccurrence; }
    public void setXpPerOccurrence(Integer xpPerOccurrence) { this.xpPerOccurrence = xpPerOccurrence; }

    public Integer getXpTotal() { return xpTotal; }
    public void setXpTotal(Integer xpTotal) { this.xpTotal = xpTotal; }

    public String getXpSplitStrategy() { return xpSplitStrategy; }
    public void setXpSplitStrategy(String xpSplitStrategy) { this.xpSplitStrategy = xpSplitStrategy; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public static String normUnit(String unit){
        if (unit == null) return "day";
        unit = unit.toLowerCase(java.util.Locale.ROOT).trim();
        if (unit.startsWith("nedel")) return "week";
        if (unit.startsWith("week"))  return "week";
        if (unit.startsWith("dan"))   return "day";
        if (unit.startsWith("day"))   return "day";
        return "day";
    }
}
